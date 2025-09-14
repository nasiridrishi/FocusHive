package com.focushive.identity.security.encryption.converters;

import com.focushive.identity.security.encryption.IEncryptionService;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.stereotype.Component;

/**
 * Utility class to provide Spring-managed beans to non-Spring managed objects like JPA converters.
 * This is needed because JPA converters are instantiated by Hibernate, not Spring,
 * so normal dependency injection doesn't work.
 */
@Component
public class SpringContextUtil implements ApplicationContextAware {
    
    private static ApplicationContext context;
    
    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        SpringContextUtil.context = applicationContext;
    }
    
    /**
     * Get the Spring ApplicationContext.
     * @return the application context
     */
    public static ApplicationContext getApplicationContext() {
        return context;
    }
    
    /**
     * Get a Spring-managed bean by type.
     * @param beanClass the bean class
     * @return the bean instance
     */
    public static <T> T getBean(Class<T> beanClass) {
        if (context == null) {
            return null;
        }
        try {
            return context.getBean(beanClass);
        } catch (BeansException e) {
            return null;
        }
    }
    
    /**
     * Get the encryption service bean.
     * @return the encryption service, or null if not available
     */
    public static IEncryptionService getEncryptionService() {
        return getBean(IEncryptionService.class);
    }
}