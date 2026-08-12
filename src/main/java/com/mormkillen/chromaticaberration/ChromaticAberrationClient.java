package com.mormkillen.chromaticaberration;

import com.mormkillen.chromaticaberration.client.AberrationRenderer;
import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.fabricmc.fabric.api.resource.v1.ResourceLoader;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.PackType;
import org.lwjgl.glfw.GLFW;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.CompletableFuture;

public class ChromaticAberrationClient implements ClientModInitializer {
	public static final String MOD_ID = "chromaticaberration";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	private static final AberrationRenderer RENDERER = new AberrationRenderer();

	private static final String[] DEBUG_MODE_NAMES = {
			"0 · normal aberration",
			"1 · passthrough copy",
			"2 · force opaque alpha",
			"3 · visualize framebuffer alpha"
	};

	public static AberrationRenderer renderer() {
		return RENDERER;
	}

	@Override
	public void onInitializeClient() {
		// Recreate the post shader after a resource reload (F3+T),
		// otherwise the old GL program would keep stale shader code.
		ResourceLoader.get(PackType.CLIENT_RESOURCES).registerReloadListener(
				Identifier.fromNamespaceAndPath(MOD_ID, "shader_reload"),
				(sharedState, backgroundExecutor, barrier, gameExecutor) -> {
					RENDERER.onResourceReload();
					return CompletableFuture.completedFuture(null);
				});

		// Debug key: cycle shader debug modes (diagnosing the black-band issue).
		KeyMapping debugKey = KeyMappingHelper.registerKeyMapping(new KeyMapping(
				"key.chromaticaberration.debug_mode",
				InputConstants.Type.KEYSYM,
				GLFW.GLFW_KEY_F8,
				KeyMapping.Category.MISC));

		ClientTickEvents.END_CLIENT_TICK.register(client -> {
			while (debugKey.consumeClick()) {
				int mode = RENDERER.cycleDebugMode();
				showActionbar(client, "Aberration debug mode: " + DEBUG_MODE_NAMES[mode]);
			}
		});

		LOGGER.info("Speed Chromatic Aberration initialized");
	}

	private static void showActionbar(Minecraft client, String message) {
		if (client.player != null) {
			client.player.sendOverlayMessage(Component.literal(message));
		}
	}
}
