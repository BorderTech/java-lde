package com.github.bordertech.lde.api;

import com.github.bordertech.didums.Didums;

/**
 * Launcher Helper Class.
 */
public final class LdeLauncher {

	private static final LdeProvider PROVIDER = Didums.getService(LdeProvider.class);

	/**
	 * Private constructor.
	 */
	private LdeLauncher() {
	}

	/**
	 * Launch Server.
	 */
	public static void launchServer() {
		PROVIDER.launchServer();
	}

	/**
	 * Launch Server and block on start.
	 *
	 * @param blocked true if block on server start
	 */
	public static void launchServer(final boolean blocked) {
		PROVIDER.launchServer(blocked);
	}

	/**
	 * Stop Server.
	 */
	public static void stopServer() {
		PROVIDER.stopServer();
	}

	/**
	 * @return true if server is running
	 */
	public static boolean isRunning() {
		return getProvider().isRunning();
	}

	/**
	 * @return the launcher provider
	 */
	public static LdeProvider getProvider() {
		return PROVIDER;
	}

}
