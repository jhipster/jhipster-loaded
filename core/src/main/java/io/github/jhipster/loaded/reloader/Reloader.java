package io.github.jhipster.loaded.reloader;

import io.github.jhipster.loaded.reloader.type.ReloaderType;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.annotation.Order;

import java.util.Collection;

/**
 * Interface will be used by all types of reloader.
 * A reloader is special class used to reload a spring bean (spring, spring data, spring rest controller etc...)
 *
 * @see io.github.jhipster.loaded.reloader.JacksonReloader
 */
@Order
public interface Reloader {

    /**
     * Initialize the reloader class
     * @param applicationContext the application context
     */
    void init(ConfigurableApplicationContext applicationContext);

    /**
     * Wheter the reloader type is supported by this reloader
     *
     * @param reloaderType type of the spring beans to reload
     *
     * @return true if supported, false otherwise
     */
    boolean supports(Class<? extends ReloaderType> reloaderType);

    /**
     * Reloader can keep beans to reload if it is not able to reload the class the fisrt time due
     * to missing dependencies for example.
     *
     * @return true if needed to reload, false otherwise
     */
    boolean hasBeansToReload();

    /**
     * Tell the reloader to be prepared to accept new classes to reload
     */
    void prepare();

    /**
     * Call when a spring classes have been compiled and
     * only this spring classes need to be reloaded
     *
     * @param classes the list of compiled classes to reload
     * @param reloaderType type of the spring beans to reload
     */
    void addBeansToReload(Collection<Class> classes, Class<? extends ReloaderType> reloaderType);

    /**
     * Tell the reloader to start loaded the list of classes that have been added previously
     * in calling the addBeans method.
     */
    void reload();
}
