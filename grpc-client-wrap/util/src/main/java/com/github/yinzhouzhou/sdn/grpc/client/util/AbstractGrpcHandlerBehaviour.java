

package com.github.yinzhouzhou.sdn.grpc.client.util;

import com.github.yinzhouzhou.sdn.grpc.client.api.GrpcChannelController;
import com.github.yinzhouzhou.sdn.grpc.client.api.GrpcClient;
import com.github.yinzhouzhou.sdn.grpc.client.api.GrpcClientController;
import com.github.yinzhouzhou.sdn.grpc.client.api.device.DeviceId;
import com.google.common.collect.Maps;
import java.net.URI;
import java.util.concurrent.ConcurrentMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Abstract implementation of HandlerBehaviour for gNMI-based devices.
 *
 * @param <CLIENT> gRPC client class
 */
public class AbstractGrpcHandlerBehaviour<CLIENT extends GrpcClient> {

    static final ConcurrentMap<DeviceId, URI> CHANNEL_URIS = Maps.newConcurrentMap();

    protected final Logger log = LoggerFactory.getLogger(getClass());

    protected GrpcClientController clientController;
    protected GrpcChannelController channelController;

    private DeviceId deviceId;
    protected CLIENT client;

    public AbstractGrpcHandlerBehaviour(GrpcChannelController channelController,
                                        GrpcClientController clientController,
                                        DeviceId deviceId) {
        this.channelController = channelController;
        this.clientController = clientController;
        this.deviceId = deviceId;
    }

    public DeviceId getDeviceId() {
        return deviceId;
    }

    protected boolean setupBehaviour(String opName) {
        client = getClientByNetcfg();
        if (client == null) {
            log.warn("Missing client for {}, aborting {}", deviceId, opName);
            return false;
        }

        return true;
    }

    private CLIENT getClientByNetcfg() {
        // Check if there's a channel for this device and if we created it with
        // the same URI of that derived from the current netcfg. This makes sure
        // we return null if the netcfg changed after we created the channel.
        if (!CHANNEL_URIS.containsKey(deviceId)) {
            return null;
        }
        return (CLIENT) clientController.get(deviceId);
    }
}
