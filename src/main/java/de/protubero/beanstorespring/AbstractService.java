package de.protubero.beanstorespring;

import java.beans.PropertyDescriptor;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.server.ResponseStatusException;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import de.protubero.beanstore.api.BeanStore;
import de.protubero.beanstore.api.EntityStoreSnapshot;
import de.protubero.beanstore.entity.AbstractEntity;
import de.protubero.beanstore.entity.EntityCompanion;
import de.protubero.beanstore.tx.TransactionFailure;
import jakarta.servlet.http.HttpServletResponse;

public abstract class AbstractService<T extends AbstractEntity> {

	public static class Wrapper<X> {
		X value;
	}
	
	@Autowired
	protected BeanStore store;

	@Autowired
	protected ObjectMapper objectMapper;
	
	protected Class<T> beanClass;

	public AbstractService(Class<T> beanClass) {
		this.beanClass = Objects.requireNonNull(beanClass);
	}
	
	protected EntityStoreSnapshot<T> entityStore() {
		return store.snapshot().entity(beanClass);
	}
	
	@GetMapping
	public List<T> getAll() {
		return entityStore().asList();
	}
	
	@GetMapping(value = "/{id}")
	public T findById(@PathVariable("id") Long id) {
		T result = entityStore().find(id);
		if (result == null) {
			throw new ResponseStatusException(HttpStatus.NOT_FOUND,  beanClass.getSimpleName() + " not found");
		} else {
			return result;
		}
	}
	
	@DeleteMapping(value = "/{id}")
	@ResponseStatus(HttpStatus.OK)
	public void delete(@PathVariable("id") Long id) {
		try {
			store.delete(beanClass, id, null);
		} catch (TransactionFailure txFailure) {
			switch (txFailure.getType()) {
			case INSTANCE_NOT_FOUND:
				throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Instance not found");
			case OPTIMISTIC_LOCKING_FAILED:
				throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Optimistic Locking Failed");
			case VERIFICATION_FAILED:
				throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to verify change: " + txFailure.getMessage());
			case PERSISTENCE_FAILED:				
				throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to persist change");
			}	
		}
	}
	
	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	public void create(@RequestBody T instance, HttpServletResponse response) {
		try {
			var tx = store.transaction();
			T newInstance = tx.create(instance);
	    	tx.execute();			
			
			String locationHeaderValue = String.valueOf(newInstance.id());
			
			RequestMapping requestMapping = getClass().getAnnotation(RequestMapping.class);
			if (requestMapping != null) {
				locationHeaderValue = requestMapping.value()[0] + "/" + locationHeaderValue;
			}
			response.addHeader("Location", locationHeaderValue);
		} catch (TransactionFailure txFailure) {
			switch (txFailure.getType()) {
			case INSTANCE_NOT_FOUND:
			case OPTIMISTIC_LOCKING_FAILED:
				throw new AssertionError("unexpected");
			case VERIFICATION_FAILED:
				throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to verify change: " + txFailure.getMessage());
			case PERSISTENCE_FAILED:				
				throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to persist change");
			}	
		}
	}

	@PutMapping(value = "/{id}")
	@ResponseStatus(HttpStatus.OK)
	public void update(@PathVariable("id") Long id, @RequestBody JsonNode jsonNode) {
		if (!jsonNode.isObject()) {
			throw new RuntimeException("json has to be an object");
		}

		Wrapper<Integer> version = new Wrapper<>();
		
		EntityCompanion<T> companion = (EntityCompanion<T>) entityStore().meta();
		Map<String, Object> updatedFields = new HashMap<>();
		jsonNode.fields().forEachRemaining(field -> {
			if (field.getKey().startsWith("_")) {
				if (field.getKey().substring(1).equals("version")) {
					version.value = field.getValue().asInt();
				}
			} else {
				PropertyDescriptor propDesc = companion.propertyDescriptorOf(field.getKey());
				if (propDesc == null) {
					throw new RuntimeException("Invalid field name: " + field.getKey());
				}
				Object value;
				try {
					value = objectMapper.treeToValue(field.getValue(), propDesc.getPropertyType());
				} catch (JsonProcessingException | IllegalArgumentException e) {
					throw new RuntimeException("Error reading json", e);
				}
				
				updatedFields.put(field.getKey(), value);
			}	
		});	

		
		try {
			store.update(beanClass, id, version.value, updatedFields);
		} catch (TransactionFailure txFailure) {
			switch (txFailure.getType()) {
			case INSTANCE_NOT_FOUND:
				throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Instance not found");
			case OPTIMISTIC_LOCKING_FAILED:
				throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Optimistic Locking Failed");
			case VERIFICATION_FAILED:
				throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to verify change: " + txFailure.getMessage());
			case PERSISTENCE_FAILED:				
				throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to persist change");
			}	
		}
	}
	
	
}
