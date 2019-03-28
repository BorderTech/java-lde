package com.github.bordertech.lde.tomcat;

import com.github.bordertech.lde.api.LdeLauncher;

/**
 * Tomcat Launcher Proxy to help IDEs find a main method and launch Tomcat.
 */
public final class TomcatLauncherProxy {

	/**
	 * Private constructor.
	 */
	private TomcatLauncherProxy() {
	}

	/**
	 * Launch Tomcat.
	 *
	 * @param args main arguments
	 */
	public static void main(final String[] args) {
		LdeLauncher.launchServer();
	}

}
