package org.eclipse.microprofile.problemdetails;

public enum LogLevel {
    /**
     * <code>INFO</code> for <code>4xx</code> and <code>ERROR</code> for anything else.
     */
    AUTO,

    ERROR, WARN, INFO, DEBUG, OFF
}
