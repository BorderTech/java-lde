package com.github.bordertech.lde.mojo;

import com.github.bordertech.lde.api.LdeProvider;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;

/**
 * Pass through proxy so the MOJO can access the {@link LdeProvider} API in the provider ClassLoader.
 * <p>
 * As the API class has been loaded in two different class loaders, the MOJO cannot directly interact with the API class loaded in the provider class
 * loader.
 * </p>
 */
public class MojoProviderProxy implements InvocationHandler {

	private final Object target;

	/**
	 * @param target provider implementation
	 */
	public MojoProviderProxy(final Object target) {
		this.target = target;
	}

	@Override
	public Object invoke(final Object proxy, final Method method, final Object[] args) throws Throwable {
		Method implMethod = target.getClass().getMethod(method.getName(), method.getParameterTypes());
		return implMethod.invoke(target, args);
	}

}
