
package com.github.yinzhouzhou.sdn.grpc.example;

import com.github.yinzhouzhou.sdn.grpc.helloworld.protobuff.messages.HelloReply;
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

public class HelloReplyObserver implements StreamObserver<HelloReply> {

    private static final Logger LOG = LoggerFactory.getLogger(HelloReplyObserver.class);

    private Consumer<HelloReply> consumer;

    // 打印字符串，仅用于识别协议编码方式
    private String dataType = "protocol buffer";

    public HelloReplyObserver(Consumer<HelloReply> consumer) {
        this.consumer = consumer;
    }

    private AtomicInteger requestCount = new AtomicInteger(0);

    @SuppressWarnings("IllegalCatch")
    @Override
    public void onNext(HelloReply reply) {
        LOG.trace("Received No.{} client's message is :{}", requestCount.incrementAndGet(), reply);
        try {
            consumer.accept(reply);
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
    }

    public void setDataType(String dataType) {
        this.dataType = dataType;
    }
}
