
package com.github.yinzhouzhou.sdn.grpc.client.api.device;

/**
 * Describes and event related to a protocol agent used to interact with an
 * infrastructure device.
 */
public class DeviceAgentEvent {

    /**
     * Type of device events.
     */
    public enum Type {
        /**
         * Signifies that a channel between the agent and the device is open and
         * the two can communicate.
         */
        CHANNEL_OPEN,

        /**
         * Signifies that a channel between the agent and the device is closed
         * and the two cannot communicate.
         */
        CHANNEL_CLOSED,

        /**
         * Signifies that a channel error has been detected. Further
         * investigation should be performed to check if the channel is still
         * open or closed.
         */
        CHANNEL_ERROR,

        /**
         * Signifies that the agent has acquired master role.
         */
        ROLE_MASTER,

        /**
         * Signifies that the agent has acquired standby/slave mastership role.
         */
        ROLE_STANDBY,

        /**
         * Signifies that the agent doesn't have any valid mastership role for
         * the device.
         */
        ROLE_NONE,

        /**
         * Signifies that the agent tried to perform some operations on the
         * device that requires master role.
         */
        NOT_MASTER,

    }

    private Type type;

    private DeviceId deviceId;

    /**
     * Creates a new device agent event for the given type and device ID.
     *
     * @param type     event type
     * @param deviceId device ID
     */
    public DeviceAgentEvent(Type type, DeviceId deviceId) {
        this.type = type;
        this.deviceId = deviceId;
    }

    public Type getType() {
        return type;
    }

    public DeviceId getDeviceId() {
        return deviceId;
    }
}
