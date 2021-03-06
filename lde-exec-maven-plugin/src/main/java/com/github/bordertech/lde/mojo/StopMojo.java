package com.github.bordertech.lde.mojo;

import com.github.bordertech.lde.api.LdeProvider;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;

/**
 * Stop LDE provider.
 */
@Mojo(name = "stop", defaultPhase = LifecyclePhase.POST_INTEGRATION_TEST)
public class StopMojo extends AbstractIdMojo {

	@Override
	public void execute() throws MojoExecutionException, MojoFailureException {
		LdeProvider providerId = CrossStateUtil.removeProvider(getProviderId());
		if (providerId != null) {
			providerId.stopServer();
		}
	}

}
