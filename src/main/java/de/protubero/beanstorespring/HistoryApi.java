package de.protubero.beanstorespring;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import de.protubero.beanstore.entity.PersistentObjectKey;
import de.protubero.beanstore.plugins.history.BeanStoreHistoryPlugin;
import de.protubero.beanstore.plugins.history.InstanceState;

@RestController
public class HistoryApi {

	@Autowired
	private BeanStoreHistoryPlugin historyPlugin;
	
	
	@GetMapping(value = "/history/{type}/{id}")
    public List<InstanceState> historyById(@PathVariable("type") String type, @PathVariable("id") int id) {
        return historyPlugin.changes(PersistentObjectKey.of(type, id));
    }
	
}
