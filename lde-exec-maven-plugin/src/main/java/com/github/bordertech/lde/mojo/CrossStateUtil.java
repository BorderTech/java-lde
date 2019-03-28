package com.github.bordertech.lde.mojo;

import com.github.bordertech.lde.api.LdeProvider;
import java.util.HashMap;
import java.util.Map;

/**
 * Save LDE Provider across Maven Lifecycle States.
 */
public final class CrossStateUtil {

	/**
	 * Private constructor.
	 */
	private CrossStateUtil() {
	}

	private static final Map<String, LdeProvider> PROVIDERS = new HashMap<>();

	/**
	 * Retrieve the provider for this id.
	 *
	 * @param id the provider id
	 * @return the provider for this id or null if does not exist
	 */
	public static LdeProvider getProvider(final String id) {
		return PROVIDERS.get(id);
	}

	/**
	 * Save the provider for this id.
	 *
	 * @param id the provider id
	 * @param provider the provider to save
	 */
	public static void addProvider(final String id, final LdeProvider provider) {
		PROVIDERS.put(id, provider);
	}

	/**
	 * Remove the provider for this id.
	 *
	 * @param id the provider id to remove
	 * @return the provider for this id or null if does not exist
	 */
	public static LdeProvider removeProvider(final String id) {
		return PROVIDERS.remove(id);
	}

}
