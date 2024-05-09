package de.protubero.beanstorespring;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix="de.protubero.beanstore")
public class BeanStoreProperties {

	private String file;
	
	private String[] packages;

	public String getFile() {
		return file;
	}

	public void setFile(String file) {
		this.file = file;
	}

	public String[] getPackages() {
		return packages;
	}

	public void setPackages(String[] packages) {
		this.packages = packages;
	}

	
}
