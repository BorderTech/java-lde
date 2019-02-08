package com.github.bordertech.lde.tomcat;

import com.github.bordertech.config.Config;
import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Deque;
import java.util.LinkedList;
import java.util.Set;
import javax.servlet.ServletContext;
import org.apache.tomcat.JarScanType;
import org.apache.tomcat.JarScannerCallback;
import org.apache.tomcat.util.scan.StandardJarScanner;

/**
 * Include the maven target classes and named dependencies when scanning the class path.
 * <p>
 * This is needed to pick up the classes on the dependant maven project modules as they are above the "webapp" class loader and their annotations are
 * not scanned.
 * </p>
 */
public class MavenCustomJarScanner extends StandardJarScanner implements CustomJarScanner {

	private static final boolean MAVEN_PATHS_ENABLED = Config.getInstance().getBoolean("lde.tomcat.scanner.paths.maven.enabled", true);
	private static final boolean NAMED_PATHS_ENABLED = Config.getInstance().getBoolean("lde.tomcat.scanner.paths.named.enabled", true);
	private static final String[] NAMED_ENTRIES = Config.getInstance().getStringArray("lde.tomcat.scanner.paths.named.includes");

	/**
	 * Default constructor.
	 */
	public MavenCustomJarScanner() {
		setScanManifest(false);
		setScanAllFiles(true);
	}

	@Override
	protected void doScanClassPath(final JarScanType scanType, final ServletContext context, final JarScannerCallback callback,
			final Set<URL> processedURLs) {
		// Process maven class paths as WebApp
		if (MAVEN_PATHS_ENABLED || NAMED_PATHS_ENABLED) {
			processURLs(scanType, callback, processedURLs, true, getMavenClassPaths());
		}
		// Process classpath
		super.doScanClassPath(scanType, context, callback, processedURLs);
	}

	/**
	 * @return the maven dependency class paths
	 */
	protected Deque<URL> getMavenClassPaths() {

		Deque<URL> urls = new LinkedList<>();

		String classPath = System.getProperty("java.class.path");
		if (classPath == null || classPath.length() == 0) {
			return urls;
		}

		String[] classPathEntries = classPath.split(File.pathSeparator);
		for (String classPathEntry : classPathEntries) {
			if ((MAVEN_PATHS_ENABLED && isMavenEntry(classPathEntry))
					|| (NAMED_PATHS_ENABLED && isNamedEntry(classPathEntry))) {
				// Convert to URL
				File f = new File(classPathEntry);
				try {
					urls.add(f.toURI().toURL());
				} catch (MalformedURLException e) {
					// TODO Need to hook into Tomcat logging
					System.out.println("Bad classpath entry: " + classPathEntry);
				}
			}
		}
		return urls;
	}

	/**
	 * Check if entry is a maven classpath.
	 *
	 * @param classPathEntry the class path entry to check
	 * @return true if a maven project dependency
	 */
	protected boolean isMavenEntry(final String classPathEntry) {
		// Maven classpath entries
		return classPathEntry.endsWith("classes") || classPathEntry.contains("SNAPSHOT");
	}

	/**
	 * Check if entry is a named entry.
	 *
	 * @param classPathEntry the class path entry to check
	 * @return true if a named entry
	 */
	protected boolean isNamedEntry(final String classPathEntry) {
		if (NAMED_ENTRIES == null) {
			return false;
		}
		for (String name : NAMED_ENTRIES) {
			if (classPathEntry.contains(name)) {
				return true;
			}
		}
		return false;
	}

}
