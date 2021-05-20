package com.github.yinzhouzhou.sdn.grpc.example;

import com.github.yinzhouzhou.sdn.grpc.client.api.device.DeviceAgentEvent;
import com.github.yinzhouzhou.sdn.grpc.client.api.device.DeviceId;
import com.github.yinzhouzhou.sdn.grpc.client.ctl.AbstractGrpcClientController;
import io.grpc.ManagedChannel;

/**
 * 创建人: yinzhou
 * 创建时间 2021-05-13 20:21
 * 功能描述:.
 **/

public class GreeterClientController extends AbstractGrpcClientController<GreeterClient, DeviceAgentEvent> {

    public GreeterClientController() {
        super("HelloWorld");
    }

    @Override
    protected GreeterClient createClientInstance(DeviceId deviceId, ManagedChannel channel) {
        return new GreeterClientImpl(deviceId, channel, this);
    }
}
