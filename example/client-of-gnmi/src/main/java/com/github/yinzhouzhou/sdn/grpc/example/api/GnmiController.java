

package com.github.yinzhouzhou.sdn.grpc.example.api;

import com.github.yinzhouzhou.sdn.grpc.client.api.GrpcClientController;
import com.google.common.annotations.Beta;

/**
 * Controller of gNMI devices.
 */
@Beta
public interface GnmiController extends GrpcClientController<GnmiClient> {
}
