package com.github.bordertech.lde.api;

/**
 * LDE server provider (eg embedded tomcat).
 */
public interface LdeProvider {

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
	 * @return true if find a free port to start web server
	 */
	boolean isFindPort();

	/**
	 * @return the base URL (with context)
	 */
	String getBaseUrl();

	/**
	 * @return true if server is running
	 */
	boolean isRunning();

}
