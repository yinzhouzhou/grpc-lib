
package com.github.yinzhouzhou.sdn.grpc.client.api;

import com.google.common.annotations.Beta;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import java.net.URI;
import java.util.Optional;

/**
 * Abstraction of a gRPC controller that creates, stores, and manages gRPC
 * channels.
 */
@Beta
@SuppressWarnings("JavadocParagraph")
public interface GrpcChannelController {

    /**
     * Creates a gRPC managed channel to the server identified by the given
     * channel URI. The channel is created using the information contained in the
     * URI, as such, the URI is expected to have absolute server-based form,
     * where the scheme can be either {@code grpc:} or {@code grpcs:}, to
     * indicated respectively a plaintext or secure channel.
     * <p>
     * Example of valid URIs are: <pre> {@code
     * grpc://10.0.0.1:50001
     * grpcs://10.0.0.1:50001
     * grpcs://myserver.local:50001
     * }</pre>
     * <p>
     * This method creates and stores the channel instance associating it to the
     * passed URI, but it does not make any attempt to connect the channel or
     * verify server reachability.
     * <p>
     * If another channel with the same  URI already exists, an {@link
     * IllegalArgumentException} is thrown. To create multiple channels to the
     * same server-port combination, URI file or query parameters can be used.
     * For example: <pre> {@code
     * grpc://10.0.0.1:50001/foo
     * grpc://10.0.0.1:50001/bar
     * grpc://10.0.0.1:50001/bar?param=1
     * grpc://10.0.0.1:50001/bar?param=2
     * }</pre>
     * <p>
     * When creating secure channels (i.e., {@code grpcs:)}, the current
     * implementation provides encryption but not authentication, any server
     * certificate, even if insecure, will be accepted.
     *
     * @param channelUri channel URI
     * @return the managed channel created
     * @throws IllegalArgumentException if a channel with the same channel URI
     *                                  already exists
     */
    ManagedChannel create(URI channelUri);

    /**
     * Similar to {@link #create(URI)} but does not create the chanel instance,
     * instead, it uses the given channel builder to create it. As such, there
     * is no requirement on the format of the URI, any URI can be used. The
     * implementation might modify the passed builder for purposes specific to
     * this controller, such as to enable gRPC message logging.
     *
     * @param channelUri     URI identifying the channel
     * @param channelBuilder builder of the managed channel
     * @return the managed channel created
     * @throws IllegalArgumentException if a channel with the same ID already
     *                                  exists
     */
    ManagedChannel create(URI channelUri,
                          ManagedChannelBuilder<?> channelBuilder);

    /**
     * Closes and destroys the gRPC channel associated to the given URI and
     * removes any internal state associated to it.
     *
     * @param channelUri URI of the channel to remove
     */
    void destroy(URI channelUri);

    /**
     * If present, returns the channel associated with the given URI.
     *
     * @param channelUri channel URI
     * @return optional channel
     */
    Optional<ManagedChannel> get(URI channelUri);
}
