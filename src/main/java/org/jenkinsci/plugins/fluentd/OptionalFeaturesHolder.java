package org.jenkinsci.plugins.fluentd;

/**
 * This class holds the optional feature flags.
 * It's needed because if dependency is marked as optional, you can not access that class outside.
 */
public class OptionalFeaturesHolder {
    private static boolean isGerritTriggerPluginListenerEnabled = false;

    static void setIsGerritTriggerPluginListenerEnabled(boolean enabled) {
        isGerritTriggerPluginListenerEnabled = enabled;
    }

    static boolean isGerritTriggerPluginListenerEnabled() {
        return isGerritTriggerPluginListenerEnabled;
    }
}
