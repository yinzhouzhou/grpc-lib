

package com.github.yinzhouzhou.sdn.grpc.client.ctl;

import static java.lang.String.format;
import static org.slf4j.LoggerFactory.getLogger;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.grpc.CallOptions;
import io.grpc.Channel;
import io.grpc.ClientCall;
import io.grpc.ClientInterceptor;
import io.grpc.ForwardingClientCall;
import io.grpc.ForwardingClientCallListener;
import io.grpc.Metadata;
import io.grpc.MethodDescriptor;
import io.grpc.Status;
import io.grpc.protobuf.lite.ProtoLiteUtils;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.net.URI;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.StringJoiner;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import org.slf4j.Logger;

/**
 * gRPC client interceptor that logs to file all messages sent and received.
 */
@SuppressFBWarnings
final class GrpcLoggingInterceptor implements ClientInterceptor {

    private static final Metadata.Key<com.google.rpc.Status> GRPC_STATUS_KEY =
        Metadata.Key.of(
            "grpc-status-details-bin",
            ProtoLiteUtils.metadataMarshaller(
                com.google.rpc.Status.getDefaultInstance()));

    private static final Logger LOG = getLogger(GrpcLoggingInterceptor.class);

    private final AtomicLong callIdGenerator = new AtomicLong();
    private final URI channelUri;
    private final AtomicBoolean enabled;

    private FileWriter writer;

    GrpcLoggingInterceptor(URI channelUri, AtomicBoolean enabled) {
        this.channelUri = channelUri;
        this.enabled = enabled;
    }

    private boolean initWriter() {
        if (writer != null) {
            return true;
        }
        final String safeChName = channelUri.toString()
            .replaceAll("[^A-Za-z0-9]", "_").toLowerCase(Locale.ENGLISH);
        try {
            final File tmpFile = File.createTempFile(safeChName + "_", ".log");
            this.writer = new FileWriter(tmpFile);
            LOG.info("Created gRPC call log file for channel {}: {}",
                channelUri, tmpFile.getAbsolutePath());
            return true;
        } catch (IOException e) {
            LOG.error("Unable to initialize gRPC call log writer", e);
        }
        return false;
    }

    void close() {
        synchronized (this) {
            if (writer == null) {
                return;
            }
            try {
                LOG.info("Closing log writer for {}...", channelUri);
                writer.close();
            } catch (IOException e) {
                LOG.error("Unable to close gRPC call log writer", e);
            }
            writer = null;
        }
    }

    private void write(String message) {
        synchronized (this) {
            if (!initWriter()) {
                return;
            }
            if (message.length() > 4096) {
                message = message.substring(0, 256) + "... TRUNCATED!\n\n";
            }
            try {
                writer.write(format(
                    "*** %s - %s",
                    new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.S")
                        .format(new Date()),
                    message));
            } catch (IOException e) {
                LOG.error("Unable to write gRPC call log", e);
            }
        }
    }

    @Override
    public <ReqT, RespT> ClientCall<ReqT, RespT> interceptCall(
        MethodDescriptor<ReqT, RespT> methodDescriptor,
        CallOptions callOptions, Channel channel) {
        return new ForwardingClientCall.SimpleForwardingClientCall<ReqT, RespT>(
            channel.newCall(methodDescriptor, callOptions.withoutWaitForReady())) {

            private final long callId = callIdGenerator.getAndIncrement();

            @Override
            public void sendMessage(ReqT message) {
                if (enabled.get()) {
                    write(format(
                        "%s >> OUTBOUND >> [callId=%s]\n%s\n",
                        methodDescriptor.getFullMethodName(),
                        callId,
                        message.toString()));
                }
                super.sendMessage(message);
            }

            @Override
            public void start(Listener<RespT> responseListener, Metadata headers) {

                if (enabled.get()) {
                    write(format(
                        "%s STARTED [callId=%s]\n%s\n\n",
                        methodDescriptor.getFullMethodName(),
                        callId,
                        headers.toString()));
                }

                Listener<RespT> listener = new ForwardingClientCallListener<RespT>() {
                    @Override
                    protected Listener<RespT> delegate() {
                        return responseListener;
                    }

                    @Override
                    public void onMessage(RespT message) {
                        if (enabled.get()) {
                            write(format(
                                "%s << INBOUND << [callId=%s]\n%s\n",
                                methodDescriptor.getFullMethodName(),
                                callId,
                                message.toString()));
                        }
                        super.onMessage(message);
                    }

                    @Override
                    public void onClose(Status status, Metadata trailers) {
                        if (enabled.get()) {
                            write(format(
                                "%s CLOSED [callId=%s]\n%s\n%s\n\n",
                                methodDescriptor.getFullMethodName(),
                                callId,
                                status.toString(),
                                parseTrailers(trailers)));
                        }
                        super.onClose(status, trailers);
                    }

                    private String parseTrailers(Metadata trailers) {
                        StringJoiner joiner = new StringJoiner("\n");
                        joiner.add(trailers.toString());
                        // If Google's RPC Status trailers are found, parse them.
                        final Iterable<com.google.rpc.Status> statuses = trailers.getAll(
                            GRPC_STATUS_KEY);
                        if (statuses == null) {
                            return joiner.toString();
                        }
                        statuses.forEach(s -> joiner.add(s.toString()));
                        return joiner.toString();
                    }

                    @Override
                    public void onHeaders(Metadata headers) {
                        super.onHeaders(headers);
                    }
                };

                super.start(listener, headers);
            }
        };
    }
}
