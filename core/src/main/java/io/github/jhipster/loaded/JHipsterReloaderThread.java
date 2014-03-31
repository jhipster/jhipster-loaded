package io.github.jhipster.loaded;

import io.github.jhipster.loaded.reloader.Reloader;
import io.github.jhipster.loaded.reloader.type.*;
import org.apache.commons.lang.ClassUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Controller;
import org.springframework.stereotype.Repository;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.RestController;

import javax.persistence.Entity;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * This thread stores classes to reload, to reload them all in one batch.
 */
public class JHipsterReloaderThread implements Runnable {

    private static Logger log = LoggerFactory.getLogger(JHipsterReloaderThread.class);

    private static final Object lock = new Object();

    public static boolean isStarted;

    private static boolean hotReloadTriggered = false;

    private static boolean isWaitingForNewClasses = false;

    private String domainPackageName;
    private String dtoPackageName;

    /**
     * How long does the thread wait until running a new batch.
     */
    private static final int BATCH_DELAY = 250;

    /**
     * The list of reloaders called when a spring class has been compiled
     * and needs to be reloaded
     */
    private Collection<Reloader> reloaders;

    /**
     * Stores the Spring controllers reloaded in the batch.
     */
    private List<Class> controllers = new ArrayList<>();

    /**
     * Stores the Spring services reloaded in the batch.
     */
    private List<Class> services = new ArrayList<>();

    /**
     * Stores the Spring repositories reloaded in the batch.
     */
    private List<Class> repositories = new ArrayList<>();

    /**
     * Stores the Spring components reloaded in the batch.
     */
    private List<Class> components = new ArrayList<>();

    /**
     * Stores the JPA entities reloaded in the batch.
     */
    private List<Class> entities = new ArrayList<>();

    /**
     * Stores the DTOs reloaded in the batch.
     */
    private List<Class> dtos = new ArrayList<>();

    public JHipsterReloaderThread(ConfigurableApplicationContext applicationContext, Collection<Reloader> reloaders) {
        this.reloaders = reloaders;
        domainPackageName = applicationContext.getEnvironment().getProperty("hotReload.package.domain");
        dtoPackageName = applicationContext.getEnvironment().getProperty("hotReload.package.restdto");
        isStarted = true;
    }

    public void reloadEvent(String typename, Class<?> clazz) {
        synchronized (lock) {
            log.trace("Hot reloading - checking if this is a Spring bean: {}", typename);

            boolean startReloading = false;
            if (AnnotationUtils.findAnnotation(clazz, Repository.class) != null ||
                    ClassUtils.isAssignable(clazz, org.springframework.data.repository.Repository.class)) {
                log.trace("{} is a Spring Repository", typename);
                repositories.add(clazz);
                startReloading = true;
            } else if (AnnotationUtils.findAnnotation(clazz, Service.class) != null) {
                log.trace("{} is a Spring Service", typename);
                services.add(clazz);
                startReloading = true;
            } else if (AnnotationUtils.findAnnotation(clazz, Controller.class) != null ||
                    AnnotationUtils.findAnnotation(clazz, RestController.class) != null) {
                log.trace("{} is a Spring Controller", typename);
                controllers.add(clazz);
                startReloading = true;
            } else if (AnnotationUtils.findAnnotation(clazz, Component.class) != null) {
                log.trace("{} is a Spring Component", typename);
                components.add(clazz);
                startReloading = true;
            } else if (typename.startsWith(domainPackageName)) {
                log.trace("{} is in the JPA package, checking if it is an entity", typename);
                if (AnnotationUtils.findAnnotation(clazz, Entity.class) != null) {
                    log.trace("{} is a JPA Entity", typename);
                    entities.add(clazz);
                    startReloading = true;
                }
            } else if (typename.startsWith(dtoPackageName)) {
                log.debug("{}  is a REST DTO", typename);
                dtos.add(clazz);
                startReloading = true;
            }

            if (startReloading) {
                hotReloadTriggered = true;
                isWaitingForNewClasses = true;
            }
        }
    }

    public void run() {
        while (isStarted) {
            try {
                Thread.sleep(BATCH_DELAY);
                if (hotReloadTriggered) {
                    if (isWaitingForNewClasses) {
                        log.info("Batch reload has been triggered, waiting for new classes for {} ms", BATCH_DELAY);
                        isWaitingForNewClasses = false;
                    } else {
                        hotReloadTriggered = batchReload();
                    }
                } else {
                    log.trace("Waiting for batch reload");
                }
            } catch (InterruptedException e) {
                log.error("JHipsterReloaderThread was awaken", e);
            }
        }
    }

    private boolean batchReload() {
        boolean hasBeansToReload = false;
        synchronized (lock) {
            log.info("Batch reload in progress...");

            for (Reloader reloader : reloaders) {
                boolean reload = false;
                reloader.prepare();

                // reload entities
                if (reloader.supports(EntityReloaderType.class) && !entities.isEmpty()) {
                    reload = true;
                    addSpringBeans(reloader, EntityReloaderType.instance, entities);
                }
                // reload dtos
                if (reloader.supports(RestDtoReloaderType.class) && !dtos.isEmpty()) {
                    reload = true;
                    addSpringBeans(reloader, RestDtoReloaderType.instance, dtos);
                }
                // reload repositories
                if (reloader.supports(RepositoryReloaderType.class) && !repositories.isEmpty()) {
                    reload = true;
                    addSpringBeans(reloader, RepositoryReloaderType.instance, repositories);
                }
                // reload services
                if (reloader.supports(ServiceReloaderType.class) && !services.isEmpty()) {
                    reload = true;
                    addSpringBeans(reloader, ServiceReloaderType.instance, services);
                }
                // reload components
                if (reloader.supports(ComponentReloaderType.class) && !components.isEmpty()) {
                    reload = true;
                    addSpringBeans(reloader, ComponentReloaderType.instance, components);
                }
                // reload controllers
                if (reloader.supports(ControllerReloaderType.class) && !controllers.isEmpty()) {
                    reload = true;
                    addSpringBeans(reloader, ControllerReloaderType.instance, controllers);
                }

                // Reload the spring beans
                if (reload || reloader.hasBeansToReload()) {
                    reloader.reload();
                }

                if (reloader.hasBeansToReload()) {
                    hasBeansToReload = true;
                }
            }

            // clear all lists
            entities.clear();
            dtos.clear();
            repositories.clear();
            services.clear();
            components.clear();
            controllers.clear();
        }

        return hasBeansToReload;
    }

    private void addSpringBeans(Reloader reloader, ReloaderType type, Collection<Class> classes) {
        if (classes.size() > 0) {
            log.debug("There are {} Spring {} updated, adding them to be reloaded", classes.size(), type.getName());
            reloader.addBeansToReload(classes, type.getClass());
        }
    }

    /**
     * Register the thread and starts it.
     */
    public static void register(JHipsterReloaderThread jHipsterReloaderThread) {
        try {
            final Thread thread = new Thread(jHipsterReloaderThread);
            thread.setDaemon(true);
            thread.start();

            Runtime.getRuntime().addShutdownHook(new Thread() {
                public void run() {
                    JHipsterReloaderThread.isStarted = false;
                    try {
                        thread.join();
                    } catch (InterruptedException e) {
                        log.error("Failed during the JVM shutdown", e);
                    }
                }
            });
        } catch (Exception e) {
            log.error("Failed to start the reloader thread. Classes will not be reloaded correctly.", e);
        }
    }
}
