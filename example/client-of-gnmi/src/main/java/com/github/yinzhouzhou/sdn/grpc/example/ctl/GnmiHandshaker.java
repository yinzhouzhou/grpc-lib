

package com.github.yinzhouzhou.sdn.grpc.example.ctl;

import com.github.yinzhouzhou.sdn.grpc.client.api.GrpcChannelController;
import com.github.yinzhouzhou.sdn.grpc.client.api.GrpcClientController;
import com.github.yinzhouzhou.sdn.grpc.client.api.device.DeviceId;
import com.github.yinzhouzhou.sdn.grpc.client.api.device.MastershipRole;
import com.github.yinzhouzhou.sdn.grpc.client.util.AbstractGrpcHandshaker;
import com.github.yinzhouzhou.sdn.grpc.example.api.GnmiClient;
import com.github.yinzhouzhou.sdn.grpc.example.api.GnmiController;
import java.util.concurrent.CompletableFuture;

/**
 * Implementation of DeviceHandshaker for gNMI.
 */
public class GnmiHandshaker
        extends AbstractGrpcHandshaker<GnmiClient, GnmiController> {


    public GnmiHandshaker(GrpcChannelController channelController,
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
