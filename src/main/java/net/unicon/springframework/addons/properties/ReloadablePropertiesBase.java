package net.unicon.springframework.addons.properties;

import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

/**
 * Useful base class for implementing {@link ReloadableProperties}.
 */
public class ReloadablePropertiesBase extends DelegatingProperties implements ReloadableProperties {
    private List<ReloadablePropertiesListener> listeners = new ArrayList<ReloadablePropertiesListener>();
    private Properties internalProperties;

    public void setListeners(final List listeners) {
        this.listeners = listeners;
    }

    protected Properties getDelegate() {
        synchronized (this) {
            return internalProperties;
        }
    }

    public Properties getProperties() {
        return getDelegate();
    }

    public void addReloadablePropertiesListener(final ReloadablePropertiesListener l) {
        listeners.add(l);
    }

    public boolean removeReloadablePropertiesListener(final ReloadablePropertiesListener l) {
        return listeners.remove(l);
    }

    protected void notifyPropertiesChanged(final Properties oldProperties) {
        final PropertiesReloadedEvent event = new PropertiesReloadedEvent(this, oldProperties);
        for (final ReloadablePropertiesListener listener : listeners) {
            listener.propertiesReloaded(event);
        }
    }

    protected void setProperties(final Properties properties) {
        final Properties oldProperties = internalProperties;
        synchronized (this) {
            internalProperties = properties;
        }
        notifyPropertiesChanged(oldProperties);
    }
}
