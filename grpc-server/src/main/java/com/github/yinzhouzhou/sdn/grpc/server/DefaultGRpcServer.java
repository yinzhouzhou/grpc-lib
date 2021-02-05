package com.github.yinzhouzhou.sdn.grpc.server;

import io.grpc.BindableService;
import io.grpc.Server;
import io.grpc.ServerInterceptor;
import io.grpc.ServerServiceDefinition;
import io.grpc.netty.shaded.io.grpc.netty.NettyServerBuilder;
import io.grpc.netty.shaded.io.netty.handler.ssl.ApplicationProtocolConfig;
import io.grpc.netty.shaded.io.netty.handler.ssl.ClientAuth;
import io.grpc.netty.shaded.io.netty.handler.ssl.SslContext;
import io.grpc.netty.shaded.io.netty.handler.ssl.SslContextBuilder;
import java.io.File;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import javax.net.ssl.SSLException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class DefaultGRpcServer {
    private final Logger logger = LoggerFactory.getLogger(DefaultGRpcServer.class);

    private Server server;
    private NettyServerBuilder serverBuilder;

    private final String host;
    private final Integer bindPort;

    private int threadPoolSize = Runtime.getRuntime().availableProcessors() * 4;
    private int threadPoolQueueSize = 10000;

    public DefaultGRpcServer(String host, Integer bindPort) {
        this.host = host;
        this.bindPort = bindPort;
    }

    public DefaultGRpcServer(Integer bindPort) {
        this.host = "0.0.0.0";
        this.bindPort = bindPort;
    }

    public void initialize() {
        serverBuilder.keepAliveTime(3000, TimeUnit.MILLISECONDS);
//            .withOption(ChannelOption.TCP_NODELAY, true);
//            .executor(executor);
        logger.info("Server started, host {} listening on {}", host, bindPort);
    }

    /**
     * 创建一个grpc-server实例，注册消息的处理类，消息为纯文本格式.
     *
     * @param grpcServiceBeanMap GrpcMessageHandlers
     */
    public void buildGRPCServerUsePlainText(Map<String, GrpcMessageHandler> grpcServiceBeanMap,
                                            List<ServerInterceptor> interceptors) {
        buildGRpcServer(null, grpcServiceBeanMap, interceptors);
    }

    // 单向认证
    public void buildGRpcServer(String certChainFilePath, String privateKeyFilePath,
                                Map<String, GrpcMessageHandler> grpcServiceBeanMap,
                                List<ServerInterceptor> interceptors) throws SSLException {
        buildGRpcServer(buildSslContextForServer(
            certChainFilePath, privateKeyFilePath, null, false), grpcServiceBeanMap, interceptors);
    }

    // 双向认证
    public void buildGRpcServer(String certChainFilePath, String privateKeyFilePath,
                                String clientCaFilePath,
                                Map<String, GrpcMessageHandler> grpcServiceBeanMap,
                                List<ServerInterceptor> interceptors) throws SSLException {
        buildGRpcServer(buildSslContextForServer(
            certChainFilePath, privateKeyFilePath, clientCaFilePath, true), grpcServiceBeanMap, interceptors);
    }

    /**
     * 创建一个grpc-server实例，注册消息的处理类，拦截器，gRPC-message使用ssl进行加解密.
     *
     * @param sslContext SslContext
     * @param grpcServiceBeanMap GrpcMessageHandlers
     */
    public void buildGRpcServer(SslContext sslContext,
                                Map<String, GrpcMessageHandler> grpcServiceBeanMap,
                                List<ServerInterceptor> interceptors) {
        NettyServerBuilder nettyServerBuilder = NettyServerBuilder
            .forAddress(new InetSocketAddress(host, bindPort));
        if (sslContext != null) {
            nettyServerBuilder.sslContext(sslContext);
        }
        for (GrpcMessageHandler handler : grpcServiceBeanMap.values()) {
            nettyServerBuilder.addService(handler);
            logger.info("{} is registered.", handler.getClass().getSimpleName());
        }
        for (ServerInterceptor serverInterceptor : interceptors) {
            nettyServerBuilder.intercept(serverInterceptor);
        }
        this.serverBuilder = nettyServerBuilder;
    }

    /**
     * 为grpc-server生成一个ssl-context.
     *
     * @param certChainFilePath 服务端的证书.
     * @param privateKeyFilePath 服务端的私钥.
     * @param caFilePath 客户端的证书.
     * @param clientAuthRequire 是否强制双向认证.
     *
     * @return SslContext
     * @throws SSLException SSLException
     */
    private SslContext buildSslContextForServer(String certChainFilePath, String privateKeyFilePath,
                                                String caFilePath, boolean clientAuthRequire)
        throws SSLException {
        // 配置服务端的证书和私钥（必需）
        SslContextBuilder sslClientContextBuilder = SslContextBuilder.forServer(
            new File(certChainFilePath),
            new File(privateKeyFilePath));
        // 配置信任的证书（双向认证需要信任客户端的证书）
        if (caFilePath != null) {
            sslClientContextBuilder.trustManager(new File(caFilePath));
        }
        if (clientAuthRequire) {
            sslClientContextBuilder.clientAuth(ClientAuth.REQUIRE);
        }
        ApplicationProtocolConfig config = new ApplicationProtocolConfig(ApplicationProtocolConfig.Protocol.ALPN,
            ApplicationProtocolConfig.SelectorFailureBehavior.CHOOSE_MY_LAST_PROTOCOL,
            ApplicationProtocolConfig.SelectedListenerFailureBehavior.ACCEPT, "h2");
        sslClientContextBuilder.applicationProtocolConfig(config);
        return sslClientContextBuilder.build();
    }

    public void addMessageHandler(GrpcMessageHandler messageHandler) {
        logger.info("Bind handler {} into gRPC server {}", messageHandler.getClass().getSimpleName(), bindPort);
        addBindableService(messageHandler);
    }

    public void addBindableService(BindableService bean) {
        serverBuilder.addService(bean);
    }

    public void addHandler(ServerServiceDefinition definition) {
        logger.info("Bind handler:[{}] into gRPC server {}:{}",
            definition.getClass().getSimpleName(), host, bindPort);
        serverBuilder.addService(definition);
    }

    public void start() {
        try {
            this.server = serverBuilder.build();
            this.server.start();
            logger.info("grpc server is started on host {} and attach port {}", host, bindPort);
        } catch (IOException e) {
            logger.error("start IOException.", e);
        }
    }

    public void awaitTermination() {
        try {
            server.awaitTermination();
        } catch (InterruptedException e) {
            logger.error("awaitTermination InterruptedException.", e);
        }
    }

    public void stopGrpcServer() {
        if (server != null) {
            logger.info("close grpc server, close port {} listen", bindPort);
            server.shutdownNow();
            server = null;
        }
    }
}
