package com.github.bordertech.lde.tomcat;

import com.github.bordertech.config.Config;
import com.github.bordertech.lde.api.ConfigUtil;
import java.io.File;
import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.nio.file.Path;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Tomcat provider configuration helper.
 */
public final class TomcatConfigUtil {

	private static final Log LOG = LogFactory.getLog(TomcatConfigUtil.class);
	private static final String BASE_DIR = Config.getInstance().getString("lde.tomcat.base.dir", "target/tomcat");
	private static final String WEBAPP_DIR = Config.getInstance().getString("lde.tomcat.webapp.dir", "target/webapp");
	private static final String WEBAPP_CLASSES_DIR = Config.getInstance().getString("lde.tomcat.webapp.classes.dir");
	private static final String WEBAPP_LIB_DIR = Config.getInstance().getString("lde.tomcat.webapp.lib.dir", "target/dependency");
	private static final String CONTEXT_PATH = Config.getInstance().getString("lde.tomcat.context.path", "/lde");
	private static final boolean CUSTOM_CLASSLOADER_ENABLED = Config.getInstance().getBoolean("lde.tomcat.custom.classloader.enabled", true);
	private static final boolean CUSTOM_JARSCANNER_ENABLED = Config.getInstance().getBoolean("lde.tomcat.custom.jarscanner.enabled", true);

	/**
	 * Private constructor.
	 */
	private TomcatConfigUtil() {
	}

	/**
	 * @return true if use custom class loader
	 */
	public static boolean isCustomClassLoaderEnabled() {
		return CUSTOM_CLASSLOADER_ENABLED;
	}

	/**
	 * @return true if use custom jar scanner
	 */
	public static boolean isCustomJarScannerEnabled() {
		return CUSTOM_JARSCANNER_ENABLED;
	}

	/**
	 * The WebApp context where "" is root.
	 *
	 * @return the webapp context path
	 */
	public static String getContextPath() {
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
	public static String getBaseDir() {
		return prefixWorkingDir(BASE_DIR);
	}

	/**
	 * WebApp directory for the context that can be used for static files.
	 * <p>
	 * The webapp directory (ie static resources) relative to work directory.
	 * </p>
	 *
	 * @return the webapp directory
	 */
	public static String getWebAppDir() {
		return prefixWorkingDir(WEBAPP_DIR);
	}

	/**
	 * Declare an alternative location for the "WEB-INF/lib" dir.
	 * <p>
	 * The webapp lib directory relative to work directory (eg /target/dependency).
	 * </p>
	 *
	 * @return the lib directory
	 */
	public static String getLibDir() {
		return prefixWorkingDir(WEBAPP_LIB_DIR);
	}

	/**
	 * Declare an alternative location for the "WEB-INF/classes" dir.
	 *
	 * @return the classes directory
	 */
	public static String getClassesDir() {
		return prefixWorkingDir(WEBAPP_CLASSES_DIR);
	}

	/**
	 * Prefix a directory with the working directory.
	 *
	 * @param dir the directory to append the working directory
	 * @return the dir with the working directory prefix
	 */
	public static String prefixWorkingDir(final String dir) {
		if (dir == null) {
			return null;
		}
		Path path = ConfigUtil.getWorkingDirectory().resolve(dir);
		checkPath(path);
		return path.toString();
	}

	/**
	 * Check the path exists or create it.
	 *
	 * @param path the directory to check exists
	 */
	public static void checkPath(final Path path) {

		File file = path.toFile();
		if (!file.exists() && !file.mkdirs()) {
			throw new IllegalStateException("Cannot create path [" + path + "]");
		}

		if (!file.isDirectory()) {
			throw new IllegalStateException(
					"Path [" + path + "] exists and is not a directory");
		}
	}

	/**
	 * Find a free port to start tomcat with.
	 *
	 * @return the next free port starting with the default port
	 */
	public static int findFreePort() {
		int start = ConfigUtil.getDefaultPort();
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
	public static boolean isTcpPortAvailable(final int port) {
		try (ServerSocket serverSocket = new ServerSocket()) {
			serverSocket.setReuseAddress(false);
			serverSocket.bind(new InetSocketAddress(InetAddress.getByName("localhost"), port), 1);
			return true;
		} catch (IOException e) {
			return false;
		}
	}

}
