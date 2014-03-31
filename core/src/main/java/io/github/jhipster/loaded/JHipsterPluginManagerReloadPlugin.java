package io.github.jhipster.loaded;

import io.github.jhipster.loaded.reloader.Reloader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ConfigurableApplicationContext;
import org.springsource.loaded.Plugins;
import org.springsource.loaded.ReloadEventProcessorPlugin;

import java.util.Collection;

/**
 * Automatically re-configures classes when Spring Loaded triggers a hot reload event.
 *
 * <p>
 *     Supported technologies are
 *     <ul>
 *         <li>Spring: dependency injection and the post-construct hook are triggered</li>
 *         <li>Jackson: the serializer and deserializer caches are invalidated on JPA beans and DTOs</li>
 *     </ul>
 * </p>
 * <p>
 *   To have Spring Loaded working, run your Application class with these VM options: 
 *   "-javaagent:spring_loaded/springloaded-jhipster.jar -noverify "
 * </p>
 */
public class JHipsterPluginManagerReloadPlugin implements ReloadEventProcessorPlugin {

    private final Logger log = LoggerFactory.getLogger(JHipsterPluginManagerReloadPlugin.class);

    private static JHipsterReloaderThread jHipsterReloaderThread;

    private String projectPackageName;

    public JHipsterPluginManagerReloadPlugin(ConfigurableApplicationContext ctx) {
        projectPackageName = ctx.getEnvironment().getProperty("hotReload.package.project");
    }

    @Override
    public boolean shouldRerunStaticInitializer(String typename, Class<?> aClass, String encodedTimestamp) {
        return true;
    }

    public void reloadEvent(String typename, Class<?> clazz, String encodedTimestamp) {
        if (!typename.startsWith(projectPackageName)) {
            log.trace("This class is not in the application package, nothing to do");
            return;
        }
        if (typename.contains("$$EnhancerBy") || typename.contains("$$FastClassBy")) {
            log.trace("This is a CGLIB proxy, nothing to do");
            return;
        }
        jHipsterReloaderThread.reloadEvent(typename, clazz);
    }

    public static void register(ConfigurableApplicationContext ctx, Collection<Reloader> reloaders, ClassLoader classLoader) {
        jHipsterReloaderThread = new JHipsterReloaderThread(ctx, reloaders);
        JHipsterReloaderThread.register(jHipsterReloaderThread);
        JHipsterFileSystemWatcher.register(classLoader, ctx);
        Plugins.registerGlobalPlugin(new JHipsterPluginManagerReloadPlugin(ctx));
    }
}
