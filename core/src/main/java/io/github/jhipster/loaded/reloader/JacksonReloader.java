package io.github.jhipster.loaded.reloader;

import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.deser.DeserializerCache;
import com.fasterxml.jackson.databind.ser.SerializerCache;
import io.github.jhipster.loaded.reloader.type.EntityReloaderType;
import io.github.jhipster.loaded.reloader.type.ReloaderType;
import io.github.jhipster.loaded.reloader.type.RestDtoReloaderType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.BeanFactoryUtils;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.util.ReflectionUtils;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Collection;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Reloads Jackson classes.
 */
@Component
@Order(80)
public class JacksonReloader implements Reloader {

    private final Logger log = LoggerFactory.getLogger(JacksonReloader.class);

    private ConfigurableApplicationContext applicationContext;

    @Override
    public void init(ConfigurableApplicationContext applicationContext) {
        log.debug("Hot reloading Jackson enabled");
        this.applicationContext = applicationContext;
    }

    @Override
    public boolean supports(Class<? extends ReloaderType> reloaderType) {
        return reloaderType.equals(EntityReloaderType.class) || reloaderType.equals(RestDtoReloaderType.class);
    }

    @Override
    public void prepare() {}

    @Override
    public boolean hasBeansToReload() {
        return false;
    }

    @Override
    public void addBeansToReload(Collection<Class> classes, Class<? extends ReloaderType> reloaderType) {
        // Do nothing. We just need to know that an Entity or a RestDTO class have been compiled
        // So we need to reload the Jackson classes
    }

    @Override
    public void reload() {
        log.debug("Hot reloading Jackson classes");
        try {
            ConfigurableListableBeanFactory beanFactory = applicationContext.getBeanFactory();
            Collection<ObjectMapper> mappers = BeanFactoryUtils
                    .beansOfTypeIncludingAncestors(beanFactory, ObjectMapper.class)
                    .values();

            for (ObjectMapper mapper : mappers) {
                log.trace("Flushing Jackson root deserializer cache");
                final Field rootDeserializersField = ReflectionUtils.findField(mapper.getClass(), "_rootDeserializers");
                ReflectionUtils.makeAccessible(rootDeserializersField);
                ((ConcurrentHashMap) ReflectionUtils.getField(rootDeserializersField, mapper)).clear();

                log.trace("Flushing Jackson serializer cache");
                SerializerProvider serializerProvider = mapper.getSerializerProvider();
                Field serializerCacheField = serializerProvider.getClass().getSuperclass().getSuperclass().getDeclaredField("_serializerCache");
                ReflectionUtils.makeAccessible(serializerCacheField);
                SerializerCache serializerCache = (SerializerCache) serializerCacheField.get(serializerProvider);
                Method serializerCacheFlushMethod = SerializerCache.class.getDeclaredMethod("flush");
                serializerCacheFlushMethod.invoke(serializerCache);

                log.trace("Flushing Jackson deserializer cache");
                DeserializationContext deserializationContext = mapper.getDeserializationContext();
                Field deSerializerCacheField = deserializationContext.getClass().getSuperclass().getSuperclass().getDeclaredField("_cache");
                ReflectionUtils.makeAccessible(deSerializerCacheField);
                DeserializerCache deSerializerCache = (DeserializerCache) deSerializerCacheField.get(deserializationContext);
                Method deSerializerCacheFlushMethod = DeserializerCache.class.getDeclaredMethod("flushCachedDeserializers");
                deSerializerCacheFlushMethod.invoke(deSerializerCache);
            }
        } catch (Exception e) {
            log.warn("Could not hot reload Jackson class!", e);
        }
    }
}
