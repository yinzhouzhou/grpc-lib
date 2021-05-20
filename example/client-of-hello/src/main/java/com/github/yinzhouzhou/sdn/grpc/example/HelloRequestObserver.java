
package com.github.yinzhouzhou.sdn.grpc.example;

import com.github.yinzhouzhou.sdn.grpc.helloworld.protobuff.messages.HelloReply;
import com.github.yinzhouzhou.sdn.grpc.helloworld.protobuff.messages.HelloRequest;
import io.grpc.stub.StreamObserver;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 创建人: yinzhou
 * 创建时间 2020-11-27 14:12
 * 功能描述:.
 **/

public class HelloRequestObserver implements StreamObserver<HelloRequest> {

    private static final Logger LOG = LoggerFactory.getLogger(HelloRequestObserver.class);

    private StreamObserver<HelloReply> responseObserver;
    private Consumer<HelloRequest> consumer;

    // 打印字符串，仅用于识别协议编码方式
    private String dataType = "protocol buffer";

    public HelloRequestObserver(StreamObserver<HelloReply> responseObserver,
                                Consumer<HelloRequest> consumer) {
        this.responseObserver = responseObserver;
        this.consumer = consumer;
    }

    private AtomicInteger requestCount = new AtomicInteger(0);

    @SuppressWarnings("IllegalCatch")
    @Override
    public void onNext(HelloRequest request) {
        LOG.trace("Received No.{} client's message is :{}", requestCount.incrementAndGet(), request);
        try {
            consumer.accept(request);
        } catch (Exception e) {
            LOG.error("process a request stream error.", e);
        }
    }

    @Override
    public void onError(Throwable throwable) {
        LOG.info("Throwable occurred when process No.{} request.", requestCount.get());
    }

    @Override
    public void onCompleted() {
        LOG.debug("Received current rpc onCompleted event. All request-num is {}", requestCount.get());
        HelloReply reply = HelloReply.newBuilder()
            .setMessage("OK, We have Done For " + dataType + " Data.")
            .build();
        responseObserver.onNext(reply);
        responseObserver.onCompleted();
    }

    public HelloRequestObserver setDataType(String dataType) {
        this.dataType = dataType;
        return this;
    }
}
