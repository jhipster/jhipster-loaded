package io.github.jhipster.loaded.instrument;

import javassist.*;
import org.springsource.loaded.LoadtimeInstrumentationPlugin;

import java.security.ProtectionDomain;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Instrument the classes loaded at runtime.
 * Be able to change the default behavior of a class before adding it in the ClassLoader
 */
public class JHipsterLoadtimeInstrumentationPlugin implements LoadtimeInstrumentationPlugin {

    private final Logger log = Logger.getLogger(JHipsterLoadtimeInstrumentationPlugin.class.getName());

    @Override
    public boolean accept(String slashedTypeName, ClassLoader classLoader, ProtectionDomain protectionDomain, byte[] bytes) {
        return slashedTypeName != null && (slashedTypeName.equals("org/springframework/security/access/method/DelegatingMethodSecurityMetadataSource") ||
                slashedTypeName.equals("org/springframework/aop/framework/AdvisedSupport") ||
                slashedTypeName.equals("liquibase/ext/hibernate/snapshot/TableSnapshotGenerator") ||
                slashedTypeName.equals("org/hibernate/jpa/HibernatePersistenceProvider") ||
                slashedTypeName.equals("org/hibernate/engine/internal/CacheHelper") ||
                slashedTypeName.equals("org/springframework/data/repository/core/support/TransactionalRepositoryProxyPostProcessor") ||
                slashedTypeName.equals("org/springframework/core/LocalVariableTableParameterNameDiscoverer"));
    }

    @Override
    public byte[] modify(String slashedClassName, ClassLoader classLoader, byte[] bytes) {
        ClassPool classPool = ClassPool.getDefault();
        classPool.appendClassPath(new LoaderClassPath(Thread.currentThread().getContextClassLoader()));
        classPool.appendClassPath(new LoaderClassPath(classLoader));

        try {
            // Remove final from a class definition to be able to proxy it. @See JHipsterReloadWebSecurityConfig class
            if (slashedClassName.equals("org/springframework/security/access/method/DelegatingMethodSecurityMetadataSource")) {
                CtClass ctClass = classPool.get("org.springframework.security.access.method.DelegatingMethodSecurityMetadataSource");
                CtMethod ctMethod = ctClass.getDeclaredMethod("getAttributes");
                ctMethod.insertBefore("{synchronized (attributeCache) { attributeCache.clear();} }");
                bytes = ctClass.toBytecode();
                ctClass.defrost();

                log.fine("Patch - Rewrite org.springframework.security.access.method.DelegatingMethodSecurityMetadataSource.getAttributes() method");
            }

            // The AdvisedSupport is in charge to manage the advised associated to a method.
            // By default, it used a cache which avoid to reload any advises like @RolesAllowed, @Timed etc...
            // So if a method has @Timed when the application is started and wants to add a @RolesAllowed,
            // the last added annotation is not advised because the cache is used.
            // The call to the method adviceChanged will clear the cache
            if (slashedClassName.equals("org/springframework/aop/framework/AdvisedSupport")) {
                CtClass ctClass = classPool.get("org.springframework.aop.framework.AdvisedSupport");
                CtMethod ctMethod = ctClass.getDeclaredMethod("getInterceptorsAndDynamicInterceptionAdvice");
                ctMethod.insertBefore("{ adviceChanged(); }");
                bytes = ctClass.toBytecode();
                ctClass.defrost();

                log.fine("Patch - Rewrite org.springframework.aop.framework.AdvisedSupport.getInterceptorsAndDynamicInterceptionAdvice() method");
            }

            // Change the super class from TableSnapshotGenerator to JHipsterTableSnapshotGenerator.
            // Quick fix for a NPE. @see JHipsterTableSnapshotGenerator
            if (slashedClassName.equals("liquibase/ext/hibernate/snapshot/TableSnapshotGenerator")) {
                CtClass ctClass = classPool.get("liquibase.ext.hibernate.snapshot.TableSnapshotGenerator");
                ctClass.setSuperclass(classPool.get("io.github.jhipster.loaded.patch.liquibase.JHipsterTableSnapshotGenerator"));
                CtMethod ctMethod = ctClass.getDeclaredMethod("snapshotObject");
                ctMethod.setBody("{ return super.snapshotObject($1, $2);}");
                bytes = ctClass.toBytecode();
                ctClass.defrost();

                log.fine("Patch - Rewrite liquibase.ext.hibernate.snapshot.TableSnapshotGenerator.snapshotObject() method");
            }

            // Add JHipsterPersistenceProvider class as the super class.
            // It will wrap the Hibernate entityManagerFactory to be able to reload it.
            if (slashedClassName.equals("org/hibernate/jpa/HibernatePersistenceProvider")) {
                CtClass ctClass = classPool.get("org.hibernate.jpa.HibernatePersistenceProvider");
                ctClass.setSuperclass(classPool.get("io.github.jhipster.loaded.hibernate.JHipsterPersistenceProvider"));
                CtMethod ctMethod = ctClass.getDeclaredMethod("createContainerEntityManagerFactory");
                ctMethod.setBody("{ return super.createContainerEntityManagerFactory($1, $2); }");
                bytes = ctClass.toBytecode();
                ctClass.defrost();

                log.fine("Patch - Rewrite org.hibernate.jpa.HibernatePersistenceProvider.createContainerEntityManagerFactory() method");
            }


            // JHipster used second level caching so by default every entity is cached.
            // The second level caching is managed by the class @see org.hibernate.engine.internal.CacheHelper
            // So when the second level caching is enabled and if an entity is updated (add or remove or update a new field)
            // the cached entity is returned and the code doesn't work.
            if (slashedClassName.equals("org/hibernate/engine/internal/CacheHelper")) {
                CtClass ctClass = classPool.get("org.hibernate.engine.internal.CacheHelper");
                CtClass sessionClass = classPool.get("org.hibernate.engine.spi.SessionImplementor");
                CtClass cacheKeyClass = classPool.get("org.hibernate.cache.spi.CacheKey");
                CtClass regionAccessStrategyClass = classPool.get("org.hibernate.cache.spi.access.RegionAccessStrategy");
                CtMethod ctMethod = ctClass.getDeclaredMethod("fromSharedCache", new CtClass[]{sessionClass, cacheKeyClass, regionAccessStrategyClass});
                ctMethod.setBody("{ return null; }");
                bytes = ctClass.toBytecode();
                ctClass.defrost();

                log.fine("Patch - Rewrite org.hibernate.engine.internal.CacheHelper.fromSharedCache() method");
            }

            // Make TransactionalRepositoryProxyPostProcessor public to use by SpringLoader to initialize
            // the Jpa repository factory.
            if (slashedClassName.equals("org/springframework/data/repository/core/support/TransactionalRepositoryProxyPostProcessor")) {
                CtClass ctClass = classPool.get("org.springframework.data.repository.core.support.TransactionalRepositoryProxyPostProcessor");
                ctClass.setModifiers(Modifier.PUBLIC);
                bytes = ctClass.toBytecode();
                ctClass.defrost();

                log.fine("Patch - Make org.springframework.data.repository.core.support.TransactionalRepositoryProxyPostProcessor class PUBLIC");
            }

            // The parameters are cached and when a class is reloaded, the map used for the caching is not able to
            // return the cached parameters for a class or a method or a constructor.
            // So the cache will be clear everytime the getParameterNames method is called
            if (slashedClassName.equals("org/springframework/core/LocalVariableTableParameterNameDiscoverer")) {
                CtClass ctClass = classPool.get("org.springframework.core.LocalVariableTableParameterNameDiscoverer");
                CtMethod ctMethod = ctClass.getDeclaredMethod("getParameterNames", new CtClass[]{classPool.get("java.lang.reflect.Method")});
                ctMethod.insertBefore("{ this.parameterNamesCache.clear(); }");
                ctMethod = ctClass.getDeclaredMethod("getParameterNames", new CtClass[]{classPool.get("java.lang.reflect.Constructor")});
                ctMethod.insertBefore("{ this.parameterNamesCache.clear(); }");
                bytes = ctClass.toBytecode();
                ctClass.defrost();

                log.fine("Patch - Clear cache org.springframework.core.LocalVariableTableParameterNameDiscoverer");
            }
        } catch (Exception e) {
            log.log(Level.SEVERE, "Failed to modify the DelegatingMethodSecurityMetadataSource class", e);
        }
        return bytes;
    }
}
