package de.protubero.beanstorespring;

import java.lang.annotation.Annotation;
import java.util.HashSet;
import java.util.Set;

import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.core.type.filter.AnnotationTypeFilter;
import org.springframework.stereotype.Component;

public class ClassScanner {

	public Set<String> findAnnotatedClasses(Class<? extends Annotation> annotationType, String... packagesToBeScanned)
	{
	    ClassPathScanningCandidateComponentProvider provider = new ClassPathScanningCandidateComponentProvider(false);
	    provider.addIncludeFilter(new 
	    AnnotationTypeFilter(annotationType));

	    Set<String> ret = new HashSet<>();

	    for (String pkg : packagesToBeScanned)
	    {
	         Set<BeanDefinition> beanDefs = provider.findCandidateComponents(pkg);
	         beanDefs.stream()
	             .map(BeanDefinition::getBeanClassName)
	             .forEach(ret::add);
	    }

	    return ret;
	}
	
}
