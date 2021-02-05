package com.github.yinzhouzhou.sdn.grpc.server;

import io.grpc.BindableService;

public interface GrpcMessageHandler extends BindableService {
    String messageKey();
}
