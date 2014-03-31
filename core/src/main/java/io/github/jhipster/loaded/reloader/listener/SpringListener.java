package io.github.jhipster.loaded.reloader.listener;

import org.springframework.context.ConfigurableApplicationContext;

/**
 * Listener used to interact with spring when classes are reloaded.
 *
 * For example, the JHipsterHandlerMappingListener will create
 * the mapping between a request and a method only for new controller
 */
public interface SpringListener {

    /**
     * Initialize the listener class
     * @param applicationContext the application context
     */
    void init(ConfigurableApplicationContext applicationContext);

    /**
     * Wheter the class is supported by this listener
     *
     * @param clazz class of the spring beans to process
     *
     * @return true if supported, false otherwise
     */
    boolean support(Class<?> clazz);

    /**
     * Call when a new or existing file has been reloaded
     *
     * @param clazz class of the spring beans to process
     * @param newClazz true if the class is a new class, false otherwise
     */
    void addBeansToProcess(Class<?> clazz, boolean newClazz);

    /**
     * Process the new or existing spring beans
     */
    void process();

}
