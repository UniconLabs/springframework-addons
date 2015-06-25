package net.unicon.springframework.addons.properties;

import org.springframework.beans.BeanWrapper;
import org.springframework.beans.BeanWrapperImpl;
import org.springframework.beans.BeansException;
import org.springframework.beans.MutablePropertyValues;
import org.springframework.beans.PropertyValue;
import org.springframework.beans.factory.BeanDefinitionStoreException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.BeanDefinitionVisitor;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.support.PropertySourcesPlaceholderConfigurer;
import org.springframework.util.ObjectUtils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

public class ReloadingPropertyPlaceholderConfigurer extends PropertySourcesPlaceholderConfigurer
        implements InitializingBean, DisposableBean, ReloadablePropertiesListener, ApplicationContextAware {
    /**
     * Default reloading placeholder prefix: "#{"
     */
    public static final String DEFAULT_RELOADING_PLACEHOLDER_PREFIX = "#{";

    /**
     * Default reloading placeholder suffix: "}"
     */
    public static final String DEFAULT_RELOADING_PLACEHOLDER_SUFFIX = "}";

    // -- un-protect:
    //    simulate the missing protected getter for some private superclass net.unicon.iamlabs.spring.properties.example.net.unicon.iamlabs.spring.properties we need to read here.

    private String placeholderPrefix = DEFAULT_PLACEHOLDER_PREFIX;

    private String placeholderSuffix = DEFAULT_PLACEHOLDER_SUFFIX;

    private String beanName;

    private BeanFactory beanFactory;
    private Properties[] propertiesArray;

    public void setProperties(final Properties properties) {
        setPropertiesArray(new Properties[]{properties});
    }

    public void setPropertiesArray(final Properties[] propertiesArray) {
        this.propertiesArray = propertiesArray;
        super.setPropertiesArray(propertiesArray);
    }

    public void setPlaceholderPrefix(final String placeholderPrefix) {
        this.placeholderPrefix = placeholderPrefix;
        super.setPlaceholderPrefix(placeholderPrefix);
    }

    public void setPlaceholderSuffix(final String placeholderSuffix) {
        this.placeholderSuffix = placeholderSuffix;
        super.setPlaceholderSuffix(placeholderPrefix);
    }

    public void setBeanName(final String beanName) {
        this.beanName = beanName;
        super.setBeanName(beanName);
    }

    public void setBeanFactory(final BeanFactory beanFactory) {
        this.beanFactory = beanFactory;
        super.setBeanFactory(beanFactory);
    }

    // --- end of un-protected

    /**
     * the application context is needed to find the beans again during reconfiguration
     */
    private ApplicationContext applicationContext;

    public void setApplicationContext(final ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
    }

    private String reloadingPlaceholderPrefix = ReloadingPropertyPlaceholderConfigurer.DEFAULT_RELOADING_PLACEHOLDER_PREFIX;

    private String reloadingPlaceholderSuffix = ReloadingPropertyPlaceholderConfigurer.DEFAULT_RELOADING_PLACEHOLDER_SUFFIX;

    public void setReloadingPlaceholderPrefix(final String reloadingPlaceholderPrefix) {
        this.reloadingPlaceholderPrefix = reloadingPlaceholderPrefix;
    }

    public void setReloadingPlaceholderSuffix(final String reloadingPlaceholderSuffix) {
        this.reloadingPlaceholderSuffix = reloadingPlaceholderSuffix;
    }

    protected String parseStringValue(final String strVal, final Properties props, final Set visitedPlaceholders)
            throws BeanDefinitionStoreException {

        DynamicProperty dynamic = null;

        // replace reloading prefix and suffix by "normal" prefix and suffix.
        // remember all the "dynamic" placeholders encountered.
        final StringBuffer buf = new StringBuffer(strVal);
        int startIndex = strVal.indexOf(this.reloadingPlaceholderPrefix);
        while (startIndex != -1) {
            final int endIndex = buf.toString().indexOf(
                    this.reloadingPlaceholderSuffix, startIndex + this.reloadingPlaceholderPrefix.length());
            if (endIndex != -1) {
                if (currentBeanName != null && currentPropertyName != null) {
                    String placeholder = buf.substring(startIndex + this.placeholderPrefix.length(), endIndex);
                    placeholder = getPlaceholder(placeholder);
                    if (dynamic == null)
                        dynamic = getDynamic(currentBeanName, currentPropertyName, strVal);
                    addDependency(dynamic, placeholder);
                } else {
                    logger.warn("dynamic property outside bean property value - ignored: " + strVal);
                }
                buf.replace(endIndex, endIndex + this.reloadingPlaceholderSuffix.length(), placeholderSuffix);
                buf.replace(startIndex, startIndex + this.reloadingPlaceholderPrefix.length(), placeholderPrefix);
                startIndex = endIndex - this.reloadingPlaceholderPrefix.length() + this.placeholderPrefix.length()
                        + this.placeholderSuffix.length();
                startIndex = strVal.indexOf(this.reloadingPlaceholderPrefix, startIndex);
            } else
                startIndex = -1;
        }
        // then, business as usual. no recursive reloading placeholders please.
        return super.parseStringValue(buf.toString(),
                props,
                visitedPlaceholders);
    }

    private DynamicProperty getDynamic(final String currentBeanName, final String currentPropertyName, final String orgStrVal) {
        final DynamicProperty dynamic = new DynamicProperty(currentBeanName, currentPropertyName, orgStrVal);
        final DynamicProperty found = (DynamicProperty) dynamicProperties.get(dynamic);
        if (found != null)
            return found;
        dynamicProperties.put(dynamic, dynamic);
        return dynamic;
    }

    private Properties lastMergedProperties;

    protected Properties mergeProperties() throws IOException {
        final Properties properties = super.mergeProperties();
        this.lastMergedProperties = properties;
        return properties;
    }

    public void propertiesReloaded(final PropertiesReloadedEvent event) {
        final Properties oldProperties = lastMergedProperties;
        try {
            final Properties newProperties = mergeProperties();

            // determine which relevant net.unicon.iamlabs.spring.properties.example.net.unicon.iamlabs.spring.properties have changed.
            final Set<String> placeholders = placeholderToDynamics.keySet();
            final Set<DynamicProperty> allDynamics = new HashSet<DynamicProperty>();
            for (final String placeholder : placeholders) {
                final String newValue = newProperties.getProperty(placeholder);
                final String oldValue = oldProperties.getProperty(placeholder);
                if (newValue != null && !newValue.equals(oldValue)
                        || newValue == null && oldValue != null) {
                    if (logger.isInfoEnabled())
                        logger.info("Property changed detected: " + placeholder + (newValue != null ? "=" + newValue : " removed"));
                    final List<DynamicProperty> affectedDynamics = placeholderToDynamics.get(placeholder);
                    allDynamics.addAll(affectedDynamics);
                }
            }
            // sort affected bean net.unicon.iamlabs.spring.properties.example.net.unicon.iamlabs.spring.properties by bean name and say hello.
            final Map<String, List<DynamicProperty>> dynamicsByBeanName = new HashMap<String, List<DynamicProperty>>();
            final Map<String, Object> beanByBeanName = new HashMap<String, Object>();
            for (final DynamicProperty dynamic : allDynamics) {
                final String beanName = dynamic.getBeanName();
                List<DynamicProperty> l = dynamicsByBeanName.get(beanName);
                if (l == null) {
                    dynamicsByBeanName.put(beanName, (l = new ArrayList<DynamicProperty>()));
                    Object bean = null;
                    try {
                        bean = applicationContext.getBean(beanName);
                        beanByBeanName.put(beanName, bean);
                    } catch (final BeansException e) {
                        // keep dynamicsByBeanName list, warn only once.
                        logger.error("Error obtaining bean " + beanName, e);
                    }
                    try {
                        if (bean instanceof ReconfigurationAware)
                            ((ReconfigurationAware) bean).beforeReconfiguration();  // hello!
                    } catch (final Exception e) {
                        logger.error("Error calling beforeReconfiguration on " + beanName, e);
                    }
                }
                l.add(dynamic);
            }
            // for all affected beans...
            final Collection<String> beanNames = dynamicsByBeanName.keySet();
            for (final String beanName : beanNames) {
                final Object bean = beanByBeanName.get(beanName);
                if (bean == null) // problems obtaining bean, earlier
                    continue;
                final BeanWrapper beanWrapper = new BeanWrapperImpl(bean);
                // for all affected net.unicon.iamlabs.spring.properties.example.net.unicon.iamlabs.spring.properties...
                final List<DynamicProperty> dynamics = dynamicsByBeanName.get(beanName);
                for (final DynamicProperty dynamic : dynamics) {
                    final String propertyName = dynamic.getPropertyName();
                    final String unparsedValue = dynamic.getUnparsedValue();

                    // obtain an updated value, including dependencies
                    String newValue;
                    removeDynamic(dynamic);
                    currentBeanName = beanName;
                    currentPropertyName = propertyName;
                    try {
                        newValue = parseStringValue(unparsedValue, newProperties, new HashSet());
                    } finally {
                        currentBeanName = null;
                        currentPropertyName = null;
                    }
                    if (logger.isInfoEnabled())
                        logger.info("Updating property " + beanName + "." + propertyName + " to " + newValue);

                    // assign it to the bean
                    try {
                        beanWrapper.setPropertyValue(propertyName, newValue);
                    } catch (final BeansException e) {
                        logger.error("Error setting property " + beanName + "." + propertyName + " to " + newValue, e);
                    }
                }
            }
            // say goodbye.
            for (final String beanName : beanNames) {
                final Object bean = beanByBeanName.get(beanName);
                try {
                    if (bean instanceof ReconfigurationAware)
                        ((ReconfigurationAware) bean).afterReconfiguration();
                } catch (final Exception e) {
                    logger.error("Error calling afterReconfiguration on " + beanName, e);
                }
            }
        } catch (final IOException e) {
            logger.error("Error trying to reload net.unicon.iamlabs.spring.properties.example.net.unicon.iamlabs.spring.properties: " + e.getMessage(), e);
        }
    }

    static class DynamicProperty {
        final String beanName;
        final String propertyName;
        final String unparsedValue;
        List<String> placeholders = new ArrayList<String>();

        public DynamicProperty(final String beanName, final String propertyName, final String unparsedValue) {
            this.beanName = beanName;
            this.propertyName = propertyName;
            this.unparsedValue = unparsedValue;
        }

        public void addPlaceholder(final String placeholder) {
            placeholders.add(placeholder);
        }

        public String getUnparsedValue() {
            return unparsedValue;
        }

        public String getBeanName() {
            return beanName;
        }

        public String getPropertyName() {
            return propertyName;
        }

        public boolean equals(final Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            final DynamicProperty that = (DynamicProperty) o;

            if (beanName != null ? !beanName.equals(that.beanName) : that.beanName != null) return false;
            if (propertyName != null ? !propertyName.equals(that.propertyName) : that.propertyName != null)
                return false;

            return true;
        }

        public int hashCode() {
            int result;
            result = (beanName != null ? beanName.hashCode() : 0);
            result = 29 * result + (propertyName != null ? propertyName.hashCode() : 0);
            return result;
        }
    }

    private Map<DynamicProperty, DynamicProperty> dynamicProperties = new HashMap<DynamicProperty, DynamicProperty>();
    private Map<String, List<DynamicProperty>> placeholderToDynamics = new HashMap<String, List<DynamicProperty>>();

    private void addDependency(final DynamicProperty dynamic, final String placeholder) {
        List<DynamicProperty> l = placeholderToDynamics.get(placeholder);
        if (l == null) {
            l = new ArrayList<DynamicProperty>();
            placeholderToDynamics.put(placeholder, l);
        }
        if (!l.contains(dynamic))
            l.add(dynamic);
        dynamic.addPlaceholder(placeholder);
    }

    private void removeDynamic(final DynamicProperty dynamic) {
        final List<String> placeholders = dynamic.placeholders;
        for (final String placeholder : placeholders) {
            final List<DynamicProperty> l = placeholderToDynamics.get(placeholder);
            l.remove(dynamic);
        }
        dynamic.placeholders.clear();
        dynamicProperties.remove(dynamic);
    }

    private String currentBeanName;
    private String currentPropertyName;

    /**
     * copy & paste, just so we can insert our own visitor.
     */
    protected void processProperties(final ConfigurableListableBeanFactory beanFactoryToProcess, final Properties props)
            throws BeansException {

        final BeanDefinitionVisitor visitor = new ReloadingPropertyPlaceholderConfigurer.PlaceholderResolvingBeanDefinitionVisitor(props);
        final String[] beanNames = beanFactoryToProcess.getBeanDefinitionNames();
        for (int i = 0; i < beanNames.length; i++) {
            // Check that we're not parsing our own bean definition,
            // to avoid failing on unresolvable placeholders in net.unicon.iamlabs.spring.properties.example.net.unicon.iamlabs.spring.properties file locations.
            if (!(beanNames[i].equals(this.beanName) && beanFactoryToProcess.equals(this.beanFactory))) {
                this.currentBeanName = beanNames[i];
                try {
                    final BeanDefinition bd = beanFactoryToProcess.getBeanDefinition(beanNames[i]);
                    try {
                        visitor.visitBeanDefinition(bd);
                    } catch (final BeanDefinitionStoreException ex) {
                        throw new BeanDefinitionStoreException(bd.getResourceDescription(), beanNames[i], ex.getMessage());
                    }
                } finally {
                    currentBeanName = null;
                }
            }
        }
    }

    public void afterPropertiesSet() {
        for (final Properties properties : propertiesArray) {
            if (properties instanceof ReloadableProperties) {
                ((ReloadableProperties) properties).addReloadablePropertiesListener(this);
            }
        }
    }

    public void destroy() throws Exception {
        for (final Properties properties : propertiesArray) {
            if (properties instanceof ReloadableProperties) {
                ((ReloadableProperties) properties).removeReloadablePropertiesListener(this);
            }
        }
    }

    private class PlaceholderResolvingBeanDefinitionVisitor extends BeanDefinitionVisitor {

        private final Properties props;

        public PlaceholderResolvingBeanDefinitionVisitor(final Properties props) {
            this.props = props;
        }

        protected void visitPropertyValues(final MutablePropertyValues pvs) {
            final PropertyValue[] pvArray = pvs.getPropertyValues();
            for (final PropertyValue pv : pvArray) {
                currentPropertyName = pv.getName();
                try {
                    final Object newVal = resolveValue(pv.getValue());
                    if (!ObjectUtils.nullSafeEquals(newVal, pv.getValue())) {
                        pvs.addPropertyValue(pv.getName(), newVal);
                    }
                } finally {
                    currentPropertyName = null;
                }
            }
        }

        protected String resolveStringValue(final String strVal) throws BeansException {
            return parseStringValue(strVal, this.props, new HashSet());
        }
    }
}
