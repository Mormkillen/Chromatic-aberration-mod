package com.mormkillen.chromaticaberration.client;

import com.mormkillen.chromaticaberration.ChromaticAberrationClient;
import net.fabricmc.loader.api.FabricLoader;

import java.lang.reflect.Method;

/**
 * Detects an active Iris shader pack via Iris's official API, using reflection
 * so this mod never hard-depends on Iris. When a shader pack is in use we
 * yield the screen: the shader pack owns the final image (including its own
 * chromatic aberration), and running our post chain on top would double up /
 * fight its output.
 */
public final class IrisCompat {
	private static Boolean irisChecked = null;
	private static Object irisApi = null;
	private static Method isShaderPackInUse = null;
	private static boolean lastLoggedState = false;

	private IrisCompat() {
	}

	/** True when Iris is installed AND a shader pack is currently in use. */
	public static boolean isShaderPackActive() {
		try {
			if (irisChecked == null) {
				irisChecked = FabricLoader.getInstance().isModLoaded("iris");
				if (irisChecked) {
					Class<?> apiClass = Class.forName("net.irisshaders.iris.api.v0.IrisApi");
					irisApi = apiClass.getMethod("getInstance").invoke(null);
					isShaderPackInUse = apiClass.getMethod("isShaderPackInUse");
				}
			}

			if (!irisChecked || irisApi == null || isShaderPackInUse == null) {
				return false;
			}

			boolean inUse = (Boolean) isShaderPackInUse.invoke(irisApi);
			if (inUse != lastLoggedState) {
				lastLoggedState = inUse;
				if (inUse) {
					ChromaticAberrationClient.LOGGER.info("Iris shader pack detected; yielding post processing to the shader pack");
				} else {
					ChromaticAberrationClient.LOGGER.info("Shader pack disabled; resuming chromatic aberration");
				}
			}
			return inUse;
		} catch (Throwable t) {
			// Any reflection problem must never break rendering.
			return false;
		}
	}
}
