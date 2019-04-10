package com.github.bordertech.lde.mojo;

import com.github.bordertech.lde.api.ConfigUtil;
import com.github.bordertech.lde.api.LdeProvider;
import java.io.File;
import java.lang.reflect.Proxy;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import org.apache.maven.artifact.DependencyResolutionRequiredException;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;

/**
 * Abstract LDE Provider instance creator.
 */
public abstract class AbstractStartMojo extends AbstractIdMojo {

	@Parameter(defaultValue = "com.github.bordertech.lde.api.LaunchWrapperProvider")
	private String providerClassName;

	@Parameter(defaultValue = "30")
	private int waitReadySeconds;

	@Parameter(defaultValue = "false")
	private boolean block;

	@Parameter(defaultValue = "test")
	private String scope;

	@Component
	private MavenProject project;

	/**
	 * Create and start the LDE provider instance.
	 *
	 * @throws MojoExecutionException a MOJO execution exception
	 * @throws MojoFailureException a MOJO failure exception
	 */
	protected void createAndStartProvider() throws MojoExecutionException, MojoFailureException {
		// Check for config overrides
		setupProviderConfig();
		// Get provider
		LdeProvider provider = createProvider();
		// Start
		provider.launchServer(isBlock());
		// Wait till ready
		waitTillProviderReady(provider);
		// Save provider
		CrossStateUtil.addProvider(getProviderId(), provider);
	}

	/**
	 * @return the provider class name
	 */
	protected String getProviderClassName() {
		return providerClassName;
	}

	/**
	 * @return the wait interval for the server to be ready
	 */
	protected int getWaitReadySeconds() {
		return waitReadySeconds;
	}

	/**
	 * @return true if block on start server
	 */
	protected boolean isBlock() {
		return block;
	}

	/**
	 * @return the class path scope.
	 */
	protected String getScope() {
		return scope;
	}

	/**
	 * @return the maven project
	 */
	protected MavenProject getProject() {
		return project;
	}

	/**
	 * Wait till provider is ready.
	 *
	 * @param provider the provider to check
	 * @throws MojoExecutionException MOJO exception
	 */
	protected void waitTillProviderReady(final LdeProvider provider) throws MojoExecutionException {
		long startTime = new Date().getTime();
		long wait = waitReadySeconds * 1000;
		while (!provider.isRunning()) {
			waitInterval();
			long diff = new Date().getTime() - startTime;
			if (diff > wait) {
				throw new MojoExecutionException("Timeout waiting for provider to be ready.");
			}
		}
	}

	/**
	 * Put thread to sleep.
	 */
	protected void waitInterval() {
		try {
			Thread.sleep(1000);
		} catch (InterruptedException ex) {
			Thread.currentThread().interrupt();
			throw new IllegalStateException("Waiting for provider was interrupted. " + ex.getMessage(), ex);
		}
	}

	/**
	 * Create the provider implementation using the custom class loader.
	 * <p>
	 * A proxy is required as the {@link LdeProvider} API has been loaded in both class loaders.
	 * </p>
	 *
	 * @return the provider implementation
	 * @throws MojoExecutionException MOJO exception
	 */
	protected LdeProvider createProvider() throws MojoExecutionException {
		// Create a new class loader to run the provider in
		ClassLoader loader = createClassLoader();
		Thread.currentThread().setContextClassLoader(loader);
		try {
			// Load the provider impl from the new class laoder
			Object providerImpl = loader.loadClass(providerClassName).newInstance();
			// Create a proxy to cross the Class Loader
			return (LdeProvider) Proxy.newProxyInstance(LdeProvider.class.getClassLoader(), new Class<?>[]{LdeProvider.class}, new MojoProviderProxy(providerImpl));
		} catch (ClassNotFoundException | IllegalAccessException | InstantiationException e) {
			throw new MojoExecutionException("Could not create provider impl [" + providerClassName + "]. " + e.getMessage(), e);
		}
	}

	/**
	 * Create a class loader with the project dependencies.
	 *
	 * @return the provider class loader with all the project dependencies
	 * @throws MojoExecutionException MOJO exception
	 */
	protected ClassLoader createClassLoader() throws MojoExecutionException {
		try {
			// Project Classpath
			List<URL> urls = new ArrayList<>();
			// Add project scope
			String scp = scope.toLowerCase();
			switch (scp) {
				case "compile":
					addElementsToUrls(urls, project.getCompileClasspathElements());
					break;
				case "test":
					addElementsToUrls(urls, project.getTestClasspathElements());
					break;
				case "runtime":
					addElementsToUrls(urls, project.getRuntimeClasspathElements());
					break;
				default:
					// Default to TEST
					addElementsToUrls(urls, project.getTestClasspathElements());
					break;
			}
			// TODO Maybe add plugin dependencies?
			return new URLClassLoader(urls.toArray(new URL[]{}), null);
		} catch (MalformedURLException | DependencyResolutionRequiredException e) {
			throw new MojoExecutionException("Could not create plugin classpath. " + e.getMessage(), e);
		}
	}

	/**
	 * Convert class path elements to URLs.
	 *
	 * @param urls the list of URLS so far
	 * @param elements the list of elements on class path
	 * @throws MalformedURLException a URL was malformed
	 */
	protected void addElementsToUrls(final List<URL> urls, final List<String> elements) throws MalformedURLException {
		if (elements == null) {
			return;
		}
		for (String element : elements) {
			URL url = new File(element).toURI().toURL();
			if (!urls.contains(url)) {
				urls.add(url);
			}
		}
	}

	/**
	 * Pass the MOJO settings to the provider.
	 *
	 * @throws MojoFailureException exception if cannot setup configuration
	 */
	protected void setupProviderConfig() throws MojoFailureException {
		// Working Directory
		String basedir = project.getBasedir().getAbsolutePath();
		System.setProperty(ConfigUtil.PARAM_WORKING_DIRECTORY_KEY, basedir);
	}

}
