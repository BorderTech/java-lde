package com.github.bordertech.lde.tomcat;

import org.apache.tomcat.JarScanner;

/**
 * Allow the scanning of jar files on the tomcat class path to be customized.
 * <p>
 * Useful when trying to run embedded tomcat outside a "war" or "webapp" configuration.
 * </p>
 */
public interface CustomJarScanner extends JarScanner {

}
