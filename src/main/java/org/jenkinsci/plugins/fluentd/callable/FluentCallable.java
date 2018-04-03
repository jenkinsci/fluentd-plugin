package org.jenkinsci.plugins.fluentd.callable;

import hudson.FilePath;
import hudson.FilePath.FileCallable;
import hudson.remoting.VirtualChannel;
import org.fluentd.logger.FluentLogger;
import org.jenkinsci.plugins.fluentd.FluentLoggerHolder;
import org.jenkinsci.remoting.RoleChecker;

import java.io.File;
import java.io.IOException;
import java.util.Map;

import static org.jenkinsci.plugins.fluentd.FluentHelper.sendJson;

public class FluentCallable implements FileCallable<Boolean> {
    private final String tag;
    private final String json;
    private final long timestamp;
    private final Map<String, String> envVars;

    public FluentCallable(String loggerName, String host, int port, String tag, String json, long timestamp, Map<String, String> envVars) {
        this.tag = tag;
        this.json = json;
        this.timestamp = timestamp;
        this.envVars = envVars;

        FluentLoggerHolder.initLogger(loggerName, host, port);
    }

    @Override
    public Boolean invoke(File f, VirtualChannel channel) throws IOException, InterruptedException {
        return invoke(new FilePath(f));
    }

    public Boolean invoke(FilePath file) throws IOException, InterruptedException {
        if (!file.exists() || file.isDirectory()) {
            return false;
        }

        send(json, file.readToString());

        return true;
    }

    private void send(String json, String jsonFromFile) throws InterruptedException {
        FluentLogger logger = FluentLoggerHolder.getLogger();

        if (logger == null) {
            throw new IllegalStateException("Fluent Logger is not initialised");
        }

        sendJson(logger, tag, envVars, json, jsonFromFile, timestamp);
    }

    @Override
    public void checkRoles(RoleChecker roleChecker) throws SecurityException {
    }
}
