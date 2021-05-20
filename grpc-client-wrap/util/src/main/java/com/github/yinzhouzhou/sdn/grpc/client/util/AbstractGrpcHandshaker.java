

package com.github.yinzhouzhou.sdn.grpc.client.util;

import static java.util.concurrent.CompletableFuture.completedFuture;

import com.github.yinzhouzhou.sdn.grpc.client.api.GrpcChannelController;
import com.github.yinzhouzhou.sdn.grpc.client.api.GrpcClient;
import com.github.yinzhouzhou.sdn.grpc.client.api.GrpcClientController;
import com.github.yinzhouzhou.sdn.grpc.client.api.device.DeviceHandshaker;
import com.github.yinzhouzhou.sdn.grpc.client.api.device.DeviceId;
import com.google.common.util.concurrent.Striped;
import io.grpc.ConnectivityState;
import io.grpc.ManagedChannel;
import java.net.URI;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.locks.Lock;

/**
 * Abstract implementation of DeviceHandshaker that uses gRPC to establish a
 * connection to the device.
 *
 * @param <CLIENT> gRPC client class
 * @param <CTRL>   gRPC controller class
 */
@SuppressWarnings("ClassTypeParameterName")
public abstract class AbstractGrpcHandshaker
    <CLIENT extends GrpcClient, CTRL extends GrpcClientController<CLIENT>>
    extends AbstractGrpcHandlerBehaviour<CLIENT>
    implements DeviceHandshaker {


    /**
     * Creates a new instance of this behaviour for the given gRPC controller
     * class.
     *
     * @param channelController gRPC channel controller class
     * @param clientController gRPC client controller class
     * @param deviceId DeviceId
     */
    public AbstractGrpcHandshaker(GrpcChannelController channelController,
                                  GrpcClientController clientController,
                                  DeviceId deviceId) {
        super(channelController, clientController, deviceId);
    }

    private static final Striped<Lock> DEVICE_LOCKS = Striped.lock(10);

    @Override
    public boolean connect() {
        final DeviceId deviceId = getDeviceId();

        DEVICE_LOCKS.get(deviceId).lock();
        try {
            if (clientController.get(deviceId) != null) {
                throw new IllegalStateException(
                    "A client for this device already exists");
            }

            // Create or get an existing channel. We support sharing the same
            // channel by different drivers for the same device.
            final ManagedChannel channel;
            final URI existingChannelUri = CHANNEL_URIS.get(deviceId);
            if (existingChannelUri != null) {
                channel = channelController.get(existingChannelUri)
                    .orElseThrow(() -> new IllegalStateException(
                        "Missing gRPC channel in controller"));
            } else {
                channel = channelController.create(deviceId.uri());
                // Store channel URI for future use.
                CHANNEL_URIS.put(deviceId, deviceId.uri());
                // Trigger connection.
                channel.getState(true);
            }

            return clientController.create(deviceId, channel);
        } finally {
            DEVICE_LOCKS.get(deviceId).unlock();
        }
    }

    @Override
    public boolean hasConnection() {
        final DeviceId deviceId = getDeviceId();
        // If a client already exists for this device, but the netcfg with the
        // server endpoints has changed, this will return false.
        DEVICE_LOCKS.get(deviceId).lock();
        try {
            final URI existingChannelUri = CHANNEL_URIS.get(deviceId);
            return existingChannelUri != null
                && channelController.get(existingChannelUri).isPresent()
                && clientController.get(deviceId) != null;
        } finally {
            DEVICE_LOCKS.get(deviceId).unlock();
        }
    }

    @Override
    public void disconnect() {
        final DeviceId deviceId = getDeviceId();
        // This removes any clients and channels associated with this device ID.
        DEVICE_LOCKS.get(deviceId).lock();
        try {
            final URI existingChannelUri = CHANNEL_URIS.remove(deviceId);
            clientController.remove(deviceId);
            if (existingChannelUri != null) {
                channelController.destroy(existingChannelUri);
            }
        } finally {
            DEVICE_LOCKS.get(deviceId).unlock();
        }
    }

    @Override
    public boolean isReachable() {
        return setupBehaviour("isReachable()") && client.isServerReachable();
    }

    @Override
    public CompletableFuture<Boolean> probeReachability() {
        if (!setupBehaviour("probeReachability()")) {
            return completedFuture(false);
        }
        resetChannelConnectBackoffIfNeeded();
        return client.probeService();
    }

    private void resetChannelConnectBackoffIfNeeded() {
        // Stimulate channel reconnect if in failure state.
        final ManagedChannel channel = getExistingChannel();
        if (channel == null) {
            // Where did the channel go?
            return;
        }
        if (channel.getState(false)
            .equals(ConnectivityState.TRANSIENT_FAILURE)) {
            channel.resetConnectBackoff();
        }
    }

    private ManagedChannel getExistingChannel() {
        final DeviceId deviceId = getDeviceId();
        if (CHANNEL_URIS.containsKey(deviceId)) {
            return channelController.get(CHANNEL_URIS.get(deviceId)).orElse(null);
        }
        return null;
    }
}
