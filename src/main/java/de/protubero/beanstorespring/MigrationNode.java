package de.protubero.beanstorespring;

public class MigrationNode implements Comparable<MigrationNode> {

	private String name;
	private int order;
	private BeanStoreMigration migration;

	public MigrationNode(String name, int order, BeanStoreMigration migration) {
		this.name = name;
		this.order = order;
		this.migration = migration;
	}

	public String getName() {
		return name;
	}

	public int getOrder() {
		return order;
	}

	public BeanStoreMigration getMigration() {
		return migration;
	}

	@Override
	public int compareTo(MigrationNode o) {
		return Integer.compare(order, o.order);
	}
	
}
