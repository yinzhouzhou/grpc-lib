

package com.github.yinzhouzhou.sdn.grpc.example;


import com.github.yinzhouzhou.sdn.grpc.client.api.device.DeviceId;
import com.github.yinzhouzhou.sdn.grpc.client.ctl.AbstractGrpcClient;
import com.github.yinzhouzhou.sdn.grpc.helloworld.protobuff.messages.GreeterGrpc;
import com.github.yinzhouzhou.sdn.grpc.helloworld.protobuff.messages.HelloReply;
import com.github.yinzhouzhou.sdn.grpc.helloworld.protobuff.messages.HelloRequest;
import io.grpc.ManagedChannel;
import io.grpc.stub.ClientCallStreamObserver;
import io.grpc.stub.StreamObserver;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

/**
 * Implementation of gNMI client.
 */
@SuppressWarnings({"MethodTypeParameterName", "ParameterName", "RegexpSinglelineJava"})
public class GreeterClientImpl extends AbstractGrpcClient implements GreeterClient {

    private static final int RPC_TIMEOUT_SECONDS = 10;

    public GreeterClientImpl(DeviceId deviceId, ManagedChannel managedChannel,
                             GreeterClientController controller) {
        super(deviceId, managedChannel, false, controller);
    }

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
            public void onError(Throwable t) {
                handleRpcError(t, opDescription);
                future.complete(defaultResponse);
            }

            @Override
            public void onCompleted() {
                // Ignore. Unary call.
            }
        };
    }

    @Override
    public CompletableFuture<HelloReply> sayHello(HelloRequest request) {
        final CompletableFuture<HelloReply> future = new CompletableFuture<>();
        execRpc(s -> s.sayHello(request, unaryObserver(
            future, HelloReply.getDefaultInstance(), "GET"))
        );
        return future;
    }

    private HelloReplyObserver helloReplyObserver = new HelloReplyObserver(System.out::println);
    private ClientCallStreamObserver<HelloRequest> requestObserver;

    @Override
    public void sayHello2(HelloRequest request) {
        if (requestObserver == null) {
            log.debug("Starting new Subscribe RPC for {}...", deviceId);
            execRpcNoTimeout(stub -> {
                    requestObserver = (ClientCallStreamObserver<HelloRequest>) stub.sayHello2(helloReplyObserver);
                }
            );
        }
        requestObserver.onNext(request);
        requestObserver.onCompleted();
    }


    /**
     * Forces execution of an RPC in a cancellable context with a timeout.
     *
     * @param stubConsumer stub consumer
     */
    private void execRpc(Consumer<GreeterGrpc.GreeterStub> stubConsumer) {
        if (log.isTraceEnabled()) {
            log.trace("Executing RPC with timeout {} seconds (context deadline {})...",
                RPC_TIMEOUT_SECONDS, context().getDeadline());
        }
        runInCancellableContext(() -> stubConsumer.accept(
            GreeterGrpc.newStub(channel)
                .withDeadlineAfter(RPC_TIMEOUT_SECONDS, TimeUnit.SECONDS)));
    }

    /**
     * Forces execution of an RPC in a cancellable context with no timeout.
     *
     * @param stubConsumer stub consumer
     */
    private void execRpcNoTimeout(Consumer<GreeterGrpc.GreeterStub> stubConsumer) {
        if (log.isTraceEnabled()) {
            log.trace("Executing RPC with no timeout (context deadline {})...",
                context().getDeadline());
        }
        runInCancellableContext(() -> stubConsumer.accept(
            GreeterGrpc.newStub(channel)));
    }

    @Override
    public CompletableFuture<Boolean> probeService() {
        final CompletableFuture<Boolean> future = new CompletableFuture<>();
        return future;
    }

    @Override
    public void shutdown() {
        super.shutdown();
    }

}
