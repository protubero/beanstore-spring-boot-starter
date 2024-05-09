package de.protubero.beanstorespring;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RestController;

import de.protubero.beanstore.api.BeanStore;

@RestController
public class MetaApi {

	@Autowired
	protected BeanStore store;
	
	// TBD
	
}
