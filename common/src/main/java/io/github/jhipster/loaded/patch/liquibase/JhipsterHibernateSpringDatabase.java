package io.github.jhipster.loaded.patch.liquibase;

import liquibase.ext.hibernate.database.HibernateSpringDatabase;
import liquibase.ext.hibernate.database.connection.HibernateConnection;
import org.hibernate.cfg.Configuration;
import org.hibernate.jpa.boot.internal.EntityManagerFactoryBuilderImpl;
import org.hibernate.jpa.boot.spi.Bootstrap;
import org.hibernate.service.ServiceRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.orm.jpa.persistenceunit.DefaultPersistenceUnitManager;
import org.springframework.orm.jpa.persistenceunit.SmartPersistenceUnitInfo;
import org.springframework.orm.jpa.vendor.HibernateJpaVendorAdapter;

import javax.persistence.spi.PersistenceUnitInfo;
import java.util.Map;

/**
 * Support for Spring 4.0.
 * <p/>
 * Actually we are not able to use the version 3.4 because liquibase version 3.2.0
 * doesn't work with SpringBoot
 */
public class JhipsterHibernateSpringDatabase extends HibernateSpringDatabase {

    private Logger log = LoggerFactory.getLogger(JhipsterHibernateSpringDatabase.class);

    public JhipsterHibernateSpringDatabase(String catalog, String schema) {
        setDefaultCatalogName(catalog);
        setDefaultSchemaName(schema);
    }

    @Override
    public Configuration buildConfigurationFromScanning(HibernateConnection connection) {
        String[] packagesToScan = connection.getPath().split(",");

        for (String packageName : packagesToScan) {
            log.info("Found package {}", packageName);
        }

        DefaultPersistenceUnitManager internalPersistenceUnitManager = new DefaultPersistenceUnitManager();

        internalPersistenceUnitManager.setPackagesToScan(packagesToScan);

        String dialectName = connection.getProperties().getProperty("dialect", null);
        if (dialectName == null) {
            throw new IllegalArgumentException("A 'dialect' has to be specified.");
        }
        log.info("Found dialect {}", dialectName);

        internalPersistenceUnitManager.preparePersistenceUnitInfos();
        PersistenceUnitInfo persistenceUnitInfo = internalPersistenceUnitManager.obtainDefaultPersistenceUnitInfo();
        HibernateJpaVendorAdapter jpaVendorAdapter = new HibernateJpaVendorAdapter();
        jpaVendorAdapter.setDatabasePlatform(dialectName);

        Map<String, Object> jpaPropertyMap = jpaVendorAdapter.getJpaPropertyMap();
        jpaPropertyMap.put("hibernate.archive.autodetection", "false");

        if (persistenceUnitInfo instanceof SmartPersistenceUnitInfo) {
            ((SmartPersistenceUnitInfo) persistenceUnitInfo).setPersistenceProviderPackageName(jpaVendorAdapter.getPersistenceProviderRootPackage());
        }

        EntityManagerFactoryBuilderImpl builder = (EntityManagerFactoryBuilderImpl) Bootstrap.getEntityManagerFactoryBuilder(persistenceUnitInfo,
                jpaPropertyMap);

        ServiceRegistry serviceRegistry = builder.buildServiceRegistry();
        return builder.buildHibernateConfiguration(serviceRegistry);
    }

    @Override
    public boolean supportsCatalogs() {
        return true;
    }

    @Override
    public boolean supportsSchemas() {
        return true;
    }
}
