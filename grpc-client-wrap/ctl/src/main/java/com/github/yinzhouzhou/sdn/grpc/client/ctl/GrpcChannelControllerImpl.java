

package com.github.yinzhouzhou.sdn.grpc.client.ctl;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Strings.isNullOrEmpty;
import static java.lang.String.format;

import com.github.yinzhouzhou.sdn.grpc.client.api.GrpcChannelController;
import com.google.common.util.concurrent.Striped;
import io.grpc.LoadBalancerRegistry;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.NameResolverRegistry;
import io.grpc.internal.DnsNameResolverProvider;
import io.grpc.internal.PickFirstLoadBalancerProvider;
import io.grpc.netty.shaded.io.grpc.netty.GrpcSslContexts;
import io.grpc.netty.shaded.io.grpc.netty.NettyChannelBuilder;
import io.grpc.netty.shaded.io.netty.handler.ssl.SslContext;
import io.grpc.netty.shaded.io.netty.handler.ssl.util.InsecureTrustManagerFactory;
import java.net.URI;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.Lock;
import javax.net.ssl.SSLException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Default implementation of the GrpcChannelController.
 */

public class GrpcChannelControllerImpl implements GrpcChannelController {

    private static final String GRPC = "grpc";
    private static final String GRPCS = "grpcs";

    private static final int DEFAULT_MAX_INBOUND_MSG_SIZE = 256; // Megabytes.
    private static final int MEGABYTES = 1024 * 1024;

    private static final PickFirstLoadBalancerProvider PICK_FIRST_LOAD_BALANCER_PROVIDER =
            new PickFirstLoadBalancerProvider();
    private static final DnsNameResolverProvider DNS_NAME_RESOLVER_PROVIDER =
            new DnsNameResolverProvider();

    /**
     * Indicates whether to log gRPC messages.
     */
    private AtomicBoolean enableMessageLog = new AtomicBoolean(false);

    private final Logger log = LoggerFactory.getLogger(getClass());

    private Map<URI, ManagedChannel> channels;
    private Map<URI, GrpcLoggingInterceptor> interceptors;

    private final Striped<Lock> channelLocks = Striped.lock(30);

    public void activate() {
        channels = new ConcurrentHashMap<>();
        interceptors = new ConcurrentHashMap<>();
        LoadBalancerRegistry.getDefaultRegistry()
                .register(PICK_FIRST_LOAD_BALANCER_PROVIDER);
        NameResolverRegistry.getDefaultRegistry()
                .register(DNS_NAME_RESOLVER_PROVIDER);
        log.info("Started");
    }

    public void deactivate() {
        LoadBalancerRegistry.getDefaultRegistry()
                .deregister(PICK_FIRST_LOAD_BALANCER_PROVIDER);
        NameResolverRegistry.getDefaultRegistry()
                .register(DNS_NAME_RESOLVER_PROVIDER);
        channels.values().forEach(ManagedChannel::shutdownNow);
        channels.clear();
        channels = null;
        interceptors.values().forEach(GrpcLoggingInterceptor::close);
        interceptors.clear();
        interceptors = null;
        log.info("Stopped");
    }

    @Override
    public ManagedChannel create(URI channelUri) {
        return create(channelUri, makeChannelBuilder(channelUri));
    }

    @Override
    public ManagedChannel create(URI channelUri, ManagedChannelBuilder<?> channelBuilder) {
        checkNotNull(channelUri);
        checkNotNull(channelBuilder);

        channelLocks.get(channelUri).lock();
        try {
            if (channels.containsKey(channelUri)) {
                throw new IllegalArgumentException(format(
                        "A channel with ID '%s' already exists", channelUri));
            }

            log.info("Creating new gRPC channel {}...", channelUri);

            final GrpcLoggingInterceptor interceptor = new GrpcLoggingInterceptor(
                    channelUri, enableMessageLog);
            channelBuilder.intercept(interceptor);

            final ManagedChannel channel = channelBuilder.build();

            channels.put(channelUri, channel);
            interceptors.put(channelUri, interceptor);

            return channel;
        } finally {
            channelLocks.get(channelUri).unlock();
        }
    }

    private NettyChannelBuilder makeChannelBuilder(URI channelUri) {

        checkArgument(channelUri.getScheme().equals(GRPC)
                              || channelUri.getScheme().equals(GRPCS),
                      format("Server URI scheme must be %s or %s", GRPC, GRPCS));
        checkArgument(!isNullOrEmpty(channelUri.getHost()),
                      "Server host address should not be empty");
        checkArgument(channelUri.getPort() > 0 && channelUri.getPort() <= 65535,
                      "Invalid server port");

        final boolean useTls = channelUri.getScheme().equals(GRPCS);

        final NettyChannelBuilder channelBuilder = NettyChannelBuilder
                .forAddress(channelUri.getHost(), channelUri.getPort())
                .nameResolverFactory(DNS_NAME_RESOLVER_PROVIDER)
                .defaultLoadBalancingPolicy(
                        PICK_FIRST_LOAD_BALANCER_PROVIDER.getPolicyName())
                .maxInboundMessageSize(
                        DEFAULT_MAX_INBOUND_MSG_SIZE * MEGABYTES);

        if (useTls) {
            try {
                // Accept any server certificate; this is insecure and
                // should not be used in production.
                final SslContext sslContext = GrpcSslContexts.forClient().trustManager(
                        InsecureTrustManagerFactory.INSTANCE).build();
                channelBuilder.sslContext(sslContext).useTransportSecurity();
            } catch (SSLException e) {
                log.error("Failed to build SSL context", e);
                return null;
            }
        } else {
            channelBuilder.usePlaintext();
        }

        return channelBuilder;
    }

    @Override
    public void destroy(URI channelUri) {
        checkNotNull(channelUri);

        channelLocks.get(channelUri).lock();
        try {
            final ManagedChannel channel = channels.remove(channelUri);
            if (channel != null) {
                shutdownNowAndWait(channel, channelUri);
            }
            final GrpcLoggingInterceptor interceptor = interceptors.remove(channelUri);
            if (interceptor != null) {
                interceptor.close();
            }
        } finally {
            channelLocks.get(channelUri).unlock();
        }
    }

    private void shutdownNowAndWait(ManagedChannel channel, URI channelUri) {
        try {
            if (!channel.shutdownNow()
                    .awaitTermination(5, TimeUnit.SECONDS)) {
                log.error("Channel {} did not terminate properly",
                          channelUri);
            }
        } catch (InterruptedException e) {
            log.warn("Channel {} didn't shutdown in time", channelUri);
            Thread.currentThread().interrupt();
        }
    }

    @Override
    public Optional<ManagedChannel> get(URI channelUri) {
        checkNotNull(channelUri);
        return Optional.ofNullable(channels.get(channelUri));
    }
}
