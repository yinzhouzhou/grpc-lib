

package com.github.yinzhouzhou.sdn.grpc.example.api;

import com.github.yinzhouzhou.sdn.grpc.client.api.device.DeviceId;
import com.github.yinzhouzhou.sdn.grpc.gnmi.protobuff.messages.Gnmi;
import com.google.common.base.MoreObjects;

/**
 * Event class for gNMI update.
 */
public class GnmiUpdate implements GnmiEventSubject {
    private DeviceId deviceId;
    private Gnmi.Notification update;
    private boolean syncResponse;

    /**
     * Default constructor.
     *
     * @param deviceId the device id for this event
     * @param update the update for this event
     * @param syncResponse indicate target has sent all values associated with
     *                     the subscription at least once.
     */
    public GnmiUpdate(DeviceId deviceId, Gnmi.Notification update, boolean syncResponse) {
        this.deviceId = deviceId;
        this.update = update;
        this.syncResponse = syncResponse;
    }

    /**
     * Gets the update data.
     *
     * @return the update data
     */
    public Gnmi.Notification update() {
        return update;
    }

    /**
     * indicate target has sent all values associated with the subscription at
     * least once.
     *
     * @return true if all value from target has sent
     */
    public boolean syncResponse() {
        return syncResponse;
    }

    @Override
    public DeviceId deviceId() {
        return deviceId;
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(getClass())
                .add("deviceId", deviceId)
                .add("syncResponse", syncResponse)
                .add("update", update)
                .toString();
    }
}
