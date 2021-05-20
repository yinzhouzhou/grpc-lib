
package com.github.yinzhouzhou.sdn.grpc.client.api;

import com.google.common.annotations.Beta;
import java.util.concurrent.CompletableFuture;

/**
 * Abstraction of a gRPC client.
 */
@Beta
@SuppressWarnings({"JavadocParagraph", "JavadocTagContinuationIndentation"})
public interface GrpcClient {

    /**
     * Shutdowns the client by terminating any active RPC.
     */
    void shutdown();

    /**
     * This method provides a coarse modelling of gRPC channel {@link
     * io.grpc.ConnectivityState}. The implementation does not make any attempt
     * at probing the server by sending messages over the network, as such it
     * does not block execution. It returns true if this client is expected to
     * communicate with the server successfully. In other words, if any RPC
     * would be executed immediately after this method is called and returns
     * true, then it is expected, but NOT guaranteed, for that RPC message to
     * reach the server and be processed. If false, it means the channel is in a
     * failure state and communication with the server is unlikely to happen
     * soon.
     *
     * @return true if server is deemed reachable, false otherwise
     */
    boolean isServerReachable();

    /**
     * Similar to {@link #isServerReachable()}, but might involve sending
     * packets over the network. This checks whether the specific gRPC
     * service(s) required by this client is available or not on the server.
     *
     * @return completable future eventually true if the gRPC service(s) on the
     * server was available during the probe; false otherwise
     */
    CompletableFuture<Boolean> probeService();
}
