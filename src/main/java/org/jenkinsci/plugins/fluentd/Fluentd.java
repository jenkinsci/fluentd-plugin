package org.jenkinsci.plugins.fluentd;

import hudson.Extension;
import hudson.FilePath;
import hudson.Launcher;
import hudson.model.AbstractProject;
import hudson.model.Result;
import hudson.model.Run;
import hudson.model.TaskListener;
import hudson.tasks.*;
import jenkins.tasks.SimpleBuildStep;
import net.sf.json.JSONObject;
import org.apache.commons.lang.StringUtils;
import org.fluentd.logger.FluentLogger;
import org.jenkinsci.plugins.fluentd.callable.FluentCallable;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.StaplerRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import static org.jenkinsci.plugins.fluentd.FluentHelper.sendJson;

/**
 *
 * {@link Fluentd} build step publishes json data to <a href="https://www.fluentd.org">FluentD</a> agent.
 *
 * @author Alexander Akbashev
 */
@SuppressWarnings("WeakerAccess")
public class Fluentd extends Recorder implements SimpleBuildStep {
    private static final Logger LOGGER = LoggerFactory.getLogger(Fluentd.class);
    public static final String DEFAULT_LOGGER = "Jenkins";
    public static final String DEFAULT_HOST = "localhost";
    public static final int DEFAULT_PORT = 24224;

    private final String tag;
    private final String json;
    private final String fileName;
    private final boolean failBuild;
    private final boolean pushFromAgent;

    @DataBoundConstructor
    public Fluentd(String tag, boolean failBuild, String fileName, String json, boolean pushFromAgent) {
        this.tag = tag;
        this.failBuild = failBuild;
        this.fileName = fileName;
        this.json = json;
        this.pushFromAgent = pushFromAgent;
    }

    @SuppressWarnings("unused")
    public String getTag() {
        return tag;
    }

    @SuppressWarnings("unused")
    public String getFileName() {
        return fileName;
    }

    @SuppressWarnings("unused")
    public String getJson() {
        return json;
    }

    @SuppressWarnings("unused")
    public boolean isFailBuild() {
        return failBuild;
    }

    @SuppressWarnings("unused")
    public boolean isPushFromAgent() {
        return pushFromAgent;
    }

    @Override
    public void perform(@Nonnull Run<?, ?> build, @Nonnull FilePath workspace, @Nonnull Launcher launcher, @Nonnull TaskListener listener) throws IOException, InterruptedException {
        boolean succeed = true;

        Map<String, String> envVars = getEnvVariables(build, listener);

        LOGGER.debug("Tag {}, json {}, filename {}", tag, json, fileName);

        try {
            if (StringUtils.isNotEmpty(fileName)) {
                DescriptorImpl descriptor = getDescriptor();
                String loggerName = descriptor.getLoggerName();
                String host = getDescriptor().getHost();
                int port = getDescriptor().getPort();

                succeed = invoke(workspace,
                        new FluentCallable(loggerName, host, port, tag, json, build.getStartTimeInMillis(), envVars));
            } else {
                sendJson(FluentLoggerHolder.getLogger(), tag, envVars, StringUtils.EMPTY, json, build.getStartTimeInMillis());
            }
        } catch (IllegalArgumentException | IOException exception) {
            listener.error("Can't send data: %s", exception.getMessage());
            LOGGER.error("Exception happened during sending json {}", exception);
            succeed = false;
        }

        if (!succeed && failBuild) {
            build.setResult(Result.FAILURE);
        }
    }

    private boolean invoke(FilePath workspace, FluentCallable callable) throws InterruptedException, IOException {
        final FilePath filePath = new FilePath(workspace, fileName);
        if (pushFromAgent) {
            LOGGER.debug("Push {} with tag {} from agent", fileName, tag);
            return filePath.act(callable);
        } else {
            LOGGER.debug("Push {} with tag {} from master", fileName, tag);
            return callable.invoke(filePath);
        }
    }

    private Map<String, String> getEnvVariables(@Nonnull Run<?, ?> build, @Nonnull TaskListener listener) throws InterruptedException {
        Map<String, String> envVars;
        try {
            envVars = build.getEnvironment(listener);
        } catch (IOException e) {
            listener.error("Failed to get environment variables");
            e.printStackTrace(listener.getLogger());
            envVars = new HashMap<>();
        }
        return envVars;
    }


    @Override
    public DescriptorImpl getDescriptor() {
        return (DescriptorImpl) super.getDescriptor();
    }

    @Override
    public BuildStepMonitor getRequiredMonitorService() {
        return BuildStepMonitor.NONE;
    }

    /**
     * Descriptor for {@link Fluentd}. Used as a singleton.
     * The class is marked as public so that it can be accessed from views.
     * <p>
     * See <tt>src/main/resources/hudson/plugins/hello_world/Fluentd/*.jelly</tt>
     * for the actual HTML fragment for the configuration screen.
     */
    @Extension
    public static final class DescriptorImpl extends BuildStepDescriptor<Publisher> {
        /**
         * To persist global configuration information,
         * simply store it in a field and call save().
         * <p>
         * <p>
         * If you don't want fields to be persisted, use <tt>transient</tt>.
         */
        private String loggerName;
        private String host;
        private int port;
        private boolean enableGerritTriggeredBuildListener;

        /**
         * In order to load the persisted global configuration, you have to
         * call load() in the constructor.
         */
        public DescriptorImpl() {
            load();
            if (port == 0) port = DEFAULT_PORT;
            if (host == null || host.isEmpty()) host = DEFAULT_HOST;
            if (loggerName == null || loggerName.isEmpty()) loggerName = DEFAULT_LOGGER;

            OptionalFeaturesHolder.setIsGerritTriggerPluginListenerEnabled(enableGerritTriggeredBuildListener);
            FluentLoggerHolder.initLogger(FluentLogger.getLogger(loggerName, host, port));
        }

        @Override
        public boolean isApplicable(Class<? extends AbstractProject> aClass) {
            // Indicates that this builder can be used with all kinds of project types 
            return true;
        }

        /**
         * This human readable name is used in the configuration screen.
         */
        @Override
        public String getDisplayName() {
            return "Send to Fluentd";
        }

        @Override
        public boolean configure(StaplerRequest req, JSONObject formData) throws FormException {
            loggerName = formData.optString("loggerName", DEFAULT_LOGGER);
            port = formData.optInt("port", DEFAULT_PORT);
            host = formData.optString("host", DEFAULT_HOST);
            enableGerritTriggeredBuildListener = formData.optBoolean("enableGerritTriggeredBuildListener");
            save();

            OptionalFeaturesHolder.setIsGerritTriggerPluginListenerEnabled(enableGerritTriggeredBuildListener);
            FluentLoggerHolder.initLogger(FluentLogger.getLogger(loggerName, host, port));
            return super.configure(req, formData);
        }

        @SuppressWarnings("unused")
        public String getLoggerName() {
            return loggerName;
        }

        @SuppressWarnings("unused")
        public int getPort() {
            return port;
        }

        @SuppressWarnings("unused")
        public String getHost() {
            return host;
        }

        @SuppressWarnings("unused")
        public boolean isEnableGerritTriggeredBuildListener() {
            return enableGerritTriggeredBuildListener;
        }
    }
}

