

package com.github.yinzhouzhou.sdn.grpc.example.ctl;

import com.github.yinzhouzhou.sdn.grpc.client.api.device.DeviceId;
import com.github.yinzhouzhou.sdn.grpc.client.ctl.AbstractGrpcClientController;
import com.github.yinzhouzhou.sdn.grpc.example.api.GnmiClient;
import com.github.yinzhouzhou.sdn.grpc.example.api.GnmiController;
import com.github.yinzhouzhou.sdn.grpc.example.api.GnmiEvent;
import io.grpc.ManagedChannel;

/**
 * Implementation of gNMI controller.
 */
public class GnmiClientControllerImpl extends AbstractGrpcClientController<GnmiClient, GnmiEvent>
        implements GnmiController {

    public GnmiClientControllerImpl() {
        super("gNMI");
    }

    @Override
    protected GnmiClient createClientInstance(
        DeviceId deviceId, ManagedChannel channel) {
        return new GnmiClientImpl(deviceId, channel, this);
    }
}
