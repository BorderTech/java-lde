package com.github.bordertech.lde.tomcat;

import com.github.bordertech.config.Config;
import com.github.bordertech.didums.Didums;
import com.github.bordertech.lde.api.LdeProvider;
import java.io.File;
import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
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
	private static final String BASE_DIR = Config.getInstance().getString("lde.tomcat.base.dir", "/target/tomcat");
	private static final String WEBAPP_DIR = Config.getInstance().getString("lde.tomcat.webapp.dir", "/target/webapp");
	private static final String WEBAPP_CLASSES_DIR = Config.getInstance().getString("lde.tomcat.webapp.classes.dir");
	private static final String WEBAPP_LIB_DIR = Config.getInstance().getString("lde.tomcat.webapp.lib.dir", "/target/dependency");
	private static final int DEFAULT_PORT = Config.getInstance().getInt("lde.tomcat.port", 8080);
	private static final boolean FIND_PORT = Config.getInstance().getBoolean("lde.tomcat.port.find", false);
	private static final String CONTEXT_PATH = Config.getInstance().getString("lde.tomcat.context.path", "/lde");
	private static final boolean CUSTOM_CLASSLOADER_ENABLED = Config.getInstance().getBoolean("lde.tomcat.custom.classloader.enabled", true);
	private static final boolean CUSTOM_JARSCANNER_ENABLED = Config.getInstance().getBoolean("lde.tomcat.custom.jarscanner.enabled", true);

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

	@Override
	public boolean isFindPort() {
		return FIND_PORT;
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
		String contextPath = getContextPath();
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
		final int port = isFindPort() ? findFreePort() : getDefaultPort();
		final String baseDir = getBaseDir();
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
		final String path = getContextPath();
		final String webAppDir = getWebAppDir();
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
		final String libDir = getLibDir();
		final String classesDir = getClassesDir();

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

		if (isCustomClassLoaderEnabled()) {
			configCustomClassLoader(context);
		}
		if (isCustomJarScannerEnabled()) {
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
	 * @return true if use custom class loader
	 */
	protected boolean isCustomClassLoaderEnabled() {
		return CUSTOM_CLASSLOADER_ENABLED;
	}

	/**
	 * @return true if use custom jar scanner
	 */
	protected boolean isCustomJarScannerEnabled() {
		return CUSTOM_JARSCANNER_ENABLED;
	}

	/**
	 * @return the default PORT for TOMCAT to listen on
	 */
	protected int getDefaultPort() {
		return DEFAULT_PORT;
	}

	/**
	 * The WebApp context where "" is root.
	 *
	 * @return the webapp context path
	 */
	protected String getContextPath() {
		return CONTEXT_PATH;
	}

	/**
	 * The Tomcat base folder on which all others will be derived.
	 * <p>
	 * The directory to install tomcat relative to work directory.
	 * </p>
	 *
	 * @return the directory for TOMCAT to be installed.
	 */
	protected String getBaseDir() {
		return prefixUserDir(BASE_DIR);
	}

	/**
	 * WebApp directory for the context that can be used for static files.
	 * <p>
	 * The webapp directory (ie static resources) relative to work directory.
	 * </p>
	 *
	 * @return the webapp directory
	 */
	protected String getWebAppDir() {
		return prefixUserDir(WEBAPP_DIR);
	}

	/**
	 * Declare an alternative location for the "WEB-INF/lib" dir.
	 * <p>
	 * The webapp lib directory relative to work directory (eg /target/dependency).
	 * </p>
	 *
	 * @return the lib directory
	 */
	protected String getLibDir() {
		return prefixUserDir(WEBAPP_LIB_DIR);
	}

	/**
	 * Declare an alternative location for the "WEB-INF/classes" dir.
	 *
	 * @return the classes directory
	 */
	protected String getClassesDir() {
		return prefixUserDir(WEBAPP_CLASSES_DIR);
	}

	/**
	 * Prefix a directory with the system user directory.
	 *
	 * @param dir the directory to append the working directory
	 * @return the dir with the working directory prefix
	 */
	protected String prefixUserDir(final String dir) {
		if (dir == null) {
			return null;
		}
		String sep = dir.startsWith("/") ? "" : "/";
		String path = System.getProperty("user.dir") + sep + dir;
		if (dir.startsWith("/target")) {
			File file = new File(path);
			if (!file.exists()) {
				try {
					file.mkdir();
				} catch (Exception e) {
					throw new IllegalStateException("Unable to create user directorty [" + path + "]. " + e.getMessage(), e);
				}
			}
		}
		return path;
	}

	/**
	 * Find a free port to start tomcat with.
	 *
	 * @return the next free port starting with the default port
	 */
	protected int findFreePort() {
		int start = DEFAULT_PORT;
		LOG.info("Finding a free port for TOMCAT starting at " + start + ".");
		int tries = 0;
		while (!isTcpPortAvailable(start)) {
			if (tries++ > 100) {
				throw new IllegalStateException("Unable to find a free port to start TOMCAT.");
			}
			start++;
		}
		LOG.info("Found port " + start + " to start TOMCAT.");
		return start;
	}

	/**
	 * Check if this port is available.
	 *
	 * @param port the port to check is available
	 * @return true if port is available
	 */
	protected boolean isTcpPortAvailable(final int port) {
		try (ServerSocket serverSocket = new ServerSocket()) {
			serverSocket.setReuseAddress(false);
			serverSocket.bind(new InetSocketAddress(InetAddress.getByName("localhost"), port), 1);
			return true;
		} catch (IOException e) {
			return false;
		}
	}

}
