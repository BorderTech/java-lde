package com.github.bordertech.lde.mojo;

import com.github.bordertech.lde.api.LdeProvider;
import java.io.File;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
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
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.apache.maven.project.MavenProject;

/**
 * Start LDE provider.
 * <p>
 * Will create a new ClassLoader for the provider to run in so it is isolated from maven.
 * </p>
 */
@Mojo(name = "start", defaultPhase = LifecyclePhase.PRE_INTEGRATION_TEST, requiresDependencyResolution = ResolutionScope.TEST)
public class StartMojo extends AbstractProviderMojo {

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

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        // Get provider
        LdeProvider provider = createProvider();
        // Start
        provider.launchServer(block);
        // Wait till ready
        waitTillProviderReady(provider);
        // Save provider
        CrossStateUtil.addProvider(getProviderId(), provider);
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
    private LdeProvider createProvider() throws MojoExecutionException {
        // Create a new class loader to run the provider in
        ClassLoader loader = createClassLoader();
        Thread.currentThread().setContextClassLoader(loader);
        try {
            // Load the provider impl from the new class laoder
            Object providerImpl = loader.loadClass(providerClassName).newInstance();
            // Create a proxy to cross the Class Loader
            return (LdeProvider) Proxy.newProxyInstance(LdeProvider.class.getClassLoader(), new Class<?>[]{LdeProvider.class}, new MojoProxy(providerImpl));
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
    private ClassLoader createClassLoader() throws MojoExecutionException {
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
    private void addElementsToUrls(final List<URL> urls, final List<String> elements) throws MalformedURLException {
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
     * Pass through proxy so the the MOJO can access the IMPL created in the new ClassLoader.
     */
    private static class MojoProxy implements InvocationHandler {

        private final Object target;

        /**
         * @param target provider implementation
         */
        public MojoProxy(final Object target) {
            this.target = target;
        }

        @Override
        public Object invoke(final Object proxy, final Method method, final Object[] args) throws Throwable {
            Method implMethod = target.getClass().getMethod(method.getName(), method.getParameterTypes());
            return implMethod.invoke(target, args);
        }

    }
}
