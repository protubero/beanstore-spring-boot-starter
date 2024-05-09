package de.protubero.beanstorespring;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import de.protubero.beanstore.api.BeanStoreSnapshot;
import de.protubero.beanstore.builder.MapStoreSnapshotBuilder;
import de.protubero.beanstore.builder.blocks.BeanStoreState;

@RestController
public class StateApi {

	
	@Autowired
	private MapStoreSnapshotBuilder builder;
	
	@GetMapping(value = "/states/{state}")
    public BeanStoreSnapshot historyById(@PathVariable("state") int state) {
        return builder.build(state);
    }	
	
	@GetMapping(value = "/states")
    public List<BeanStoreState> states() {
        return builder.states();
    }	
}
