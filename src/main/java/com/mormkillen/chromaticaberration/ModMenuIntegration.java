package com.mormkillen.chromaticaberration;

import com.mormkillen.chromaticaberration.client.AberrationConfigScreen;
import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;

/**
 * Mod Menu integration. Loaded only when Mod Menu is installed (the "modmenu"
 * entrypoint is ignored otherwise), so the mod never hard-depends on it.
 */
public class ModMenuIntegration implements ModMenuApi {
	@Override
	public ConfigScreenFactory<?> getModConfigScreenFactory() {
		return AberrationConfigScreen::new;
	}
}
