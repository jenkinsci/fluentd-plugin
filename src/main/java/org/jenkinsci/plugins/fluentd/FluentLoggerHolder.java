package org.jenkinsci.plugins.fluentd;

import org.fluentd.logger.FluentLogger;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.CheckForNull;

/**
 * Holds the reference to the current {@link FluentLogger}
 */
public final class FluentLoggerHolder {
    private static final Logger LOGGER = LoggerFactory.getLogger(FluentLogger.class);
    private static transient volatile FluentLogger fluentLogger;

    @CheckForNull
    public static FluentLogger getLogger() {
        return fluentLogger;
    }

    public static void initLogger(FluentLogger newLogger) {
        if (fluentLogger != null) {
            LOGGER.info("Reset fluent logger");
        }

        fluentLogger = newLogger;
    }

    public static void initLogger(String loggerName, String host, int port) {
        FluentLogger logger = FluentLogger.getLogger(loggerName, host, port);
        initLogger(logger);
    }
}
