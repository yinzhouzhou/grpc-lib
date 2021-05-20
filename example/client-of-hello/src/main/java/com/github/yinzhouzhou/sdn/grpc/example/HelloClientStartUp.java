
package com.github.yinzhouzhou.sdn.grpc.example;

import com.github.yinzhouzhou.sdn.grpc.client.api.device.DeviceId;
import com.github.yinzhouzhou.sdn.grpc.client.ctl.GrpcChannelControllerImpl;
import com.github.yinzhouzhou.sdn.grpc.helloworld.protobuff.messages.HelloRequest;
import java.util.concurrent.ExecutionException;

/**
 * 创建人: yinzhou
 * 创建时间 2021-05-13 16:58
 * 功能描述:.
 **/

@SuppressWarnings({"RegexpSinglelineJava", "HideUtilityClassConstructor"})
public final class HelloClientStartUp {

    public static void main(String[] args) throws InterruptedException, ExecutionException {
        // Component初始化, serviceName表示？？
        GrpcChannelControllerImpl grpcChannelController = new GrpcChannelControllerImpl();
        grpcChannelController.activate();
        GreeterClientController clientController = new GreeterClientController();

        DeviceId deviceId = DeviceId.deviceId("grpc://127.0.0.1:9980");
        // grpcs表示附带TLS加密
        // DeviceId deviceId = DeviceId.deviceId("grpcs://127.0.0.1:9980");

        long start;
        start = System.currentTimeMillis();
        HelloHandShaker handShaker = new HelloHandShaker(
            grpcChannelController,
            clientController,
            deviceId);
        handShaker.connect();
        System.out.println("connect take time :" + (System.currentTimeMillis() - start));

        //******** 非流式RPC *********//
        /* 不要直接使用grpcChannelController获取managedChannel来发消息
         * 尽量用clientController获取client来发消息，client用于聚合一个proto中的RPC的实现
         */
        HelloRequest.Builder helloRequest = HelloRequest.newBuilder().setName("onos-test");
//        grpcChannelController.get(deviceId.uri()).ifPresent(managedChannel -> {
//            GreeterGrpc.GreeterBlockingStub stub = GreeterGrpc.newBlockingStub(managedChannel);
//            HelloReply helloReply = stub.sayHello(helloRequest);
//            System.out.println(helloReply);
//        });
        // 使用client来发送rpc消息, 如果发送失败请检查两端proto文件是否一致
        GreeterClient client = clientController.get(deviceId);
        //System.out.println(client.sayHello(helloRequest.build()).get());
        System.out.println("sayHello take time :" + (System.currentTimeMillis() - start));

        //******** 流式RPC *********//
        helloRequest.setName("onos-test-stream");
        //client.sayHello2(helloRequest.build());
        System.out.println("sayHello2 take time :" + (System.currentTimeMillis() - start));

        // 优雅地关闭连接
        Thread.sleep(1000);
        System.out.println(client.isServerReachable());
        client.shutdown();
    }
}
