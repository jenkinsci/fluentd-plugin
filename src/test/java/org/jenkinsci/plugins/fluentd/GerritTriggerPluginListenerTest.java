package org.jenkinsci.plugins.fluentd;

import com.sonymobile.tools.gerrit.gerritevents.dto.GerritChangeKind;
import com.sonymobile.tools.gerrit.gerritevents.dto.GerritEventType;
import com.sonymobile.tools.gerrit.gerritevents.dto.attr.Account;
import com.sonymobile.tools.gerrit.gerritevents.dto.attr.Change;
import com.sonymobile.tools.gerrit.gerritevents.dto.attr.PatchSet;
import com.sonymobile.tools.gerrit.gerritevents.dto.events.ChangeBasedEvent;
import com.sonymobile.tools.gerrit.gerritevents.dto.events.GerritTriggeredEvent;
import com.sonymobile.tools.gerrit.gerritevents.dto.events.PatchsetCreated;
import com.sonymobile.tools.gerrit.gerritevents.dto.events.RefUpdated;
import hudson.model.Result;
import org.fluentd.logger.FluentLogger;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import static org.mockito.Mockito.*;

public class GerritTriggerPluginListenerTest {
    private FluentLogger fluentLogger;
    private GerritTriggerPluginListener listener;
    private final String COMMAND = "custom command";

    @Before
    public void setUp() throws Exception {
        fluentLogger = Mockito.mock(FluentLogger.class);

        listener = new GerritTriggerPluginListener();
        OptionalFeaturesHolder.setIsGerritTriggerPluginListenerEnabled(false);

        FluentLoggerHolder.initLogger(fluentLogger);
    }

    @Test
    public void onStartedAndDisabled() throws Exception {
        GerritTriggeredEvent gerritTriggeredEvent = Mockito.mock(ChangeBasedEvent.class);

        listener.onStarted(gerritTriggeredEvent, COMMAND);

        verifyZeroInteractions(fluentLogger);
    }

    @Test
    public void onCompletedAndDisabled() throws Exception {
        GerritTriggeredEvent gerritTriggeredEvent = Mockito.mock(ChangeBasedEvent.class);

        listener.onCompleted(Result.ABORTED, gerritTriggeredEvent, COMMAND);

        verifyZeroInteractions(fluentLogger);
    }

    @Test
    public void onStartedEnabledLogNotChangedBased() throws Exception {
        OptionalFeaturesHolder.setIsGerritTriggerPluginListenerEnabled(true);
        GerritTriggeredEvent gerritTriggeredEvent = getNotChangedBasedEvent();

        listener.onStarted(gerritTriggeredEvent, COMMAND);

        Map<String, Object> expectedData = new HashMap<>();
        expectedData.put("event_type", "ref-updated");
        expectedData.put("command_size", 14);

        verify(fluentLogger, times(1)).log(eq("gerrit_trigger_plugin.on_started"), eq(expectedData));
    }

    @Test
    public void onCompletedEnabledLogNotChangedBased() throws Exception {
        OptionalFeaturesHolder.setIsGerritTriggerPluginListenerEnabled(true);
        GerritTriggeredEvent gerritTriggeredEvent = getNotChangedBasedEvent();

        listener.onCompleted(Result.ABORTED, gerritTriggeredEvent, COMMAND);

        Map<String, Object> expectedData = new HashMap<>();
        expectedData.put("result", "ABORTED");
        expectedData.put("event_type", "ref-updated");
        expectedData.put("command_size", 14);

        verify(fluentLogger, times(1)).log(eq("gerrit_trigger_plugin.on_completed"), eq(expectedData));
    }

    @Test
    public void onStartedEnabledLogChangedBased() throws Exception {
        OptionalFeaturesHolder.setIsGerritTriggerPluginListenerEnabled(true);
        GerritTriggeredEvent gerritTriggeredEvent = getChangeBasedEvent();

        listener.onStarted(gerritTriggeredEvent, COMMAND);

        Map<String, Object> expectedData = new HashMap<>();
        expectedData.put("event_type", "patchset-created");
        expectedData.put("command_size", 14);
        expectedData.put("owner_name", "name");
        expectedData.put("owner_email", "email");
        expectedData.put("change_id", "change");
        expectedData.put("project", "project_name");
        expectedData.put("topic", "topic_name");
        expectedData.put("branch", "branch_name");
        expectedData.put("patchset_number", "number");
        expectedData.put("patchset_kind", "TRIVIAL_REBASE");
        expectedData.put("change_created_on", 5L);
        expectedData.put("patchset_created_on", 10L);
        expectedData.put("event_created_on", 15L);
        expectedData.put("change_number", "777");

        verify(fluentLogger, times(1)).log(eq("gerrit_trigger_plugin.on_started"), eq(expectedData));
    }

    @Test
    public void onCompletedEnabledLogChangedBased() throws Exception {
        OptionalFeaturesHolder.setIsGerritTriggerPluginListenerEnabled(true);
        GerritTriggeredEvent gerritTriggeredEvent = getChangeBasedEvent();

        listener.onCompleted(Result.ABORTED, gerritTriggeredEvent, COMMAND);

        Map<String, Object> expectedData = new HashMap<>();
        expectedData.put("result", "ABORTED");
        expectedData.put("event_type", "patchset-created");
        expectedData.put("command_size", 14);
        expectedData.put("owner_name", "name");
        expectedData.put("owner_email", "email");
        expectedData.put("change_id", "change");
        expectedData.put("project", "project_name");
        expectedData.put("topic", "topic_name");
        expectedData.put("branch", "branch_name");
        expectedData.put("patchset_number", "number");
        expectedData.put("patchset_kind", "TRIVIAL_REBASE");
        expectedData.put("change_created_on", 5L);
        expectedData.put("patchset_created_on", 10L);
        expectedData.put("event_created_on", 15L);
        expectedData.put("change_number", "777");

        verify(fluentLogger, times(1)).log(eq("gerrit_trigger_plugin.on_completed"), eq(expectedData));
    }

    @Test
    public void onCompletedEnabledLogChangedBasedWithoutTopic() throws Exception {
        OptionalFeaturesHolder.setIsGerritTriggerPluginListenerEnabled(true);
        PatchsetCreated gerritTriggeredEvent = getChangeBasedEvent();
        when(gerritTriggeredEvent.getChange().getTopic()).thenReturn(null);

        listener.onCompleted(Result.ABORTED, gerritTriggeredEvent, COMMAND);

        Map<String, Object> expectedData = new HashMap<>();
        expectedData.put("result", "ABORTED");
        expectedData.put("event_type", "patchset-created");
        expectedData.put("command_size", 14);
        expectedData.put("owner_name", "name");
        expectedData.put("owner_email", "email");
        expectedData.put("change_id", "change");
        expectedData.put("project", "project_name");
        expectedData.put("topic", "");
        expectedData.put("branch", "branch_name");
        expectedData.put("patchset_number", "number");
        expectedData.put("patchset_kind", "TRIVIAL_REBASE");
        expectedData.put("change_created_on", 5L);
        expectedData.put("patchset_created_on", 10L);
        expectedData.put("event_created_on", 15L);
        expectedData.put("change_number", "777");

        verify(fluentLogger, times(1)).log(eq("gerrit_trigger_plugin.on_completed"), eq(expectedData));
    }

    private GerritTriggeredEvent getNotChangedBasedEvent() {
        GerritTriggeredEvent gerritTriggeredEvent = Mockito.mock(RefUpdated.class);

        GerritEventType eventType = GerritEventType.REF_UPDATED;
        when(gerritTriggeredEvent.getEventType()).thenReturn(eventType);
        return gerritTriggeredEvent;
    }

    private PatchsetCreated getChangeBasedEvent() {
        PatchsetCreated patchsetCreated = Mockito.mock(PatchsetCreated.class);
        Change change = Mockito.mock(Change.class);
        Account account = Mockito.mock(Account.class);
        PatchSet patchSet = Mockito.mock(PatchSet.class);

        GerritEventType eventType = GerritEventType.PATCHSET_CREATED;
        when(patchsetCreated.getEventType()).thenReturn(eventType);
        when(patchsetCreated.getChange()).thenReturn(change);
        when(patchsetCreated.getEventCreatedOn()).thenReturn(new Date(15));
        when(patchsetCreated.getPatchSet()).thenReturn(patchSet);

        when(change.getOwner()).thenReturn(account);
        when(account.getName()).thenReturn("name");
        when(account.getEmail()).thenReturn("email");
        when(change.getCreatedOn()).thenReturn(new Date(5));
        when(change.getId()).thenReturn("change");
        when(change.getBranch()).thenReturn("branch_name");
        when(change.getProject()).thenReturn("project_name");
        when(change.getTopic()).thenReturn("topic_name");
        when(change.getNumber()).thenReturn("777");
        when(patchSet.getNumber()).thenReturn("number");
        when(patchSet.getKind()).thenReturn(GerritChangeKind.TRIVIAL_REBASE);
        when(patchSet.getCreatedOn()).thenReturn(new Date(10));

        return patchsetCreated;
    }
}

