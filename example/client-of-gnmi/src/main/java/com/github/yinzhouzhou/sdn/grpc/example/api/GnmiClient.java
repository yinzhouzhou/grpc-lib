

package com.github.yinzhouzhou.sdn.grpc.example.api;

import com.github.yinzhouzhou.sdn.grpc.client.api.GrpcClient;
import com.github.yinzhouzhou.sdn.grpc.gnmi.protobuff.messages.Gnmi;
import com.google.common.annotations.Beta;
import java.util.concurrent.CompletableFuture;

/**
 * Client to control a gNMI server.
 */
@Beta
public interface GnmiClient extends GrpcClient {

    /**
     * Gets capability from a target.
     *
     * @return the capability response
     */
    CompletableFuture<Gnmi.CapabilityResponse> capabilities();

    /**
     * Retrieves a snapshot of data from the device.
     *
     * @param request the get request
     * @return the snapshot of data from the device
     */
    CompletableFuture<Gnmi.GetResponse> get(Gnmi.GetRequest request);

    /**
     * Modifies the state of data on the device.
     *
     * @param request the set request
     * @return the set result
     */
    CompletableFuture<Gnmi.SetResponse> set(Gnmi.SetRequest request);

    /**
     * Starts a subscription for the given request. Updates will be notified by
     * the controller via {@link GnmiEvent.Type#UPDATE} events. The client
     * guarantees that a Subscription RPC is active at all times despite channel
     * or server failures, unless {@link #unsubscribe()} is called.
     *
     * @param request the subscribe request
     */
    void subscribe(Gnmi.SubscribeRequest request);

    /**
     * Terminates any Subscribe RPC active.
     */
    void unsubscribe();
}
