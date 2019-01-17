package com.github.bordertech.lde.tomcat.spi;

import org.apache.commons.logging.LogFactory;
import org.apache.juli.logging.Log;

/**
 * Commons Logging implementation for JULI.
 */
public class TomcatCommonsLogging implements Log {

	private final org.apache.commons.logging.Log logger;

	/**
	 * Default constructor.
	 */
	public TomcatCommonsLogging() {
		// this constructor is important, otherwise the ServiceLoader cannot start
		logger = LogFactory.getLog(TomcatCommonsLogging.class);
	}

	/**
	 * @param name the class name
	 */
	public TomcatCommonsLogging(final String name) {
		// this constructor is needed by the LogFactory implementation
		logger = LogFactory.getLog(name);
	}

	@Override
	public boolean isFatalEnabled() {
		return logger.isFatalEnabled();
	}

	@Override
	public boolean isTraceEnabled() {
		return logger.isTraceEnabled();
	}

	@Override
	public void fatal(final Object msg) {
		logger.fatal(msg);
	}

	@Override
	public void fatal(final Object msg, final Throwable throwable) {
		logger.fatal(msg, throwable);
	}

	@Override
	public boolean isDebugEnabled() {
		return logger.isDebugEnabled();
	}

	@Override
	public boolean isErrorEnabled() {
		return logger.isErrorEnabled();
	}

	@Override
	public boolean isInfoEnabled() {
		return logger.isInfoEnabled();
	}

	@Override
	public boolean isWarnEnabled() {
		return logger.isWarnEnabled();
	}

	@Override
	public void trace(final Object message) {
		logger.trace(message);
	}

	@Override
	public void trace(final Object message, final Throwable t) {
		logger.trace(message, t);
	}

	@Override
	public void debug(final Object message) {
		logger.debug(message);
	}

	@Override
	public void debug(final Object message, final Throwable t) {
		logger.debug(message, t);
	}

	@Override
	public void info(final Object message) {
		logger.info(message);
	}

	@Override
	public void info(final Object message, final Throwable t) {
		logger.info(message, t);
	}

	@Override
	public void warn(final Object message) {
		logger.warn(message);
	}

	@Override
	public void warn(final Object message, final Throwable t) {
		logger.warn(message, t);
	}

	@Override
	public void error(final Object message) {
		logger.error(message);
	}

	@Override
	public void error(final Object message, final Throwable t) {
		logger.error(message, t);
	}

}
