package io.github.jhipster.loaded.reloader.loader;

import io.github.jhipster.loaded.reloader.ReloaderUtils;
import org.apache.commons.lang.ClassUtils;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * Default loader used to load a new spring bean independent of the type
 *
 */
@Component
@Order(Ordered.LOWEST_PRECEDENCE)
public class DefaultSpringLoader implements SpringLoader {

    private DefaultListableBeanFactory beanFactory;

    @Override
    public void init(ConfigurableApplicationContext applicationContext) {
        beanFactory = (DefaultListableBeanFactory) applicationContext.getBeanFactory();
   }

    @Override
    public boolean supports(Class clazz) {
        return !ClassUtils.isAssignable(clazz, org.springframework.data.repository.Repository.class);
    }

    @Override
    public void registerBean(Class clazz) throws BeansException {
        String beanName = ReloaderUtils.constructBeanName(clazz);
        beanFactory.getBean(beanName);
    }
}
