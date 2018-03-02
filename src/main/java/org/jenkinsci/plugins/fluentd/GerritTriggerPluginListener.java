package org.jenkinsci.plugins.fluentd;


import com.sonyericsson.hudson.plugins.gerrit.trigger.extensions.GerritTriggeredBuildListener;
import com.sonymobile.tools.gerrit.gerritevents.dto.attr.Account;
import com.sonymobile.tools.gerrit.gerritevents.dto.attr.Change;
import com.sonymobile.tools.gerrit.gerritevents.dto.attr.PatchSet;
import com.sonymobile.tools.gerrit.gerritevents.dto.events.ChangeBasedEvent;
import com.sonymobile.tools.gerrit.gerritevents.dto.events.GerritTriggeredEvent;
import hudson.Extension;
import hudson.model.Result;
import org.fluentd.logger.FluentLogger;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

@Extension(optional = true)
public class GerritTriggerPluginListener extends GerritTriggeredBuildListener {
    private static final Logger LOGGER = LoggerFactory.getLogger(GerritTriggerPluginListener.class);
    private static final String GERRIT_TRIGGER_STARTED_TAG = "gerrit_trigger_plugin.on_started";
    private static final String GERRIT_TRIGGER_COMPLETED_TAG = "gerrit_trigger_plugin.on_completed";

    @Override
    public void onStarted(GerritTriggeredEvent gerritTriggeredEvent, String command) {
        if (!OptionalFeaturesHolder.isGerritTriggerPluginListenerEnabled()) {
            return;
        }

        try {
            Map<String, Object> data = new HashMap<>();
            data.put("event_type", gerritTriggeredEvent.getEventType().getTypeValue());
            data.put("command_size", command.length());

            if (gerritTriggeredEvent instanceof ChangeBasedEvent) {
                addChangeBasedInfo((ChangeBasedEvent) gerritTriggeredEvent, data);
            }

            logEvent(GERRIT_TRIGGER_STARTED_TAG, gerritTriggeredEvent, data);
        } catch (Exception exception) {
            LOGGER.warn("{} was not logged for event: {} due to exception during logging: {}",
                    GERRIT_TRIGGER_STARTED_TAG,
                    gerritTriggeredEvent,
                    exception.getMessage());
        }
    }

    @Override
    public void onCompleted(Result result, GerritTriggeredEvent gerritTriggeredEvent, String command) {
        if (!OptionalFeaturesHolder.isGerritTriggerPluginListenerEnabled()) {
            return;
        }

        try {
            Map<String, Object> data = new HashMap<>();
            data.put("event_type", gerritTriggeredEvent.getEventType().getTypeValue());
            data.put("command_size", command.length());
            data.put("result", result.toString());

            if (gerritTriggeredEvent instanceof ChangeBasedEvent) {
                addChangeBasedInfo((ChangeBasedEvent) gerritTriggeredEvent, data);
            }

            logEvent(GERRIT_TRIGGER_COMPLETED_TAG, gerritTriggeredEvent, data);
        } catch (Exception exception) {
            LOGGER.warn("{} was not logged for event: {} due to exception during logging: {}",
                    GERRIT_TRIGGER_COMPLETED_TAG,
                    gerritTriggeredEvent,
                    exception.getMessage());
        }
    }

    private void logEvent(String tag, GerritTriggeredEvent gerritTriggeredEvent, Map<String, Object> data) {
        FluentLogger logger = FluentLoggerHolder.getLogger();
        if (logger == null) {
            LOGGER.warn("Can't send data: fluentd logger is not initialized");
            return;
        }

        logger.log(tag, data);
        LOGGER.trace("{} is logged for event: {}", tag, gerritTriggeredEvent);
    }

    private void addChangeBasedInfo(ChangeBasedEvent changeBasedEvent, Map<String, Object> data) {
        Change change = changeBasedEvent.getChange();

        data.put("change_id", change.getId());
        data.put("branch", change.getBranch());
        data.put("project", change.getProject());
        data.put("change_number", change.getNumber());

        String topic = change.getTopic();
        if (topic != null) {
            data.put("topic", change.getTopic());
        } else {
            data.put("topic", "");
        }

        if (change.getCreatedOn() != null) {
            data.put("change_created_on", change.getCreatedOn().getTime());
        }

        if (changeBasedEvent.getEventCreatedOn() != null) {
            data.put("event_created_on", changeBasedEvent.getEventCreatedOn().getTime());
        }

        PatchSet patchSet = changeBasedEvent.getPatchSet();
        if (patchSet != null) {
            data.put("patchset_number", patchSet.getNumber());
            data.put("patchset_kind", patchSet.getKind().toString());

            if (patchSet.getCreatedOn() != null) {
                data.put("patchset_created_on", patchSet.getCreatedOn().getTime());
            }
        }

        Account changeOwner = change.getOwner();
        if (changeOwner != null) {
            data.put("owner_email", changeOwner.getEmail());
            data.put("owner_name", changeOwner.getName());
        }
    }
}
