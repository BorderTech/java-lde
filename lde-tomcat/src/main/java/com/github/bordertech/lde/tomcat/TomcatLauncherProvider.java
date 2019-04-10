package com.github.bordertech.lde.tomcat;

import com.github.bordertech.didums.Didums;
import com.github.bordertech.lde.api.ConfigUtil;
import com.github.bordertech.lde.api.LdeProvider;
import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.List;
import javax.servlet.ServletException;
import org.apache.catalina.Context;
import org.apache.catalina.LifecycleException;
import org.apache.catalina.LifecycleState;
import org.apache.catalina.WebResourceRoot;
import org.apache.catalina.core.StandardContext;
import org.apache.catalina.session.StandardManager;
import org.apache.catalina.startup.Tomcat;
import org.apache.catalina.webresources.DirResourceSet;
import org.apache.catalina.webresources.StandardRoot;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.tomcat.util.scan.Constants;

/**
 * Start Tomcat Server allowing for Servlet 3 config.
 * <p>
 * Simulate a WAR structure by defining a class directory and a lib directory.
 * </p>
 * <p>
 * If using surefire, for tomcat to do its jar scanning correctly the <code>useSystemClassLoader</code> property needs to be set to
 * <code>false</code>.
 * </p>
 */
public class TomcatLauncherProvider implements LdeProvider {

	private static final Log LOG = LogFactory.getLog(TomcatLauncherProvider.class);

	private Tomcat tomcat = null;

	@Override
	public void launchServer() {
		launchServer(true);
	}

	@Override
	public void launchServer(final boolean block) {
		try {
			// Check if already exists
			if (isTomcatCreated()) {
				throw new IllegalStateException("TOMCAT is already created.");
			}
			// Create tomcat
			Tomcat tom = createTomcatInstance();
			setTomcat(tom);
			// Config tomcat
			configTomcat(tom);
			// Start tomcat
			LOG.info("Starting TOMCAT.");
			tom.start();
			LOG.info("Started TOMCAT.");

			// Wait till started
			waitForTomcatToStart();
			if (block) {
				tom.getServer().await();
			}
		} catch (IOException | IllegalStateException | ServletException | LifecycleException e) {
			LOG.error("Could not start LDE TOMCAT server. " + e.getMessage(), e);
			setTomcat(null);
		}
	}

	@Override
	public void stopServer() {
		// Check created before try and stop
		if (!isTomcatCreated()) {
			return;
		}

		Tomcat tom = getTomcat();
		try {
			// Stop server
			tom.stop();
			LOG.info("Stopped TOMCAT.");

			// Wait till server stopped
			waitForTomcatToStop();

			tom.destroy();
		} catch (LifecycleException e) {
			LOG.error("Could not STOP LDE TOMCAT server. " + e.getMessage(), e);
		} finally {
			setTomcat(null);
		}
	}

	@Override
	public int getPort() {
		return isRunning() ? getTomcat().getConnector().getPort() : -1;
	}

	/**
	 * @return the base URL (with context)
	 */
	@Override
	public String getBaseUrl() {
		// Check is running
		if (!isRunning()) {
			return null;
		}
		// Get parts of URL
		Tomcat tom = getTomcat();
		String host = "localhost";
		String scheme = tom.getConnector().getScheme();
		int port = tom.getConnector().getPort();
		String contextPath = TomcatConfigUtil.getContextPath();
		// Build URL
		return scheme + "://" + host + ":" + port + contextPath;
	}

	@Override
	public boolean isRunning() {
		return isTomcatCreated() && getTomcat().getServer().getState() == LifecycleState.STARTED;
	}

	/**
	 * @return the tomcat instance or null if not running
	 */
	public Tomcat getTomcat() {
		return tomcat;
	}

	/**
	 * @return true if tomcat instance created
	 */
	protected boolean isTomcatCreated() {
		return getTomcat() != null;
	}

	/**
	 * Wait for TOMCAT to start.
	 *
	 * @throws LifecycleException life cycle exception occurred
	 */
	protected void waitForTomcatToStart() throws LifecycleException {
		// Check tomcat is created
		if (!isTomcatCreated()) {
			throw new IllegalStateException("Tomcat instance not created before checking if started.");
		}

		int i = 0;
		while (!isRunning()) {
			waitInterval();
			if (i++ > 10) {
				getTomcat().stop();
				throw new IllegalStateException("Timeout waiting for TOMCAT to start");
			}
		}
	}

	/**
	 * Wait for TOMCAT to stop.
	 *
	 * @throws LifecycleException life cycle exception occurred
	 */
	protected void waitForTomcatToStop() throws LifecycleException {
		// Check is created before try to stop
		if (!isTomcatCreated()) {
			return;
		}

		int i = 0;
		while (getTomcat().getServer().getState() != LifecycleState.STOPPED) {
			waitInterval();
			if (i++ > 10) {
				throw new IllegalStateException("Timeout waiting for TOMCAT to stop");
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
			throw new IllegalStateException("Waiting for TOMCAT status interrupted. " + ex.getMessage(), ex);
		}
	}

	/**
	 * Configure tomcat.
	 *
	 * @param tom the tomcat instance to configure
	 *
	 * @throws IOException an IO Exception
	 * @throws ServletException a Servlet Exception
	 */
	protected void configTomcat(final Tomcat tom) throws IOException, ServletException {
		LOG.info("Configure TOMCAT.");
		final int port = findServerPort();
		final String baseDir = TomcatConfigUtil.getBaseDir();
		tom.setPort(port);
		tom.setBaseDir(baseDir);
		// Create context
		Context context = addWebAppContext(tom);
		configWebApp(context);
	}

	/**
	 * @param tomcat the tomcat instance
	 */
	protected void setTomcat(final Tomcat tomcat) {
		this.tomcat = tomcat;
	}

	/**
	 * Create a tomcat instance.
	 *
	 * @return a new tomcat instance
	 */
	protected Tomcat createTomcatInstance() {
		return new Tomcat();
	}

	/**
	 * Add the web app context.
	 *
	 * @param tom the tomcat instance to add web app
	 * @return the web app context
	 */
	protected Context addWebAppContext(final Tomcat tom) {
		final String path = TomcatConfigUtil.getContextPath();
		final String webAppDir = TomcatConfigUtil.getWebAppDir();
		return tom.addWebapp(path, webAppDir);
	}

	/**
	 * Configure the web app context.
	 *
	 * @param context the web app context to configure
	 * @throws IOException an IO Exception
	 * @throws ServletException a Servlet Exception
	 */
	protected void configWebApp(final Context context) throws IOException, ServletException {
		final String libDir = TomcatConfigUtil.getLibDir();
		final String classesDir = TomcatConfigUtil.getClassesDir();

		WebResourceRoot resources = new StandardRoot(context);
		context.setResources(resources);
		// Declare an alternative location for the "WEB-INF/lib" dir
		if (libDir != null && !libDir.isEmpty()) {
			resources.addPreResources(new DirResourceSet(resources, Constants.WEB_INF_LIB, libDir, "/"));
		}
		// Declare an alternative location for the "WEB-INF/classes" dir
		if (classesDir != null && !classesDir.isEmpty()) {
			resources.addPreResources(new DirResourceSet(resources, Constants.WEB_INF_CLASSES, classesDir, "/"));
		}

		// Stop persistent sessions
		StandardManager mgr = new StandardManager();
		mgr.setPathname(null);
		context.setManager(mgr);

		// Delay for requests to stop processing in milliseconds
		if (context instanceof StandardContext) {
			((StandardContext) context).setUnloadDelay(10000);
		}

		if (TomcatConfigUtil.isCustomClassLoaderEnabled()) {
			configCustomClassLoader(context);
		}
		if (TomcatConfigUtil.isCustomJarScannerEnabled()) {
			configCustomJarScanner(context);
		}

	}

	/**
	 * Configure the WebApp context class loader.
	 * <p>
	 * Each WebApp context has its own class loader to isolate the class paths. However, the tomcat StandardJarScanner only scans classes it thinks is
	 * a web application dependency. Therefore we need to put the project classes into a custom class loader so they are scanned.
	 * </p>
	 *
	 * @param context the context to configure
	 */
	protected void configCustomClassLoader(final Context context) {
		// Put all the classpath URLS into a new ClassLoader so the StandardJarScanner will scan them as a potential webapp library.
		// This is needed for Tomcat to find the Servlet 3 annotations when setting up the webapp.
		// The "isWebApp" logic in the tomcat jar scanner is not so good.
		ClassLoader loader = TomcatLauncherProvider.class.getClassLoader();
		ClassLoader wrapper = new URLClassLoader(retrieveClassLoaderUrls(loader), loader);
		context.setParentClassLoader(wrapper);
	}

	/**
	 * Configure a custom jar scanner.
	 *
	 * @param context the context to configure
	 */
	protected void configCustomJarScanner(final Context context) {
		// Check if a custom jar scanner has bee defined
		if (Didums.hasService(CustomJarScanner.class)) {
			context.setJarScanner(Didums.getService(CustomJarScanner.class));
		}
	}

	/**
	 * Retrieve all the URLS in the class loader tree.
	 *
	 * @param current the current class loader
	 * @return the URLS of the class loader tree
	 */
	protected URL[] retrieveClassLoaderUrls(final ClassLoader current) {
		List<URL> urls = new ArrayList<>();
		ClassLoader loader = current;
		while (loader != null) {
			if (loader instanceof URLClassLoader) {
				for (URL url : ((URLClassLoader) loader).getURLs()) {
					if (!urls.contains(url)) {
						urls.add(url);
					}
				}
			}
			loader = loader.getParent();
		}
		return urls.toArray(new URL[]{});
	}

	/**
	 * Find the port to start tomcat with.
	 *
	 * @return the port to start tomcat with
	 */
	protected int findServerPort() {
		return ConfigUtil.isFindPort() ? TomcatConfigUtil.findFreePort() : ConfigUtil.getDefaultPort();
	}

}
