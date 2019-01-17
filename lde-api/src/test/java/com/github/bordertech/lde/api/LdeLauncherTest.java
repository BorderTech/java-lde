package com.github.bordertech.lde.api;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

/**
 * Unit tests for {@link LdeLauncher}.
 */
public class LdeLauncherTest {

	@Before
	public void resetMockBefore() {
		getMockProvider().resetMock();
	}

	@After
	public void resetMockAfter() {
		getMockProvider().resetMock();
	}

	@Test
	public void testStart() {
		MockProvider mock = (MockProvider) LdeLauncher.getProvider();
		Assert.assertFalse("Server default status should be not started", mock.isStarted());
		Assert.assertFalse("Server default status should be not blocked", mock.isBlocked());
		LdeLauncher.launchServer();
		Assert.assertTrue("Server should be started", mock.isStarted());
		Assert.assertTrue("Server status should be blocked", mock.isBlocked());
	}

	@Test
	public void testStartBlocked() {
		MockProvider mock = (MockProvider) LdeLauncher.getProvider();
		Assert.assertFalse("Server default status should be not started", mock.isStarted());
		Assert.assertFalse("Server default status should be not blocked", mock.isBlocked());
		LdeLauncher.launchServer();
		Assert.assertTrue("Server should be started", mock.isStarted());
		Assert.assertTrue("Server status should be blocked", mock.isBlocked());
	}

	@Test
	public void testStartNotBlocked() {
		MockProvider mock = (MockProvider) LdeLauncher.getProvider();
		Assert.assertFalse("Server default status should be not started", mock.isStarted());
		Assert.assertFalse("Server default status should be not blocked", mock.isBlocked());
		LdeLauncher.launchServer(false);
		Assert.assertTrue("Server should be started", mock.isStarted());
		Assert.assertFalse("Server status should not be blocked", mock.isBlocked());
	}

	protected MockProvider getMockProvider() {
		return (MockProvider) LdeLauncher.getProvider();
	}

}
