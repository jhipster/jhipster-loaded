package com.jhipster.loaded;

import com.jhipster.loaded.condition.ConditionalOnSpringLoaded;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springsource.loaded.agent.SpringLoadedAgent;

@Configuration
@ConditionalOnSpringLoaded
public class JHipsterReloaderAutoConfiguration implements ApplicationContextAware {

    private final Logger log = LoggerFactory.getLogger(JHipsterReloaderAutoConfiguration.class);

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        try {
            Environment env = applicationContext.getEnvironment();
            if (env.getProperty("hotReload.enabled", Boolean.class, false)) {
                SpringLoadedAgent.getInstrumentation();
                log.info("Spring Loaded is running, registering hot reloading features");
                JHipsterPluginManagerReloadPlugin.register((ConfigurableApplicationContext) applicationContext,
                    JHipsterReloaderAutoConfiguration.class.getClassLoader());
            }
        } catch (UnsupportedOperationException uoe) {
            log.info("Spring Loaded is not running, hot reloading is not enabled");
        }
    }
}
