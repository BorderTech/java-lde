package com.github.bordertech.lde.api;

/**
 * Convenience class with a main method to start and stop the server.
 */
public final class LdeMain {

	/**
	 * Start and Stop server.
	 *
	 * @param args main arguments
	 */
	public static void main(final String[] args) {
		boolean stop = args != null && args.length > 0 && args[0].equals("stop");
		if (stop) {
			LdeLauncher.stopServer();
		} else {
			LdeLauncher.launchServer();
		}
	}
}
