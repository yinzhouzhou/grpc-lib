

package com.github.yinzhouzhou.sdn.grpc.example.ctl;


import static java.util.concurrent.Executors.newSingleThreadScheduledExecutor;
import static org.slf4j.LoggerFactory.getLogger;

import com.github.yinzhouzhou.sdn.grpc.client.api.device.DeviceId;
import com.github.yinzhouzhou.sdn.grpc.example.api.GnmiEvent;
import com.github.yinzhouzhou.sdn.grpc.example.api.GnmiUpdate;
import com.github.yinzhouzhou.sdn.grpc.gnmi.protobuff.messages.Gnmi;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import io.grpc.StatusRuntimeException;
import io.grpc.stub.ClientCallStreamObserver;
import io.grpc.stub.StreamObserver;
import java.net.ConnectException;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import javax.annotation.concurrent.NotThreadSafe;
import org.slf4j.Logger;

/**
 * A manager for the gNMI Subscribe RPC that opportunistically starts new RPC
 * (e.g. when one fails because of errors) and posts subscribe events via the
 * gNMI controller.
 */
@NotThreadSafe
final class GnmiSubscriptionManager {

    // FIXME: make this configurable
    private static final long DEFAULT_RECONNECT_DELAY = 5; // Seconds

    private static final Logger LOG = getLogger(GnmiSubscriptionManager.class);

    private final GnmiClientImpl client;
    private final DeviceId deviceId;
    private final GnmiClientControllerImpl controller;
    private final StreamObserver<Gnmi.SubscribeResponse> responseObserver;

    private final ScheduledExecutorService streamCheckerExecutor =
        newSingleThreadScheduledExecutor(new ThreadFactoryBuilder()
            .setNameFormat("GnmiSubscriptionManager-%d")
            .setUncaughtExceptionHandler((thread, throwable) -> {
                LOG.error("Uncaught exception on {}", thread.getName(), throwable);
            }).build());

    private Future<?> checkTask;

    private ClientCallStreamObserver<Gnmi.SubscribeRequest> requestObserver;
    private Gnmi.SubscribeRequest existingSubscription;
    private AtomicBoolean active = new AtomicBoolean(false);

    GnmiSubscriptionManager(GnmiClientImpl client, DeviceId deviceId,
                            GnmiClientControllerImpl controller) {
        this.client = client;
        this.deviceId = deviceId;
        this.controller = controller;
        this.responseObserver = new InternalStreamResponseObserver();
    }

    synchronized void subscribe(Gnmi.SubscribeRequest request) {

        if (existingSubscription != null) {
            if (existingSubscription.equals(request)) {
                // Nothing to do. We are already subscribed for the same
                // request.
                LOG.debug("Ignoring re-subscription to same request for {}",
                    deviceId);
                return;
            }
            LOG.debug("Cancelling existing subscription for {} before " + "starting a new one", deviceId);
            complete();
        }
        existingSubscription = request;
        sendSubscribeRequest();
        if (checkTask != null) {
            checkTask = streamCheckerExecutor.scheduleAtFixedRate(
                this::checkSubscription, 0,
                DEFAULT_RECONNECT_DELAY,
                TimeUnit.SECONDS);
        }

    }

    synchronized void unsubscribe() {
        if (checkTask != null) {
            checkTask.cancel(false);
            checkTask = null;
        }
        existingSubscription = null;
        complete();

    }

    public void shutdown() {
        LOG.debug("Shutting down gNMI subscription manager for {}", deviceId);
        unsubscribe();
        streamCheckerExecutor.shutdownNow();
    }

    private synchronized void checkSubscription() {
        if (existingSubscription != null && !active.get()) {
            if (client.isServerReachable() || Futures.getUnchecked(client.probeService())) {
                LOG.info("Re-starting Subscribe RPC for {}...", deviceId);
                sendSubscribeRequest();
            } else {
                LOG.debug("Not restarting Subscribe RPC for {}, server is NOT reachable",
                    deviceId);
            }
        }
    }

    private synchronized void sendSubscribeRequest() {
        if (requestObserver == null) {
            LOG.debug("Starting new Subscribe RPC for {}...", deviceId);
            client.execRpcNoTimeout(
                s -> requestObserver =
                    (ClientCallStreamObserver<Gnmi.SubscribeRequest>)
                        s.subscribe(responseObserver)
            );
        }
        requestObserver.onNext(existingSubscription);
        active.set(true);
    }

    public synchronized void complete() {
        active.set(false);
        if (requestObserver != null) {
            requestObserver.onCompleted();
            requestObserver.cancel("Terminated", null);
            requestObserver = null;
        }
    }

    /**
     * Handles messages received from the device on the Subscribe RPC.
     */
    private final class InternalStreamResponseObserver
        implements StreamObserver<Gnmi.SubscribeResponse> {

        @Override
        @SuppressWarnings("IllegalCatch")
        public void onNext(Gnmi.SubscribeResponse message) {
            try {
                if (LOG.isTraceEnabled()) {
                    LOG.trace("Received SubscribeResponse from {}: {}",
                        deviceId, message.toString());
                }
                controller.postEvent(new GnmiEvent(GnmiEvent.Type.UPDATE, new GnmiUpdate(
                    deviceId, message.getUpdate(), message.getSyncResponse())));
            } catch (Throwable ex) {
                LOG.error("Exception processing SubscribeResponse from {}", deviceId,
                    ex);
            }
        }

        @Override
        public void onError(Throwable throwable) {
            complete();
            if (throwable instanceof StatusRuntimeException) {
                StatusRuntimeException sre = (StatusRuntimeException) throwable;
                if (sre.getStatus().getCause() instanceof ConnectException) {
                    LOG.warn("{} is unreachable ({})",
                        deviceId, sre.getCause().getMessage());
                } else {
                    LOG.warn("Error on Subscribe RPC for {}: {}",
                        deviceId, throwable.getMessage());
                }
            } else {
                LOG.error("Exception on Subscribe RPC for {}", deviceId, throwable);
            }
        }

        @Override
        public void onCompleted() {
            complete();
            LOG.warn("Subscribe RPC for {} has completed", deviceId);
        }
    }

    @Override
    @SuppressWarnings("NoFinalizer")
    protected void finalize() throws Throwable {
        if (!streamCheckerExecutor.isShutdown()) {
            LOG.error("Finalizing object but executor is still active! BUG? Shutting down...");
            shutdown();
        }
        super.finalize();
    }
}


