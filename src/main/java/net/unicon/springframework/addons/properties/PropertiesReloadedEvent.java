package net.unicon.springframework.addons.properties;

import java.util.Properties;

public class PropertiesReloadedEvent {
    final ReloadableProperties target;
    final Properties oldProperties;

    public PropertiesReloadedEvent(final ReloadableProperties target, final Properties oldProperties) {
        this.target = target;
        this.oldProperties = oldProperties;
    }

    public ReloadableProperties getTarget() {
        return target;
    }

    public Properties getOldProperties() {
        return oldProperties;
    }
}
