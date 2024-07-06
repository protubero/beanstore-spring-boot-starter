package de.protubero.beanstorespring;

import java.io.File;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import de.protubero.beanstore.api.BeanStore;
import de.protubero.beanstore.builder.BeanStoreBuilder;
import de.protubero.beanstore.builder.MapStoreSnapshotBuilder;
import de.protubero.beanstore.entity.AbstractEntity;
import de.protubero.beanstore.entity.Entity;
import de.protubero.beanstore.persistence.api.KryoConfig;
import de.protubero.beanstore.persistence.kryo.KryoConfiguration;
import de.protubero.beanstore.persistence.kryo.KryoPersistence;
import de.protubero.beanstore.pluginapi.BeanStorePlugin;
import de.protubero.beanstore.plugins.history.BeanStoreHistoryPlugin;
import de.protubero.beanstore.plugins.search.BeanStoreSearchPlugin;
import de.protubero.beanstore.plugins.validate.BeanValidationPlugin;

@Configuration
public class BeanStoreConfiguration {

	public static Logger log = LoggerFactory.getLogger(BeanStoreConfiguration.class);

	@Autowired
	private BeanStoreProperties properties;

	@Autowired(required = false)
	private BeanStoreInitializer storeInitializer;

	@Autowired
	private ApplicationContext applicationContext;

	
	@Bean
	public KryoPersistence kryoPersistence() {
		log.info("Build Kryo Configuration");

		KryoConfiguration kryoConfig = KryoConfiguration.create();
		Set<String> kryoConfClassSet = new ClassScanner().findAnnotatedClasses(KryoConfig.class, properties.getPackages());
		kryoConfClassSet.forEach(cls -> {
			Class<?> clazz = classByName(cls);
			kryoConfig.register(clazz);
		});

		log.info("Build Kryo Persistence");
		File dataFile = new File(properties.getFile());
		KryoPersistence persistence = KryoPersistence.of(dataFile, kryoConfig);
		return persistence;
	}
	
	@SuppressWarnings({ "unchecked", "rawtypes" })
	@Bean
	public BeanStore createBeanStore(
			@Autowired KryoPersistence persistence,
			@Autowired BeanStoreSearchPlugin searchPlugin,
			@Autowired BeanStoreHistoryPlugin historyPlugin) {

		log.info("Build Bean Store Builder");
		BeanStoreBuilder builder = BeanStoreBuilder.init(persistence);
	
		if (storeInitializer != null) {
			builder.initNewStore(storeInitializer);
		}	

		// Migrations
		List<MigrationNode> migrationList = new ArrayList<>();
		Set<String> migrationBeanClassSet = new ClassScanner().findAnnotatedClasses(Migration.class, properties.getPackages());
		migrationBeanClassSet.forEach(cls -> {
			Class<?> clazz = classByName(cls);
			Migration annotation = clazz.getAnnotation(Migration.class);
			BeanStoreMigration migration = null;
			try {
				Constructor<?> noArgsConstructor = clazz.getConstructor();
				migration = (BeanStoreMigration) noArgsConstructor.newInstance();
			} catch (NoSuchMethodException | SecurityException | InstantiationException | IllegalAccessException
					| IllegalArgumentException | InvocationTargetException e) {
				throw new RuntimeException(e);
			}

			migrationList.add(new MigrationNode(annotation.name(), annotation.order(), migration));
		});
		migrationList.sort(Comparator.comparingInt(m -> m.getOrder()));
		migrationList.forEach(m -> {
			builder.addMigration(m.getName(), m.getMigration());
		});

		
		
		Set<String> dataBeanClassSet = new ClassScanner().findAnnotatedClasses(Entity.class, properties.getPackages());
		dataBeanClassSet.forEach(cls -> {
			Class<? extends AbstractEntity> clazz = (Class<? extends AbstractEntity>) classByName(cls);
			Entity entity = clazz.getAnnotation(Entity.class);
			builder.registerEntity((Class) clazz);
			if (Searchable.class.isAssignableFrom(clazz)) {
				searchPlugin.register(clazz, obj -> {
					return ((Searchable) obj).toSearchString();
				});
			}
			
			History history = clazz.getAnnotation(History.class);
			if (history != null) {
				log.info("providing history of entity " + entity.alias());
				historyPlugin.register(entity.alias());
			}
		});		
		
		Collection<BeanStorePlugin> plugins = applicationContext.getBeansOfType(BeanStorePlugin.class).values();
		plugins.forEach(plugin -> {
			log.info("registering plugin " + plugin);
			builder.addPlugin(plugin);
		});
		
		return builder.build();
	}

	
	@Bean
	public MapStoreSnapshotBuilder createMapStoreSnapshotBuilder(@Autowired KryoPersistence persistence) {
		return MapStoreSnapshotBuilder.init(persistence);
	}
	
	@Bean
	public BeanStoreSearchPlugin searchPlugin() {
		BeanStoreSearchPlugin plugin = new BeanStoreSearchPlugin();
		
		
		return plugin;
	}
	
	@Bean
	public BeanValidationPlugin beanValidation() {
		return new BeanValidationPlugin();
	}
	
	@Bean
	public BeanStoreHistoryPlugin beanStoreHistory() {
		BeanStoreHistoryPlugin result = new BeanStoreHistoryPlugin();
		return result;
	}
		

	private Class<?> classByName(String cls) {
		Class<?> clazz;
		try {
			clazz = Class.forName(cls);
		} catch (ClassNotFoundException e) {
			throw new RuntimeException(e);
		}
		return clazz;
	}

}
