package com.github.yinzhouzhou.sdn.grpc.client;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.netty.shaded.io.grpc.netty.GrpcSslContexts;
import io.grpc.netty.shaded.io.grpc.netty.NettyChannelBuilder;
import io.grpc.netty.shaded.io.netty.handler.codec.http2.Http2SecurityUtil;
import io.grpc.netty.shaded.io.netty.handler.ssl.ApplicationProtocolConfig;
import io.grpc.netty.shaded.io.netty.handler.ssl.ApplicationProtocolNames;
import io.grpc.netty.shaded.io.netty.handler.ssl.ClientAuth;
import io.grpc.netty.shaded.io.netty.handler.ssl.SslContext;
import io.grpc.netty.shaded.io.netty.handler.ssl.SslContextBuilder;
import io.grpc.netty.shaded.io.netty.handler.ssl.SslProvider;
import io.grpc.netty.shaded.io.netty.handler.ssl.SupportedCipherSuiteFilter;
import java.io.File;
import java.util.concurrent.TimeUnit;
import javax.net.ssl.SSLException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DefaultGRpcClient {

    private final Logger logger = LoggerFactory.getLogger(DefaultGRpcClient.class);

    private String grpcServerHost;
    private Integer grpcServerPort;

    private ManagedChannel channel;

    public DefaultGRpcClient(String grpcServerHost, Integer grpcServerPort) {
        this.grpcServerHost = grpcServerHost;
        this.grpcServerPort = grpcServerPort;
    }

    /**
     * 开启grpc-channel，使用纯文本传输.
     *
     */
    public void openPlainTextChannel() {
        ManagedChannelBuilder channelBuilder = ManagedChannelBuilder.forAddress(grpcServerHost, grpcServerPort)
            .usePlaintext();
        this.channel = channelBuilder.build();
        logger.info("grpc-client connect started without ssl, message is plain text.");
    }

    /**
     * 开启grpc-channel，使用纯文本传输.
     *
     */
    public void openPlainTextChannel(long keepAliveTime) {
        ManagedChannelBuilder channelBuilder = ManagedChannelBuilder.forAddress(grpcServerHost, grpcServerPort)
            .usePlaintext();
        channelBuilder.keepAliveTime(keepAliveTime, TimeUnit.MILLISECONDS);
        this.channel = channelBuilder.build();
        logger.info("grpc-client connect started without ssl, message is plain text.");
    }

    /**
     * 开启单向认证(One-way authentication)的 grpc-channel，通道将使用ssl-context对消息加解密.
     *
     * @param serverCaFile 服务端的数字证书.
     * @param authority 授权机构（使用者）.
     *
     * @throws SSLException  when ssl shark hand failed. throw this exception.
     */
    public void openChannelWithOneWaySsl(File serverCaFile, String authority)
        throws SSLException {
        openChannelWithTwoWaySsl(serverCaFile, authority, null, null);
    }

    /**
     * 开启双向认证(Two-way authentication)的 grpc-channel，通道将使用ssl-context对消息加解密.
     *
     * @param serverCaFile 服务端的数字证书.
     * @param authority 授权机构（使用者）, 双向认证的服务端和客户端的证书的授权机构需要是同一个机构.
     * @param clientCaFile 客户端的证书（仅双向认证需要，单项认证输入null即可）.
     * @param clientPrivateKeyFile 客户端的私钥（仅双向认证需要，单项认证输入null即可）.
     *
     * @throws SSLException when ssl shark hand failed. throw this exception.
     */
    public void openChannelWithTwoWaySsl(File serverCaFile, String authority,
                                         File clientCaFile, File clientPrivateKeyFile)
        throws SSLException {
        SslContextBuilder sslContextBuilder = GrpcSslContexts.forClient();
        // sslContextBuilder.clientAuth(ClientAuth.REQUIRE);
        if (serverCaFile != null) {
            sslContextBuilder.trustManager(serverCaFile);
        }
        if (clientCaFile != null && clientPrivateKeyFile != null) {
            sslContextBuilder.clientAuth(ClientAuth.REQUIRE);
            sslContextBuilder.keyManager(clientCaFile, clientPrivateKeyFile);
        }
        sslContextBuilder.sslProvider(SslProvider.OPENSSL)
            .ciphers(Http2SecurityUtil.CIPHERS, SupportedCipherSuiteFilter.INSTANCE)
            .applicationProtocolConfig(
                new ApplicationProtocolConfig(
                    ApplicationProtocolConfig.Protocol.ALPN,
                    ApplicationProtocolConfig.SelectorFailureBehavior.NO_ADVERTISE,
                    ApplicationProtocolConfig.SelectedListenerFailureBehavior.ACCEPT,
                    ApplicationProtocolNames.HTTP_2,
                    ApplicationProtocolNames.HTTP_1_1));
        openChannelWithSsl(authority, sslContextBuilder.build());
    }

    /**
     * 开启grpc-channel，通道将使用ssl-context对消息加解密.
     *
     * @param authority 授权机构（使用者）.
     * @param sslContext ssl Context.
     */
    public void openChannelWithSsl(String authority, SslContext sslContext) {
        NettyChannelBuilder channelBuilder = NettyChannelBuilder
            .forAddress(grpcServerHost, grpcServerPort).overrideAuthority(authority);
        channelBuilder.sslContext(sslContext);
        this.channel = channelBuilder.build();
        logger.info("grpc-client connect started with ssl-context.");
    }

    public ManagedChannel getChannel() {
        return channel;
    }

    public void shutdown() {
        try {
            channel.shutdown().awaitTermination(5, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            logger.error("InterruptedException:", e);
        }
    }
}
