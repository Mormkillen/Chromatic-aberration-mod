package com.mormkillen.chromaticaberration;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.resource.ResourceManagerHelper;
import net.fabricmc.fabric.api.resource.SimpleSynchronousResourceReloadListener;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.resource.ResourceManager;
import net.minecraft.resource.ResourceType;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import org.lwjgl.glfw.GLFW;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.mormkillen.chromaticaberration.client.AberrationRenderer;

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
		ResourceManagerHelper.get(ResourceType.CLIENT_RESOURCES).registerReloadListener(new SimpleSynchronousResourceReloadListener() {
			@Override
			public Identifier getFabricId() {
				return new Identifier(MOD_ID, "shader_reload");
			}

			@Override
			public void reload(ResourceManager manager) {
				RENDERER.onResourceReload();
			}
		});

		// Debug key: cycle shader debug modes (diagnosing the black-band issue).
		KeyBinding debugKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
				"key.chromaticaberration.debug_mode",
				InputUtil.Type.KEYSYM,
				GLFW.GLFW_KEY_F8,
				"category.chromaticaberration"));

		ClientTickEvents.END_CLIENT_TICK.register(client -> {
			while (debugKey.wasPressed()) {
				int mode = RENDERER.cycleDebugMode();
				showActionbar(client, "Aberration debug mode: " + DEBUG_MODE_NAMES[mode]);
			}
		});

		LOGGER.info("Speed Chromatic Aberration initialized");
	}

	private static void showActionbar(MinecraftClient client, String message) {
		if (client.player != null) {
			client.player.sendMessage(Text.literal(message), true);
		}
	}
}

