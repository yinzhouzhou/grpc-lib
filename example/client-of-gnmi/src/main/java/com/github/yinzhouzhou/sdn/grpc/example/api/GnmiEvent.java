

package com.github.yinzhouzhou.sdn.grpc.example.api;

import com.google.common.annotations.Beta;

/**
 * Representation of an event received from a gNMI device.
 */
@Beta
public final class GnmiEvent {

    private Type type;
    private GnmiEventSubject subject;

    /**
     * Type of gNMI event.
     */
    public enum Type {
        /**
         * Update.
         */
        UPDATE
    }

    public GnmiEvent(Type type, GnmiEventSubject subject) {
        this.type = type;
        this.subject = subject;
    }

    public Type getType() {
        return type;
    }

    public GnmiEventSubject getSubject() {
        return subject;
    }
}
