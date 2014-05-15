package io.github.jhipster.loaded.reloader;

import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Controller;
import org.springframework.stereotype.Repository;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.RestController;

import java.lang.annotation.Annotation;

/**
 * Utility class
 */
public class ReloaderUtils {

    public static Annotation getSpringClassAnnotation(Class clazz) {
        Annotation classAnnotation = null;

        if (AnnotationUtils.isAnnotationDeclaredLocally(Controller.class, clazz)) {
            classAnnotation = AnnotationUtils.findAnnotation(clazz, Controller.class);
        }
        if (AnnotationUtils.isAnnotationDeclaredLocally(RestController.class, clazz)) {
            classAnnotation = AnnotationUtils.findAnnotation(clazz, RestController.class);
        }
        if (AnnotationUtils.isAnnotationDeclaredLocally(Service.class, clazz)) {
            classAnnotation = AnnotationUtils.findAnnotation(clazz, Service.class);
        }
        if (AnnotationUtils.isAnnotationDeclaredLocally(Repository.class, clazz)) {
            classAnnotation = AnnotationUtils.findAnnotation(clazz, Repository.class);
        }
        if (AnnotationUtils.isAnnotationDeclaredLocally(Component.class, clazz)) {
            classAnnotation = AnnotationUtils.findAnnotation(clazz, Component.class);
        }

        return classAnnotation;
    }


    public static String getScope(Class clazz) {
        String scope = ConfigurableBeanFactory.SCOPE_SINGLETON;
        Annotation scopeAnnotation = AnnotationUtils.findAnnotation(clazz, Scope.class);
        if (scopeAnnotation != null) {
            scope = (String) AnnotationUtils.getValue(scopeAnnotation);
        }
        return scope;
    }

    public static String constructBeanName(Class clazz) {
        Annotation annotation = ReloaderUtils.getSpringClassAnnotation(clazz);
        String beanName = (String) AnnotationUtils.getValue(annotation);
        if (beanName == null || beanName.isEmpty()) {
            beanName = StringUtils.uncapitalize(clazz.getSimpleName());
        }
        return beanName;
    }
}
