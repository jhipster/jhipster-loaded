package io.github.jhipster.loaded.hibernate;

import org.hibernate.jpa.boot.spi.Bootstrap;
import org.springframework.orm.jpa.persistenceunit.MutablePersistenceUnitInfo;

import javax.persistence.*;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.metamodel.Metamodel;
import javax.persistence.spi.PersistenceUnitInfo;
import java.util.Collection;
import java.util.Map;

/**
 * Wrapper around the Hibernate EntityManagerFactory. This will be used to reload the entity manager
 * when an Entity is reloaded.
 */
public class JHipsterEntityManagerFactoryWrapper implements EntityManagerFactory {

    private PersistenceUnitInfo info;
    private Map properties;
    private EntityManagerFactory entityManagerFactory;
    private static JHipsterEntityManagerFactoryWrapper instance;

    public JHipsterEntityManagerFactoryWrapper(PersistenceUnitInfo info, Map properties) {
        this.info = info;
        this.properties = properties;
        instance = this;
        build(null);
    }

    /**
     * Reload the Entity manager factory
     * @param entities list of entities to load
     */
    public static void reload(Collection<Class> entities) {
        instance.build(entities);
    }

    private void build(Collection<Class> entities) {
        // Add new entities if not exists
        if (entities != null) {
            MutablePersistenceUnitInfo mutablePersistenceUnitInfo = (MutablePersistenceUnitInfo) info;
            for (Class entity : entities) {
                mutablePersistenceUnitInfo.addManagedClassName(entity.getName());
            }
        }
        entityManagerFactory = Bootstrap.getEntityManagerFactoryBuilder(info, properties).build();
    }

    public EntityManager createEntityManager() {
        return entityManagerFactory.createEntityManager();
    }

    public EntityManager createEntityManager(Map map) {
        return entityManagerFactory.createEntityManager(map);
    }

    public EntityManager createEntityManager(SynchronizationType synchronizationType) {
        return entityManagerFactory.createEntityManager(synchronizationType);
    }

    public EntityManager createEntityManager(SynchronizationType synchronizationType, Map map) {
        return entityManagerFactory.createEntityManager(synchronizationType, map);
    }

    public CriteriaBuilder getCriteriaBuilder() {
        return entityManagerFactory.getCriteriaBuilder();
    }

    public Metamodel getMetamodel() {
        return entityManagerFactory.getMetamodel();
    }

    public boolean isOpen() {
        return entityManagerFactory.isOpen();
    }

    public void close() {
        entityManagerFactory.close();
    }

    public Map<String, Object> getProperties() {
        return entityManagerFactory.getProperties();
    }

    public Cache getCache() {
        return entityManagerFactory.getCache();
    }

    public PersistenceUnitUtil getPersistenceUnitUtil() {
        return entityManagerFactory.getPersistenceUnitUtil();
    }

    public void addNamedQuery(String name, Query query) {
        entityManagerFactory.addNamedQuery(name, query);
    }

    public <T> T unwrap(Class<T> cls) {
        return entityManagerFactory.unwrap(cls);
    }

    public <T> void addNamedEntityGraph(String graphName, EntityGraph<T> entityGraph) {
        entityManagerFactory.addNamedEntityGraph(graphName, entityGraph);
    }
}
