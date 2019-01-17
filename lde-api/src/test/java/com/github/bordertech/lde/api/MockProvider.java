package com.github.bordertech.lde.api;

/**
 * Mock provider for testing.
 */
public class MockProvider implements LdeProvider {

	private boolean started;
	private boolean blocked;

	public boolean isStarted() {
		return started;
	}

	public boolean isBlocked() {
		return blocked;
	}

	public void resetMock() {
		started = false;
		blocked = false;
	}

	@Override
	public void launchServer() {
		launchServer(true);
	}

	@Override
	public void launchServer(final boolean block) {
		started = true;
		blocked = block;
	}

	@Override
	public void stopServer() {
		started = false;
	}

	@Override
	public int getPort() {
		throw new UnsupportedOperationException("Not supported yet.");
	}

	@Override
	public Object getServer() {
		throw new UnsupportedOperationException("Not supported yet.");
	}

	@Override
	public boolean isFindPort() {
		throw new UnsupportedOperationException("Not supported yet.");
	}

}
