package net.unicon.springframework.addons.properties;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.config.PropertiesFactoryBean;
import org.springframework.core.io.Resource;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * A properties factory bean that creates a reconfigurable Properties object.
 * When the Properties' reloadConfiguration method is called, and the file has
 * changed, the properties are read again from the file.
 */
public class ReloadablePropertiesFactoryBean extends PropertiesFactoryBean implements DisposableBean {

    private Logger log = LoggerFactory.getLogger(getClass());
    private Resource[] locations;
    private long[] lastModified;
    private List<ReloadablePropertiesListener> preListeners;

    public void setLocation(final Resource location) {
        setLocations(new Resource[]{location});
    }

    public void setLocations(final Resource[] locations) {
        this.locations = locations;
        lastModified = new long[locations.length];
        super.setLocations(locations);
    }

    protected Resource[] getLocations() {
        return locations;
    }

    public void setListeners(final List listeners) {
        // early type check, and avoid aliassing
        this.preListeners = new ArrayList<ReloadablePropertiesListener>();
        for (final Object o : listeners) {
            preListeners.add((ReloadablePropertiesListener) o);
        }
    }

    private ReloadablePropertiesBase reloadableProperties;

    protected Object createInstance() throws IOException {
        // would like to uninherit from AbstractFactoryBean (but it's final!)
        if (!isSingleton())
            throw new RuntimeException("ReloadablePropertiesFactoryBean only works as singleton");
        reloadableProperties = new ReloadablePropertiesImpl();
        if (preListeners != null)
            reloadableProperties.setListeners(preListeners);
        reload(true);
        return reloadableProperties;
    }

    public void destroy() throws Exception {
        reloadableProperties = null;
    }

    protected void reload(final boolean forceReload) throws IOException {
        boolean reload = forceReload;
        for (int i = 0; i < locations.length; i++) {
            final Resource location = locations[i];
            final File file;
            try {
                file = location.getFile();
            } catch (final IOException e) {
                // not a file resource
                continue;
            }
            try {
                final long l = file.lastModified();
                if (l > lastModified[i]) {
                    lastModified[i] = l;
                    reload = true;
                }
            } catch (final Exception e) {
                // cannot access file. assume unchanged.
                if (log.isDebugEnabled())
                    log.debug("can't determine modification time of " + file + " for " + location, e);
            }
        }
        if (reload)
            doReload();
    }

    private void doReload() throws IOException {
        reloadableProperties.setProperties(mergeProperties());
    }

    @Override
    public String toString() {
        return this.getClass().getSimpleName();
    }

    class ReloadablePropertiesImpl extends ReloadablePropertiesBase implements ReconfigurableBean {
        public void reloadConfiguration() throws Exception {
            ReloadablePropertiesFactoryBean.this.reload(false);
        }
    }

}
