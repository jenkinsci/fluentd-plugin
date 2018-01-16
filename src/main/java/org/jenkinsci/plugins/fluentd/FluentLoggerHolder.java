package org.jenkinsci.plugins.fluentd;

import org.fluentd.logger.FluentLogger;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.CheckForNull;

/**
 * Holds the reference to the current {@link FluentLogger}
 */
final class FluentLoggerHolder {
    private static final Logger LOGGER = LoggerFactory.getLogger(FluentLogger.class);
    private static transient volatile FluentLogger fluentLogger;

    @CheckForNull
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
