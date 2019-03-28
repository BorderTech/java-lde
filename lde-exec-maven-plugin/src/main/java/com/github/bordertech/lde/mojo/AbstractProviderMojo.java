package com.github.bordertech.lde.mojo;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugins.annotations.Parameter;

/**
 * Abstract LDE provider mojo.
 */
public abstract class AbstractProviderMojo extends AbstractMojo {

	@Parameter(defaultValue = "default")
	private String providerId;

	/**
	 * @return the provider id
	 */
	public String getProviderId() {
		return providerId;
	}

}
