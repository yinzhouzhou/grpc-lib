
package com.github.yinzhouzhou.sdn.grpc.example.ctl;

import com.github.yinzhouzhou.sdn.grpc.client.api.device.DeviceId;
import com.github.yinzhouzhou.sdn.grpc.client.ctl.AbstractGrpcClient;
import com.github.yinzhouzhou.sdn.grpc.example.api.GnmiClient;
import com.github.yinzhouzhou.sdn.grpc.gnmi.protobuff.messages.Gnmi;
import com.github.yinzhouzhou.sdn.grpc.gnmi.protobuff.messages.gNMIGrpc;
import io.grpc.ManagedChannel;
import io.grpc.Status;
import io.grpc.stub.StreamObserver;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

/**
 * Implementation of gNMI client.
 */
public class GnmiClientImpl extends AbstractGrpcClient implements GnmiClient {

    private static final int RPC_TIMEOUT_SECONDS = 10;

    private static final Gnmi.GetRequest PING_REQUEST = Gnmi.GetRequest.newBuilder().addPath(
            Gnmi.Path.newBuilder().addElem(
                    Gnmi.PathElem.newBuilder().setName("onos-gnmi-ping").build()
            ).build()).build();

    private GnmiSubscriptionManager subscribeManager;

    GnmiClientImpl(DeviceId deviceId, ManagedChannel managedChannel,
                   GnmiClientControllerImpl controller) {
        super(deviceId, managedChannel, false, controller);
        this.subscribeManager =
                new GnmiSubscriptionManager(this, deviceId, controller);
    }

    @Override
    public CompletableFuture<Gnmi.CapabilityResponse> capabilities() {
        final CompletableFuture<Gnmi.CapabilityResponse> future = new CompletableFuture<>();
        execRpc(s -> s.capabilities(
                Gnmi.CapabilityRequest.getDefaultInstance(),
                unaryObserver(future, Gnmi.CapabilityResponse.getDefaultInstance(),
                              "capabilities request"))
        );
        return future;
    }

    @Override
    public CompletableFuture<Gnmi.GetResponse> get(Gnmi.GetRequest request) {
        final CompletableFuture<Gnmi.GetResponse> future = new CompletableFuture<>();
        execRpc(s -> s.get(request, unaryObserver(
                future, Gnmi.GetResponse.getDefaultInstance(), "GET"))
        );
        return future;
    }

    @Override
    public CompletableFuture<Gnmi.SetResponse> set(Gnmi.SetRequest request) {
        final CompletableFuture<Gnmi.SetResponse> future = new CompletableFuture<>();
        execRpc(s -> s.set(request, unaryObserver(
                future, Gnmi.SetResponse.getDefaultInstance(), "SET"))
        );
        return future;
    }

    @SuppressWarnings("MethodTypeParameterName")
    private <RES> StreamObserver<RES> unaryObserver(
            final CompletableFuture<RES> future,
            final RES defaultResponse,
            final String opDescription) {
        return new StreamObserver<RES>() {
            @Override
            public void onNext(RES value) {
                future.complete(value);
            }

            @Override
            public void onError(Throwable throwable) {
                handleRpcError(throwable, opDescription);
                future.complete(defaultResponse);
            }

            @Override
            public void onCompleted() {
                // Ignore. Unary call.
            }
        };
    }

    @Override
    public void subscribe(Gnmi.SubscribeRequest request) {
        subscribeManager.subscribe(request);
    }

    @Override
    public void unsubscribe() {
        subscribeManager.unsubscribe();
    }

    @Override
    public CompletableFuture<Boolean> probeService() {
        final CompletableFuture<Boolean> future = new CompletableFuture<>();
        final StreamObserver<Gnmi.GetResponse> responseObserver = new StreamObserver<Gnmi.GetResponse>() {
            @Override
            public void onNext(Gnmi.GetResponse value) {
                future.complete(true);
            }

            @Override
            public void onError(Throwable throwable) {
                // This gRPC call should throw INVALID_ARGUMENT status exception
                // since "/onos-gnmi-ping" path does not exists in any config
                // model For other status code such as UNIMPLEMENT, means the
                // gNMI service is not available on the device.
                future.complete(Status.fromThrowable(throwable).getCode()
                                        == Status.Code.INVALID_ARGUMENT);
            }

            @Override
            public void onCompleted() {
                // Ignore. Unary call.
            }
        };
        execRpc(s -> s.get(PING_REQUEST, responseObserver));
        return future;
    }

    @Override
    public void shutdown() {
        subscribeManager.shutdown();
        super.shutdown();
    }

    /**
     * Forces execution of an RPC in a cancellable context with a timeout.
     *
     * @param stubConsumer P4Runtime stub consumer
     */
    private void execRpc(Consumer<gNMIGrpc.gNMIStub> stubConsumer) {
        if (log.isTraceEnabled()) {
            log.trace("Executing RPC with timeout {} seconds (context deadline {})...",
                      RPC_TIMEOUT_SECONDS, context().getDeadline());
        }
        runInCancellableContext(() -> stubConsumer.accept(
                gNMIGrpc.newStub(channel)
                        .withDeadlineAfter(RPC_TIMEOUT_SECONDS, TimeUnit.SECONDS)));
    }

    /**
     * Forces execution of an RPC in a cancellable context with no timeout.
     *
     * @param stubConsumer P4Runtime stub consumer
     */
    void execRpcNoTimeout(Consumer<gNMIGrpc.gNMIStub> stubConsumer) {
        if (log.isTraceEnabled()) {
            log.trace("Executing RPC with no timeout (context deadline {})...",
                      context().getDeadline());
        }
        runInCancellableContext(() -> stubConsumer.accept(
                gNMIGrpc.newStub(channel)));
    }
}
