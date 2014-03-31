package io.github.jhipster.loaded.reloader.listener;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.Ordered;
import org.springframework.util.ClassUtils;
import org.springframework.util.ReflectionUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.method.HandlerMethodSelector;
import org.springframework.web.servlet.handler.AbstractHandlerMethodMapping;
import org.springframework.web.servlet.mvc.method.RequestMappingInfo;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.*;

/**
 * This Handler mapping is used to map only new Controller classes.
 * The existing controllers are mapped by the default RequestMappingHandlerMapping class.
 *
 * Each time, a controller is compiled this handler is called and the new controllers will be re-mapped
 */
public class JHipsterHandlerMappingListener extends RequestMappingHandlerMapping implements SpringListener, Ordered {

    private final Logger log = LoggerFactory.getLogger(JHipsterHandlerMappingListener.class);

    private List<Class> newControllers = new ArrayList<>();

    private ConfigurableApplicationContext applicationContext;

    @Override
    public void init(ConfigurableApplicationContext applicationContext) {
        this.applicationContext = applicationContext;

        // By default the static resource handler mapping is LOWEST_PRECEDENCE - 1
        super.setOrder(LOWEST_PRECEDENCE - 3);

        // Register the bean
        String beanName = StringUtils.uncapitalize(JHipsterHandlerMappingListener.class.getSimpleName());

        RootBeanDefinition bd = new RootBeanDefinition(JHipsterHandlerMappingListener.class);
        bd.setScope(BeanDefinition.SCOPE_SINGLETON);
        applicationContext.getBeanFactory().registerSingleton(beanName, this);
    }

    @Override
    public boolean support(Class<?> clazz) {
        return super.isHandler(clazz);
    }

    @Override
    public void addBeansToProcess(Class<?> clazz, boolean newClass) {
        // Register only new classes - existing classes will be handled by the default RequestMappingHandlerMapping class
        if (newClass) {
            newControllers.add(ClassUtils.getUserClass(clazz));
        } else { // remove the class from the current list because now the class is managed by the default handler mapping
            newControllers.remove(ClassUtils.getUserClass(clazz));
        }
    }

    @Override
    public void process() {
        // Clear existing mapping to register new classes
        clearExistingMapping();

        // Re-map the methods
        for (Class<?> clazz : newControllers) {
            final Class<?> userType = clazz;

            Set<Method> methods = HandlerMethodSelector.selectMethods(userType, new ReflectionUtils.MethodFilter() {
                @Override
                public boolean matches(Method method) {
                    return getMappingForMethod(method, userType) != null;
                }
            });

            try {
                Object handler = applicationContext.getBean(clazz);
                for (Method method : methods) {
                    RequestMappingInfo mapping = getMappingForMethod(method, userType);
                    try {
                        registerHandlerMethod(handler, method, mapping);
                    } catch (Exception e) {
                        logger.trace("Failed to register the handler for the method '" + method.getName() + "'", e);
                    }
                }
            } catch (BeansException e) {
                log.debug("JHipster reload - Spring bean '{}' does not exist, " +
                        "wait until this class will be available.", clazz.getName());
            }
        }
    }

    /**
     * Clear the two maps used to map the urls and the methods.
     */
    private void clearExistingMapping() {
        try {
            final Field urlMapField = ReflectionUtils.findField(AbstractHandlerMethodMapping.class, "urlMap");
            urlMapField.setAccessible(true);
            Map urlMap = (Map) urlMapField.get(this);

            for (Object mapping : urlMap.keySet()) {
                if (logger.isInfoEnabled()) {
                    logger.info("Remove Mapped \"" + mapping + "\"");
                }
            }

            urlMap.clear();

            final Field handlerMethodsField = ReflectionUtils.findField(AbstractHandlerMethodMapping.class, "handlerMethods");
            handlerMethodsField.setAccessible(true);
            Map m = (Map) handlerMethodsField.get(this);
            m.clear();
        } catch (Exception e) {
            log.error("Failed to clean the urlMap and the handlerMethods objects", e);
        }
    }
}
