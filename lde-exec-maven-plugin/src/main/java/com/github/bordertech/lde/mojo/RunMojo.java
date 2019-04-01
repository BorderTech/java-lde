package com.github.bordertech.lde.mojo;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.ResolutionScope;

/**
 * Run the LDE provider and block when started.
 * <p>
 * Start the LDE provider via <code>mvn lde-exec:run</code>.
 * </p>
 */
@Mojo(name = "run", defaultPhase = LifecyclePhase.PACKAGE, requiresDependencyResolution = ResolutionScope.TEST, requiresDirectInvocation = true)
public class RunMojo extends AbstractStartMojo {

	@Override
	public void execute() throws MojoExecutionException, MojoFailureException {
		createAndStartProvider();
	}

	@Override
	protected boolean isBlock() {
		return true;
	}

}
