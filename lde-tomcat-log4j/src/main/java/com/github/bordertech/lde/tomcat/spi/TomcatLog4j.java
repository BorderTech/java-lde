package com.github.bordertech.lde.tomcat.spi;

import org.apache.juli.logging.Log;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;

/**
 * LOG4J implementation for tomcat juli logging.
 */
public class TomcatLog4j implements Log {

	private final Logger logger;

	/**
	 * Default constructor.
	 */
	public TomcatLog4j() {
		// this constructor is important, otherwise the ServiceLoader cannot start
		logger = Logger.getLogger(TomcatLog4j.class);
	}

	/**
	 * @param name the logger name
	 */
	public TomcatLog4j(final String name) {
		// this constructor is needed by the LogFactory implementation
		logger = Logger.getLogger(name);
	}

	@Override
	public boolean isFatalEnabled() {
		return logger.isEnabledFor(Level.FATAL);
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
		return logger.isEnabledFor(Level.ERROR);
	}

	@Override
	public boolean isInfoEnabled() {
		return logger.isInfoEnabled();
	}

	@Override
	public boolean isWarnEnabled() {
		return logger.isEnabledFor(Level.WARN);
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
