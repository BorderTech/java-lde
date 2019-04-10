package com.github.bordertech.lde.api;

import java.nio.file.Path;

/**
 * Provider that wraps the default launcher.
 * <p>
 * This can be used as a provider that points to the default launcher. Useful in the MOJO launcher.
 * </p>
 */
public class LaunchWrapperProvider implements LdeProvider {

	private final LdeProvider backing = LdeLauncher.getProvider();

	@Override
	public void launchServer() {
		backing.launchServer();
	}

	@Override
	public void launchServer(final boolean block) {
		backing.launchServer(block);
	}

	@Override
	public void stopServer() {
		backing.stopServer();
	}

	@Override
	public int getPort() {
		return backing.getPort();
	}

	@Override
	public boolean isFindPort() {
		return backing.isFindPort();
	}

	@Override
	public String getBaseUrl() {
		return backing.getBaseUrl();
	}

	@Override
	public boolean isRunning() {
		return backing.isRunning();
	}

	@Override
	public int getDefaultPort() {
		return backing.getDefaultPort();
	}

	@Override
	public Path getWorkingDirectory() {
		return backing.getWorkingDirectory();
	}

}
