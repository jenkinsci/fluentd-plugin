package org.jenkinsci.plugins.fluentd;

import org.fluentd.logger.FluentLogger;

import java.util.logging.Logger;

/**
 * Holds the reference to the current {@link FluentLogger}
 */
final class FluentLoggerHolder {
    private static final Logger LOGGER = Logger.getLogger(FluentLogger.class.toString());
    private static transient volatile FluentLogger fluentLogger;

    static FluentLogger getLogger() {
        return fluentLogger;
    }

    static void initLogger(FluentLogger newLogger) {
        if (fluentLogger != null) {
            LOGGER.info("Reset fluent logger");
        }

        fluentLogger = newLogger;
    }
}
