package io.github.jhipster.loaded.reloader.loader;

import org.springframework.beans.BeansException;
import org.springframework.context.ConfigurableApplicationContext;

/**
 *  A loader is used by SpringReloader to load a new spring bean.
 */
public interface SpringLoader {

    /**
     * Initialize the loader class
     * @param applicationContext the application context
     */
    void init(ConfigurableApplicationContext applicationContext);

    /**
     * Wheter the reloader class is supported by this loader
     *
     * @param clazz type of the spring bean to load
     *
     * @return true if supported, false otherwise
     */
    boolean supports(Class clazz);

    /**
     * Register the class as a spring bean
     * The bean definition has been already registered before calling the method
     *
     * @param clazz class of the new bean to load
     * @throws BeansException if the bean could not be registered
     */
    void registerBean(Class clazz) throws BeansException;
}
