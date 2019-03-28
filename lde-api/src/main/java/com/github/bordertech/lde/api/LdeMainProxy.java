package com.github.bordertech.lde.api;

/**
 * LDE Launcher Proxy to help IDEs find a main method and launch LDE.
 */
public final class LdeMainProxy {

	/**
	 * Private constructor.
	 */
	private LdeMainProxy() {
	}

	/**
	 * Launch LDE.
	 *
	 * @param args main arguments
	 */
	public static void main(final String[] args) {
		LdeLauncher.launchServer();
	}

}
