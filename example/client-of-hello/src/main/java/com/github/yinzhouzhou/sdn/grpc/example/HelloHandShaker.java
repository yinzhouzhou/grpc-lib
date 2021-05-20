
package com.github.yinzhouzhou.sdn.grpc.example;

import com.github.yinzhouzhou.sdn.grpc.client.api.GrpcChannelController;
import com.github.yinzhouzhou.sdn.grpc.client.api.GrpcClientController;
import com.github.yinzhouzhou.sdn.grpc.client.api.device.DeviceId;
import com.github.yinzhouzhou.sdn.grpc.client.api.device.MastershipRole;
import com.github.yinzhouzhou.sdn.grpc.client.util.AbstractGrpcHandshaker;
import java.util.concurrent.CompletableFuture;

/**
 * 创建人: yinzhou
 * 创建时间 2021-05-13 16:55
 * 功能描述:.
 **/

public class HelloHandShaker extends AbstractGrpcHandshaker<GreeterClient, GreeterClientController> {

    public HelloHandShaker(GrpcChannelController channelController,
                           GrpcClientController clientController,
                           DeviceId deviceId) {
        super(channelController, clientController, deviceId);
    }

    @Override
    public boolean isAvailable() {
        return isReachable();
    }

    @Override
    public CompletableFuture<Boolean> probeAvailability() {
        return probeReachability();
    }

    @Override
    public void roleChanged(MastershipRole newRole) {
        throw new UnsupportedOperationException("Mastership operation not supported");
    }

    @Override
    public MastershipRole getRole() {
        throw new UnsupportedOperationException("Mastership operation not supported");
    }
}
