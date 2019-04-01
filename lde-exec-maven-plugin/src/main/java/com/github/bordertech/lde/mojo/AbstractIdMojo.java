package com.github.bordertech.lde.mojo;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugins.annotations.Parameter;

/**
 * Abstract MOJO that has the LDE Provider ID.
 */
public abstract class AbstractIdMojo extends AbstractMojo {

	@Parameter(defaultValue = "default")
	private String providerId;

	/**
	 * @return the provider id
	 */
	protected String getProviderId() {
		return providerId;
	}

}
