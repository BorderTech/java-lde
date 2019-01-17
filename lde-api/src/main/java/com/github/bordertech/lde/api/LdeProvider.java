package com.github.bordertech.lde.api;

/**
 * LDE Provider.
 *
 * @param <T> the server type
 */
public interface LdeProvider<T> {

	/**
	 * Launch the server.
	 */
	void launchServer();

	/**
	 * @param block true if block thread on starting server
	 */
	void launchServer(final boolean block);

	/**
	 * Stop server.
	 */
	void stopServer();

	/**
	 * @return the port being used by web server
	 */
	int getPort();

	/**
	 * @return the web server instance
	 */
	T getServer();

	/**
	 * @return true if find a free port to start web server
	 */
	boolean isFindPort();

}
