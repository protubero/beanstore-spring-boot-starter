package de.protubero.beanstorespring;

import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;

import de.protubero.beanstore.builder.MigrationTransaction;
import de.protubero.beanstore.entity.MapObject;

public interface BeanStoreMigration extends Consumer<MigrationTransaction> {

	default void renameField(MigrationTransaction tx, String alias, String sourceFieldName, String targetFieldName) {
		tx.snapshot().mapEntity(alias).forEach(task ->  {
			var upd = tx.update(task);
			upd.set(targetFieldName, task.get(sourceFieldName));
			
			// .remove will not work because we do not alter the instance but create a recording obj
			upd.set(sourceFieldName, null);
		});
	}
	
	default void replaceNullValues(MigrationTransaction tx, String alias, String field, Object value) {
		replaceNullValues(tx, alias, field, obj -> value);
	}
	
	default void replaceNullValues(MigrationTransaction tx, String alias, String field, Function<MapObject, Object> valueFunction) {
		migrate(tx, alias, obj -> obj.get(field) == null, (instance, updObject) -> {
			updObject.set(field, valueFunction.apply(instance));
		});
	}
	
	default void migrate(MigrationTransaction tx, String alias, Predicate<MapObject> applyCond, BiConsumer<MapObject, MapObject> updObjectConsumer) {
		tx.snapshot().mapEntity(alias).forEach(instance ->  {
			if (applyCond.test(instance)) {
				var upd = tx.update(instance);
				updObjectConsumer.accept(instance, upd);
			}
		});
	}
	
}
