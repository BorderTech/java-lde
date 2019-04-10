package com.github.bordertech.lde.api;

import com.github.bordertech.config.Config;
import java.nio.file.Path;
import java.nio.file.Paths;
import org.apache.commons.lang.StringUtils;

/**
 * LDE server provider (eg embedded tomcat).
 */
public interface LdeProvider {

	/**
	 * Working directory parameter key.
	 */
	String PARAM_WORKING_DIRECTORY_KEY = "bordertech.lde.working.dir";

	/**
	 * LDE server default port parameter key.
	 */
	String PARAM_PORT_DEFAULT_KEY = "bordertech.lde.port.default";

	/**
	 * LDE find available port parameter key.
	 */
	String PARAM_PORT_FIND_KEY = "bordertech.lde.port.find.enabled";

	/**
	 * Launch the server.
	 */
	void launchServer();

	/**
	 * @param block true if block thread on starting server
	 */
	void launchServer(boolean block);

	/**
	 * Stop server.
	 */
	void stopServer();

	/**
	 * @return the port being used by web server
	 */
	int getPort();

	/**
	 * @return the base URL (with context)
	 */
	String getBaseUrl();

	/**
	 * @return true if server is running
	 */
	boolean isRunning();

	/**
	 * @return the default port for the provider
	 */
	default int getDefaultPort() {
		return Config.getInstance().getInteger(PARAM_PORT_DEFAULT_KEY, 8080);
	}

	/**
	 * @return true if find a free port to start provider
	 */
	default boolean isFindPort() {
		return Config.getInstance().getBoolean(PARAM_PORT_FIND_KEY, false);
	}

	/**
	 * @return the working directory for the provider
	 */
	default Path getWorkingDirectory() {

		// Check custom system property (usually set by MOJO)
		String file = System.getProperty(PARAM_WORKING_DIRECTORY_KEY);

		// Check user properties
		if (StringUtils.isBlank(file)) {
			file = Config.getInstance().getString(PARAM_WORKING_DIRECTORY_KEY);
		}

		// Fall back to JVM user directory
		if (StringUtils.isBlank(file)) {
			file = System.getProperty("user.dir");
		}

		// Convert to path
		return Paths.get(file);
	}

}
