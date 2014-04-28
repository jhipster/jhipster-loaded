package io.github.jhipster.loaded.reloader.loader;

import com.mongodb.Mongo;
import io.github.jhipster.loaded.reloader.ReloaderUtils;
import org.apache.commons.lang.ClassUtils;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.repository.support.MongoRepositoryFactory;
import org.springframework.stereotype.Component;

import javax.inject.Inject;

/**
 * Default loader used to load a new spring bean independent of the type
 *
 */
@Component
@Order(Ordered.LOWEST_PRECEDENCE - 1)
@ConditionalOnClass(Mongo.class)
public class MongoSpringLoader implements SpringLoader {

    private DefaultListableBeanFactory beanFactory;
    private MongoRepositoryFactory mongoRepositoryFactory;

    @Inject
    private MongoTemplate mongoTemplate;

    @Override
    public void init(ConfigurableApplicationContext applicationContext) {
        beanFactory = (DefaultListableBeanFactory) applicationContext.getBeanFactory();
        this.mongoRepositoryFactory = new MongoRepositoryFactory(mongoTemplate);
    }

    @Override
    public boolean supports(Class clazz) {
        return ClassUtils.isAssignable(clazz, org.springframework.data.repository.Repository.class);
    }

    @Override
    public void registerBean(Class clazz) throws BeansException {
        String beanName = ReloaderUtils.constructBeanName(clazz);
        final Object repository = mongoRepositoryFactory.getRepository(clazz);
        beanFactory.registerSingleton(beanName, repository);
    }
}
