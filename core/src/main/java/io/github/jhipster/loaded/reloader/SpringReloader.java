package io.github.jhipster.loaded.reloader;

import io.github.jhipster.loaded.reloader.listener.JHipsterHandlerMappingListener;
import io.github.jhipster.loaded.reloader.listener.SpringListener;
import io.github.jhipster.loaded.reloader.loader.SpringLoader;
import io.github.jhipster.loaded.reloader.type.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.aop.Advisor;
import org.springframework.aop.framework.Advised;
import org.springframework.aop.framework.autoproxy.BeanFactoryAdvisorRetrievalHelper;
import org.springframework.aop.support.AopUtils;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.support.AbstractBeanDefinition;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.annotation.AnnotationAwareOrderComparator;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.util.ReflectionUtils;

import javax.inject.Inject;
import java.lang.reflect.Field;
import java.util.*;

/**
 * Reloads Spring Beans.
 */
@Component
@Order(100)
public class SpringReloader implements Reloader {

    private final Logger log = LoggerFactory.getLogger(SpringReloader.class);

    private ConfigurableApplicationContext applicationContext;
    private BeanFactoryAdvisorRetrievalHelper beanFactoryAdvisorRetrievalHelper;


    private final List<SpringListener> springListeners = new ArrayList<>();
    private final List<SpringLoader> springLoaders = new ArrayList<>();

    private Set<Class> toReloadBeans = new LinkedHashSet<>();
    private List<Class> newToWaitFromBeans = new ArrayList<>();
    private Map<String, Class> existingToWaitFromBeans = new HashMap<>();

    @Override
    public void init(ConfigurableApplicationContext applicationContext) {
        log.debug("Hot reloading Spring Beans enabled");
        this.applicationContext = applicationContext;
        this.beanFactoryAdvisorRetrievalHelper = new BeanFactoryAdvisorRetrievalHelper(applicationContext.getBeanFactory());

        this.applicationContext.getAutowireCapableBeanFactory().autowireBean(this);

        // register listeners
        registerListeners();

        // register loaders
        registerLoaders();
    }

    @Override
    public boolean supports(Class<? extends ReloaderType> reloaderType) {
        return reloaderType.equals(EntityReloaderType.class) || reloaderType.equals(RepositoryReloaderType.class)
                || reloaderType.equals(ServiceReloaderType.class) || reloaderType.equals(ComponentReloaderType.class)
                || reloaderType.equals(ControllerReloaderType.class);
    }

    @Override
    public void prepare() {}

    @Override
    public boolean hasBeansToReload() {
        return toReloadBeans.size() > 0 || newToWaitFromBeans.size() > 0;
    }

    @Override
    public void addBeansToReload(Collection<Class> classes, Class<? extends ReloaderType> reloaderType) {
        if (reloaderType.equals(EntityReloaderType.class)) {
            List<Class> newSpringBeans = new ArrayList<>();
            List<Class> existingSpringBeans = new ArrayList<>();

            newSpringBeans.addAll(newToWaitFromBeans);
            newToWaitFromBeans.clear();
            existingSpringBeans.addAll(existingToWaitFromBeans.values());
            existingToWaitFromBeans.clear();

            start(newSpringBeans, existingSpringBeans);
        } else {
            toReloadBeans.addAll(classes);
        }
    }

    @Override
    public void reload() {
        List<Class> newSpringBeans = new ArrayList<>();
        List<Class> existingSpringBeans = new ArrayList<>();

        newSpringBeans.addAll(newToWaitFromBeans);
        newToWaitFromBeans.clear();
        existingSpringBeans.addAll(existingToWaitFromBeans.values());
        existingToWaitFromBeans.clear();

        start(newSpringBeans, existingSpringBeans);
    }

    private void start(List<Class> newSpringBeans, List<Class> existingSpringBeans) {
        try {
            DefaultListableBeanFactory beanFactory = (DefaultListableBeanFactory) applicationContext.getBeanFactory();

            //1) Split between new/existing beans
            for (Class toReloadBean : toReloadBeans) {
                log.trace("Hot reloading Spring bean: {}", toReloadBean.getName());
                String beanName = ReloaderUtils.constructBeanName(toReloadBean);
                if (!beanFactory.containsBeanDefinition(beanName)) {
                    newSpringBeans.add(toReloadBean);
                    // Check if this new class is a dependent class.
                    // If so add this dependent class to the newSpringBeans list
                    if (newToWaitFromBeans.size() > 0) {
                        newSpringBeans.addAll(newToWaitFromBeans);
                        newToWaitFromBeans.clear();
                    }
                } else {
                    existingSpringBeans.add(toReloadBean);
                    if (existingToWaitFromBeans.containsKey(toReloadBean.getName())) {
                        existingSpringBeans.add(existingToWaitFromBeans.get(toReloadBean.getName()));
                        existingToWaitFromBeans.remove(toReloadBean.getName());
                    }
                }
            }

            //2) Declare new beans prior to instanciation for cross bean references
            for (Class clazz : newSpringBeans) {
                String beanName = ReloaderUtils.constructBeanName(clazz);
                String scope = ReloaderUtils.getScope(clazz);
                RootBeanDefinition bd = new RootBeanDefinition(clazz, AbstractBeanDefinition.AUTOWIRE_BY_TYPE, true);
                bd.setScope(scope);
                beanFactory.registerBeanDefinition(beanName, bd);
            }

            //3) Instanciate new beans
            for (Class clazz : newSpringBeans) {
                String beanName = ReloaderUtils.constructBeanName(clazz);
                try {
                    processLoader(clazz);
                    processListener(clazz, true);
                    toReloadBeans.remove(clazz);
                    log.info("JHipster reload - New Spring bean '{}' has been reloaded.", clazz);
                } catch (Exception e) {
                    log.trace("The Spring bean can't be loaded at this time. Keep it to reload it later", e);
                    // remove the registration bean to treat this class as new class
                    beanFactory.removeBeanDefinition(beanName);
                    newToWaitFromBeans.add(clazz);
                    toReloadBeans.remove(clazz);
                }
            }

            //4) Resolve dependencies for existing beans
            for (Class clazz : existingSpringBeans) {
                Object beanInstance = applicationContext.getBean(clazz);

                log.trace("Existing bean, autowiring fields");
                if (AopUtils.isCglibProxy(beanInstance)) {
                    log.trace("This is a CGLIB proxy, getting the real object");
                    addAdvisorIfNeeded(clazz, beanInstance);
                    final Advised advised = (Advised) beanInstance;
                    beanInstance = advised.getTargetSource().getTarget();
                } else if (AopUtils.isJdkDynamicProxy(beanInstance)) {
                    log.trace("This is a JDK proxy, getting the real object");
                    addAdvisorIfNeeded(clazz, beanInstance);
                    final Advised advised = (Advised) beanInstance;
                    beanInstance = advised.getTargetSource().getTarget();
                } else {
                    log.trace("This is a normal Java object");
                }
                boolean failedToUpdate = false;
                Field[] fields = beanInstance.getClass().getDeclaredFields();
                for (Field field : fields) {
                    if (AnnotationUtils.getAnnotation(field, Inject.class) != null ||
                            AnnotationUtils.getAnnotation(field, Autowired.class) != null) {
                        log.trace("@Inject/@Autowired annotation found on field {}", field.getName());
                        ReflectionUtils.makeAccessible(field);
                        if (ReflectionUtils.getField(field, beanInstance) != null) {
                            log.trace("Field is already injected, not doing anything");
                        } else {
                            log.trace("Field is null, injecting a Spring bean");
                            try {
                            Object beanToInject = applicationContext.getBean(field.getType());
                            ReflectionUtils.setField(field, beanInstance, beanToInject);
                            } catch (NoSuchBeanDefinitionException bsbde) {
                                log.debug("JHipster reload - Spring bean '{}' does not exist, " +
                                        "wait until this class will be available.", field.getType());
                                failedToUpdate = true;
                                existingToWaitFromBeans.put(field.getType().getName(), clazz);
                            }
                        }
                    }
                }
                toReloadBeans.remove(clazz);
                if (!failedToUpdate) {
                    processListener(clazz, false);
                }
                log.info("JHipster reload - Existing Spring bean '{}' has been reloaded.", clazz);
            }

            for (SpringListener springListener : springListeners) {
                springListener.process();
            }
        } catch (Exception e) {
            log.warn("Could not hot reload Spring bean!", e);
        }
    }

    /**
     * AOP uses advisor to intercept any annotations.
     */
    private void addAdvisorIfNeeded(Class clazz, Object beanInstance) {
        final Advised advised = (Advised) beanInstance;
        final List<Advisor> candidateAdvisors = this.beanFactoryAdvisorRetrievalHelper.findAdvisorBeans();

        final List<Advisor> advisorsThatCanApply = AopUtils.findAdvisorsThatCanApply(candidateAdvisors, clazz);

        for (Advisor advisor : advisorsThatCanApply) {
            // Add the advisor to the advised if it doesn't exist
            if (advised.indexOf(advisor) == -1) {
                advised.addAdvisor(advisor);
            }
        }
    }

    private void processListener(Class<?> clazz, boolean isNewClass) {
        for (SpringListener springListener : springListeners) {
            if (springListener.support(clazz)) {
                springListener.addBeansToProcess(clazz, isNewClass);
            }
        }
    }

    private void processLoader(Class clazz) {
        for (SpringLoader springLoader : springLoaders) {
            if (springLoader.supports(clazz)) {
                springLoader.registerBean(clazz);
            }
        }
    }

    private void registerListeners() {
        springListeners.add(new JHipsterHandlerMappingListener());

        for (SpringListener springListener : springListeners) {
            springListener.init(applicationContext);
        }
    }

    private void registerLoaders() {
        final Map<String, SpringLoader> beansOfType = applicationContext.getBeansOfType(SpringLoader.class);

        for (SpringLoader springLoader : beansOfType.values()) {
            springLoaders.add(springLoader);
            springLoader.init(applicationContext);
        }

        Collections.sort(springLoaders, new AnnotationAwareOrderComparator());
    }
}
