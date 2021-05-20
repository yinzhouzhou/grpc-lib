
package com.github.yinzhouzhou.sdn.grpc.example.ctl;

import com.github.yinzhouzhou.sdn.grpc.client.api.device.DeviceId;
import com.github.yinzhouzhou.sdn.grpc.client.ctl.GrpcChannelControllerImpl;
import com.github.yinzhouzhou.sdn.grpc.example.api.GnmiClient;
import com.github.yinzhouzhou.sdn.grpc.gnmi.protobuff.messages.Gnmi;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.concurrent.ExecutionException;

/**
 * 创建人: yinzhou
 * 创建时间 2021-05-13 16:58
 * 功能描述:.
 **/

@SuppressWarnings({"HideUtilityClassConstructor"})
@SuppressFBWarnings(value = {"DLS_DEAD_LOCAL_STORE"}, justification = "1")
public final class GnmiClientStartUp {

    public static void main(String[] args) throws InterruptedException, ExecutionException {
        // Component初始化, serviceName表示？？
        GrpcChannelControllerImpl grpcChannelController = new GrpcChannelControllerImpl();
        grpcChannelController.activate();
        GnmiClientControllerImpl clientController = new GnmiClientControllerImpl();

        DeviceId deviceId = DeviceId.deviceId("grpc://127.0.0.1:9980");
        // grpcs表示附带TLS加密
        // DeviceId deviceId = DeviceId.deviceId("grpcs://127.0.0.1:9980");

        GnmiHandshaker handShaker = new GnmiHandshaker(
            grpcChannelController,
            clientController,
            deviceId);
        handShaker.connect();

        //******** 非流式RPC *********//
//        Gnmi.CapabilityRequest.Builder capRequest = Gnmi.CapabilityRequest.newBuilder()
//            .addExtension(
//                GnmiExt.Extension.newBuilder().setMasterArbitration(GnmiExt.MasterArbitration.newBuilder()));
        Gnmi.GetRequest.Builder getRequest = Gnmi.GetRequest.newBuilder().setEncoding(Gnmi.Encoding.JSON);
        Gnmi.SetRequest.Builder setRequest = Gnmi.SetRequest.newBuilder().addDelete(Gnmi.Path.newBuilder());
        // 使用client来发送rpc消息, 如果发送失败请检查两端proto文件是否一致
        GnmiClient client = clientController.get(deviceId);
        //out.println(client.capabilities().get());
        //out.println(client.get(getRequest.build()).get());
        //out.println(client.set(setRequest.build()).get());

        //******** 流式RPC *********//
        Gnmi.SubscribeRequest.Builder subscribeRequest = Gnmi.SubscribeRequest.newBuilder()
            .setSubscribe(Gnmi.SubscriptionList.newBuilder());
        client.subscribe(subscribeRequest.build());

        // 优雅地关闭连接
        Thread.sleep(1000);
        client.shutdown();
    }
}
