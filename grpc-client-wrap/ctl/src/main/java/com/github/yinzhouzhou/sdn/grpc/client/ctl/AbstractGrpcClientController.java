package com.github.yinzhouzhou.sdn.grpc.client.ctl;

import static com.google.common.base.Preconditions.checkNotNull;
import static java.lang.String.format;
import static org.slf4j.LoggerFactory.getLogger;

import com.github.yinzhouzhou.sdn.grpc.client.api.GrpcClient;
import com.github.yinzhouzhou.sdn.grpc.client.api.GrpcClientController;
import com.github.yinzhouzhou.sdn.grpc.client.api.device.DeviceId;
import com.google.common.collect.Maps;
import com.google.common.util.concurrent.Striped;
import io.grpc.ManagedChannel;
import java.util.Map;
import java.util.concurrent.locks.Lock;
import java.util.function.Supplier;
import org.slf4j.Logger;

/**
 * Abstract class of a controller for gRPC clients which provides means to
 * create clients, associate device agent listeners to them and register other
 * event listeners.
 *
 * @param <C> the gRPC client type
 */
public abstract class AbstractGrpcClientController<C extends GrpcClient, E> implements GrpcClientController<C> {

    /**
     * The default max inbound message size (MB).
     */
    private static final int DEFAULT_DEVICE_LOCK_SIZE = 30;

    private final Logger log = getLogger(getClass());

    private final Map<DeviceId, C> clients = Maps.newHashMap();

//    private final ConcurrentMap<DeviceId, ConcurrentMap<ProviderId, DeviceAgentListener>>
//            deviceAgentListeners = Maps.newConcurrentMap();
    private final String serviceName;
    private final Striped<Lock> stripedLocks = Striped.lock(DEFAULT_DEVICE_LOCK_SIZE);

    public AbstractGrpcClientController(String serviceName) {
        this.serviceName = serviceName;
    }

    public void init() {
        log.info("Started");
    }

    public void close() {
        clients.clear();
        log.info("Stopped");
    }

    @Override
    public boolean create(DeviceId deviceId, ManagedChannel channel) {
        checkNotNull(deviceId);
        checkNotNull(channel);
        return withDeviceLock(() -> doCreateClient(deviceId, channel), deviceId);
    }

    @SuppressWarnings("IllegalCatch")
    private boolean doCreateClient(DeviceId deviceId, ManagedChannel channel) {

        if (clients.containsKey(deviceId)) {
            throw new IllegalArgumentException(format(
                    "A %s client already exists for %s", serviceName, deviceId));
        }

        log.debug("Creating {}...", clientName(deviceId));

        final C client;
        try {
            client = createClientInstance(deviceId, channel);
        } catch (Throwable e) {
            log.error("Exception while creating {}", clientName(deviceId), e);
            return false;
        }

        if (client == null) {
            log.error("Unable to create {}, implementation returned null...",
                      clientName(deviceId));
            return false;
        }

        clients.put(deviceId, client);
        return true;
    }

    protected abstract C createClientInstance(DeviceId deviceId, ManagedChannel channel);

    @Override
    public C get(DeviceId deviceId) {
        checkNotNull(deviceId);
        return withDeviceLock(() -> clients.get(deviceId), deviceId);
    }

    @Override
    public void remove(DeviceId deviceId) {
        checkNotNull(deviceId);
        withDeviceLock(() -> {
            final C client = clients.remove(deviceId);
            if (client != null) {
                log.debug("Removing {}...", clientName(deviceId));
                client.shutdown();
            }
            return null;
        }, deviceId);
    }

    public void postEvent(E event) {
        // We should have only one event delivery mechanism. We have two now
        // because we have two different types of events, DeviceAgentEvent and
        // controller/protocol specific ones (e.g. P4Runtime or gNMI).
        // TODO: extend device agent event to allow delivery of protocol-specific
        //  events, e.g. packet-in
        checkNotNull(event);
//        if (deviceAgentListeners.containsKey(event.getDeviceId())) {
//            deviceAgentListeners.get(event.getDeviceId()).values()
//                    .forEach(l -> l /*l.event(event)*/);
//        }
    }

    private <U> U withDeviceLock(Supplier<U> task, DeviceId deviceId) {
        final Lock lock = stripedLocks.get(deviceId);
        lock.lock();
        try {
            return task.get();
        } finally {
            lock.unlock();
        }
    }

    private String clientName(DeviceId deviceId) {
        return format("%s client for %s", serviceName, deviceId);
    }
}
