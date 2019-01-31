package com.github.bordertech.lde.tomcat;

import com.github.bordertech.config.Config;
import com.github.bordertech.lde.api.LdeProvider;
import java.io.File;
import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
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
import org.apache.tomcat.JarScanner;
import org.apache.tomcat.util.scan.Constants;
import org.apache.tomcat.util.scan.StandardJarScanner;

/**
 * Start Tomcat Server allowing for Servlet 3 config.
 * <p>
 * Simulate a WAR structure by defining a class directory and a lib directory.
 * </p>
 */
public class TomcatLauncherProvider implements LdeProvider<Tomcat> {

	private static final Log LOG = LogFactory.getLog(TomcatLauncherProvider.class);

	/**
	 * The directory to install tomcat relative to work directory.
	 */
	private static final String BASE_DIR = Config.getInstance().getString("lde.tomcat.base.dir", "/target/tomcat");

	/**
	 * Append the maven target classes as a webapp class path.
	 */
	private static final boolean MAVEN_PATHS = Config.getInstance().getBoolean("lde.tomcat.maven.paths", true);

	/**
	 * The webapp directory (ie static resources) relative to work directory.
	 */
	private static final String WEBAPP_DIR = Config.getInstance().getString("lde.tomcat.webapp.dir", "/target/webapp");

	/**
	 * The webapp classes directory relative to work directory (eg /target/classes).
	 */
	private static final String WEBAPP_CLASSES_DIR = Config.getInstance().getString("lde.tomcat.webapp.classes.dir");

	/**
	 * The webapp lib directory relative to work directory (eg /target/dependency).
	 */
	private static final String WEBAPP_LIB_DIR = Config.getInstance().getString("lde.tomcat.webapp.lib.dir", "/target/dependency");

	private static final int DEFAULT_PORT = Config.getInstance().getInt("lde.tomcat.port", 8080);

	private static final boolean FIND_PORT = Config.getInstance().getBoolean("lde.tomcat.port.find", false);

	private static final String CONTEXT_PATH = Config.getInstance().getString("lde.tomcat.context.path", "/lde");

	private Tomcat tomcat = null;

	@Override
	public void launchServer() {
		launchServer(true);
	}

	@Override
	public void launchServer(final boolean block) {
		try {
			// Config tomcat
			if (tomcat != null) {
				throw new IllegalStateException("TOMCAT already exists.");
			}
			tomcat = new Tomcat();
			configTomcat();

			tomcat.start();
			LOG.info("Started TOMCAT.");

			// Wait till started
			waitForTomcatToStart();
			if (block) {
				tomcat.getServer().await();
			}
		} catch (IOException | IllegalStateException | ServletException | LifecycleException e) {
			LOG.error("Could not start LDE TOMCAT server. " + e.getMessage(), e);
		}
	}

	@Override
	public void stopServer() {
		if (tomcat == null) {
			return;
		}

		try {
			// Stop server
			tomcat.stop();
			LOG.info("Stopped TOMCAT.");

			// Wait till server stopped
			waitForTomcatToStop();

			tomcat.destroy();
			tomcat = null;
		} catch (LifecycleException e) {
			LOG.error("Could not STOP LDE TOMCAT server. " + e.getMessage(), e);
		}
	}

	@Override
	public Tomcat getServer() {
		return tomcat;
	}

	@Override
	public int getPort() {
		if (tomcat == null) {
			return -1;
		}
		return tomcat.getConnector().getPort();
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
		Tomcat tom = getServer();
		if (tom == null) {
			return null;
		}
		// Get parts of URL
		String host = "localhost";
		String scheme = tom.getConnector().getScheme();
		int port = tom.getConnector().getPort();
		String contextPath = getContextPath();
		// Build URL
		return scheme + "://" + host + ":" + port + contextPath;
	}

	@Override
	public boolean isRunning() {
		return getServer() != null;
	}

	/**
	 * Wait for TOMCAT to start.
	 *
	 * @throws LifecycleException life cycle exception occurred
	 */
	protected void waitForTomcatToStart() throws LifecycleException {
		int i = 0;
		while (tomcat.getServer().getState() != LifecycleState.STARTED) {
			waitInterval();
			if (i++ > 10) {
				tomcat.stop();
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
		int i = 0;
		while (tomcat.getServer().getState() != LifecycleState.STOPPED) {
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
	 * @throws IOException an IO Exception
	 * @throws ServletException a Servlet Exception
	 */
	protected void configTomcat() throws IOException, ServletException {

		LOG.info("Configure TOMCAT.");

		final int port = isFindPort() ? findFreePort() : getDefaultPort();
		final String path = getContextPath();
		final String baseDir = getBaseDir();
		final String webAppDir = getWebAppDir();

		tomcat.setPort(port);
		tomcat.setBaseDir(baseDir);

		Context context = tomcat.addWebapp(path, webAppDir);
		configWebApp(context);
	}

	/**
	 * @param context the context to configure
	 * @throws IOException an IO Exception
	 * @throws ServletException a Servlet Exception
	 */
	protected void configWebApp(final Context context) throws IOException, ServletException {
		final String libDir = getLibDir();
		final String classesDir = getClassesDir();

		configJarScanner(context);

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

		// Delay for requets to stop processing in milliseconds
		((StandardContext) context).setUnloadDelay(10000);
	}

	/**
	 * @return the default PORT for TOMCAT to listen on
	 */
	protected int getDefaultPort() {
		return DEFAULT_PORT;
	}

	/**
	 * @return the webapp context path
	 */
	protected String getContextPath() {
		return CONTEXT_PATH;
	}

	/**
	 * @return the directory for TOMCAT to be installed.
	 */
	protected String getBaseDir() {
		return prefixUserDir(BASE_DIR);
	}

	/**
	 * @return the webapp directory
	 */
	protected String getWebAppDir() {
		return prefixUserDir(WEBAPP_DIR);
	}

	/**
	 * @return the lib directory
	 */
	protected String getLibDir() {
		return prefixUserDir(WEBAPP_LIB_DIR);
	}

	/**
	 * @return the classes directory
	 */
	protected String getClassesDir() {
		return prefixUserDir(WEBAPP_CLASSES_DIR);
	}

	/**
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

	/**
	 * Configure how to scan the jars in the tomcat classpath.
	 *
	 * @param context the server context
	 * @throws IOException an IO Exception
	 * @throws ServletException a servlet exception
	 */
	protected void configJarScanner(final Context context) throws IOException, ServletException {

		// Add Maven target class paths (if any)
		if (MAVEN_PATHS) {
			context.setJarScanner(new MavenStandardJarScanner());
		}

		// Scan for Annotations
		JarScanner scanner = context.getJarScanner();
		if (scanner instanceof StandardJarScanner) {
			StandardJarScanner std = (StandardJarScanner) scanner;
			std.setScanManifest(false);
			std.setScanAllFiles(true);
		}

	}

}
