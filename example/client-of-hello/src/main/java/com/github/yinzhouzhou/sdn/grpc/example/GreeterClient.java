package com.github.yinzhouzhou.sdn.grpc.example;

import com.github.yinzhouzhou.sdn.grpc.client.api.GrpcClient;
import com.github.yinzhouzhou.sdn.grpc.helloworld.protobuff.messages.HelloReply;
import com.github.yinzhouzhou.sdn.grpc.helloworld.protobuff.messages.HelloRequest;
import java.util.concurrent.CompletableFuture;

/**
 * Client to control a gNMI server.
 */
public interface GreeterClient extends GrpcClient {


    /**
     * Retrieves a snapshot of data from the device.
     *
     * @param request the get request
     * @return the snapshot of data from the device
     */
    CompletableFuture<HelloReply> sayHello(HelloRequest request);

    /**
     * Retrieves a snapshot of data from the device.
     *
     * @param request the get request
     * @return the snapshot of data from the device
     */
    void sayHello2(HelloRequest request);
}
