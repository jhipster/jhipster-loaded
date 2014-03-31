package io.github.jhipster.loaded.reloader.loader;

import io.github.jhipster.loaded.reloader.ReloaderUtils;
import org.apache.commons.lang.ClassUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.ListableBeanFactory;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.data.jpa.repository.support.JpaRepositoryFactory;
import org.springframework.data.repository.core.support.RepositoryProxyPostProcessor;
import org.springframework.data.repository.util.TxUtils;
import org.springframework.stereotype.Component;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import java.lang.reflect.Constructor;

/**
 * Default loader used to load a new spring bean independent of the type
 *
 */
@Component
@Order(Ordered.LOWEST_PRECEDENCE - 1)
public class JpaSpringLoader implements SpringLoader {

    private final Logger log = LoggerFactory.getLogger(JpaSpringLoader.class);

    private DefaultListableBeanFactory beanFactory;
    private JpaRepositoryFactory jpaRepositoryFactory;

    @PersistenceContext
    private EntityManager entityManager;

    @Override
    public void init(ConfigurableApplicationContext applicationContext) {
        beanFactory = (DefaultListableBeanFactory) applicationContext.getBeanFactory();
        this.jpaRepositoryFactory = new JpaRepositoryFactory(entityManager);
        try {
            // Make sure calls to the repository instance are intercepted for annotated transactions
            Class transactionalRepositoryProxyPostProcessor = Class.forName("org.springframework.data.repository.core.support.TransactionalRepositoryProxyPostProcessor");
            final Constructor constructor = transactionalRepositoryProxyPostProcessor.getConstructor(ListableBeanFactory.class, String.class);
            final RepositoryProxyPostProcessor repositoryProxyPostProcessor = (RepositoryProxyPostProcessor)
                    constructor.newInstance(applicationContext.getBeanFactory(), TxUtils.DEFAULT_TRANSACTION_MANAGER);
            jpaRepositoryFactory.addRepositoryProxyPostProcessor(repositoryProxyPostProcessor);
        } catch (Exception e) {
            log.error("Failed to initialize the TransactionalRepositoryProxyPostProcessor class", e);
        }
    }

    @Override
    public boolean supports(Class clazz) {
        return ClassUtils.isAssignable(clazz, org.springframework.data.repository.Repository.class);
    }

    @Override
    public void registerBean(Class clazz) throws BeansException {
        String beanName = ReloaderUtils.constructBeanName(clazz);
        final Object repository = jpaRepositoryFactory.getRepository(clazz);
        beanFactory.registerSingleton(beanName, repository);
    }
}
