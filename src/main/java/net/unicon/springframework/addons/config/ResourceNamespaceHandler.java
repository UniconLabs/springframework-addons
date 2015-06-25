package net.unicon.springframework.addons.config;

import net.unicon.springframework.addons.resource.ResourceChangeDetectingEventNotifier;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.xml.AbstractSingleBeanDefinitionParser;
import org.springframework.beans.factory.xml.NamespaceHandlerSupport;
import org.w3c.dom.Element;


/**
 *
 * {@link org.springframework.beans.factory.xml.NamespaceHandler} for convenient <i>resource</i> configuration namespace.
 * @author Dmitriy Kopylenko
 * @author Unicon, inc.
 * @since 0.1
 */
public class ResourceNamespaceHandler extends NamespaceHandlerSupport {

    @Override
    public void init() {
        registerBeanDefinitionParser("change-detector", new ResourceChangeDetectingEventNotifierBeanDefinitionParser());
    }

    /**
     * Parses <pre>change-detector</pre> elements into bean definitions of type {@link ResourceChangeDetectingEventNotifier}
     */
    private static class ResourceChangeDetectingEventNotifierBeanDefinitionParser extends AbstractSingleBeanDefinitionParser {

        @Override
        protected Class<?> getBeanClass(final Element element) {
            return ResourceChangeDetectingEventNotifier.class;
        }

        @Override
        protected void doParse(final Element element, final BeanDefinitionBuilder builder) {
            builder.addConstructorArgValue(element.getAttribute("watched-resource"));
        }
    }
}
