package com.mormkillen.chromaticaberration.client;

import com.google.gson.JsonSyntaxException;
import com.mormkillen.chromaticaberration.AberrationConfig;
import com.mormkillen.chromaticaberration.ChromaticAberrationClient;
import com.mormkillen.chromaticaberration.mixin.PostEffectProcessorAccessor;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.Framebuffer;
import net.minecraft.client.gl.GlUniform;
import net.minecraft.client.gl.PostEffectPass;
import net.minecraft.client.gl.PostEffectProcessor;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Vec3d;

import java.io.IOException;

/**
 * Owns the chromatic-aberration post shader and drives its strength from the
 * camera's per-frame velocity.
 *
 * Defensive design notes (things that commonly break mods like this):
 *  - The processor is created lazily: on the very first frames the main
 *    framebuffer may not be ready yet.
 *  - Shader creation/rendering is wrapped in try/catch; any failure disables
 *    the effect instead of crashing the game.
 *  - F3+T resource reloads and window resizes are handled explicitly.
 *  - Teleports / respawns / dimension changes cause huge position jumps;
 *    those frames are ignored so the effect does not flash at full strength.
 *  - When the intensity is effectively zero the shader is not rendered at
 *    all, so the mod costs no GPU time while standing still.
 */
public final class AberrationRenderer {
	// NOTE: the constructor reads the post JSON from this exact resource path.
	private static final Identifier SHADER_ID = Identifier.of(ChromaticAberrationClient.MOD_ID, "shaders/post/chromatic_aberration.json");

	/** Camera speed (blocks/second) below which the effect fades to zero. */
	private static final float MIN_SPEED = 1.5f;
	/** Camera speed (blocks/second) at which the effect reaches full strength. */
	private static final float FULL_SPEED = 10.0f;
	/** Ignore frame deltas longer than this (lag spikes, window drag, etc.). */
	private static final double MAX_DT_SECONDS = 0.25;
	/** Position jumps larger than this within one frame count as teleports. */
	private static final double TELEPORT_DISTANCE = 64.0;
	/** How quickly the intensity eases towards its target (per second). */
	private static final float SMOOTHING = 10.0f;
	/** Skip rendering below this intensity. */
	private static final float EPSILON = 0.002f;
	/** Baseline intensity used when "静止时也渲染色差" is enabled. */
	private static final float STATIONARY_BASELINE = 0.35f;

	private PostEffectProcessor processor = null;
	private GlUniform intensityUniform = null;
	private GlUniform deadzoneUniform = null;
	private GlUniform debugModeUniform = null;
	private boolean shaderBroken = false;
	private int lastWidth = -1;
	private int lastHeight = -1;

	/** 0 = normal, 1 = passthrough copy, 2 = force alpha 1, 3 = visualize alpha. */
	private int debugMode = 0;

	private Vec3d prevCameraPos = null;
	private long prevFrameNanos = -1L;
	private float intensity = 0.0f;

	public int cycleDebugMode() {
		debugMode = (debugMode + 1) % 4;
		return debugMode;
	}

	/** Called every frame from the tail of {@code GameRenderer#renderWorld}. */
	public void onWorldRendered(MinecraftClient client, float tickDelta) {
		updateIntensity(client);

		// A shader pack owns the final image; never fight it for rendering.
		if (IrisCompat.isShaderPackActive()) {
			return;
		}

		AberrationConfig config = AberrationConfig.get();
		// Speed scaling off: the slider is the only source of intensity.
		float effectiveIntensity = config.disableSpeedScaling
				? config.intensityMultiplier
				: intensity * config.intensityMultiplier;

		// Debug modes bypass the zero-intensity skip so they can be inspected anytime.
		if (shaderBroken || (effectiveIntensity < EPSILON && debugMode == 0)) {
			return;
		}

		Framebuffer framebuffer = client.getFramebuffer();
		if (framebuffer == null) {
			return;
		}

		ensureProcessor(client, framebuffer);
		if (processor == null) {
			return;
		}

		try {
			if (intensityUniform != null) {
				intensityUniform.set(effectiveIntensity);
			}
			if (deadzoneUniform != null) {
				deadzoneUniform.set(config.disableCenterDeadzone ? 0.0f : 1.0f);
			}
			if (debugModeUniform != null) {
				debugModeUniform.set(debugMode);
			}
			processor.render(tickDelta);
			// PostEffectProcessor leaves its internal output target bound;
			// re-bind the main framebuffer exactly like vanilla does.
			framebuffer.beginWrite(true);
		} catch (Throwable t) {
			// Never let a GL problem crash the render loop.
			shaderBroken = true;
			closeProcessor();
			ChromaticAberrationClient.LOGGER.error("Chromatic aberration shader failed while rendering; effect disabled", t);
		}
	}

	/** Called on F3+T so the shader is rebuilt from the (re)loaded resources. */
	public void onResourceReload() {
		closeProcessor();
		shaderBroken = false;
		lastWidth = -1;
		lastHeight = -1;
	}

	private void updateIntensity(MinecraftClient client) {
		long now = System.nanoTime();
		// Compute the frame delta ONCE, before prevFrameNanos is overwritten.
		double dt = prevFrameNanos > 0L ? (now - prevFrameNanos) / 1_000_000_000.0 : 0.0;
		float target = 0.0f;

		if (client.gameRenderer != null && client.gameRenderer.getCamera() != null && client.world != null) {
			Vec3d cameraPos = client.gameRenderer.getCamera().getPos();

			if (prevCameraPos != null && dt > 0.0 && dt <= MAX_DT_SECONDS) {
				double distance = cameraPos.distanceTo(prevCameraPos);

				if (distance <= TELEPORT_DISTANCE) {
					double speed = distance / dt; // blocks per second
					target = (float) ((speed - MIN_SPEED) / (FULL_SPEED - MIN_SPEED));
					target = Math.max(0.0f, Math.min(1.0f, target));
					// x^1.5 curve: gentle at low speeds, strong at high speeds.
					target = (float) (target * Math.sqrt(target));
				}
				// else: teleport / respawn / dimension change — ignore this frame.
			}

			prevCameraPos = cameraPos;
		} else {
			prevCameraPos = null;
		}
		prevFrameNanos = now;

		// Config: keep a visible baseline even without motion when enabled.
		if (AberrationConfig.get().alwaysVisible) {
			target = Math.max(target, STATIONARY_BASELINE);
		}

		// Frame-rate independent smoothing.
		float k = dt > 0.0 ? (float) Math.min(1.0, Math.min(dt, MAX_DT_SECONDS) * SMOOTHING) : 0.1f;
		intensity += (target - intensity) * k;
	}

	private void ensureProcessor(MinecraftClient client, Framebuffer framebuffer) {
		// Recreate lazily after a reload.
		if (processor == null && !shaderBroken) {
			try {
				processor = new PostEffectProcessor(
						client.getTextureManager(),
						client.getResourceManager(),
						framebuffer,
						SHADER_ID);
				processor.setupDimensions(framebuffer.textureWidth, framebuffer.textureHeight);
				lastWidth = framebuffer.textureWidth;
				lastHeight = framebuffer.textureHeight;
				// Cache the Intensity uniform of our pass (1.20.1 has no
				// PostEffectProcessor#setUniformValue, so we go through the pass).
				intensityUniform = null;
				deadzoneUniform = null;
				debugModeUniform = null;
				for (PostEffectPass pass : ((PostEffectProcessorAccessor) processor).chromaticaberration$getPasses()) {
					if (intensityUniform == null) {
						intensityUniform = pass.getProgram().getUniformByName("Intensity");
					}
					if (deadzoneUniform == null) {
						deadzoneUniform = pass.getProgram().getUniformByName("UseDeadzone");
					}
					if (debugModeUniform == null) {
						debugModeUniform = pass.getProgram().getUniformByName("DebugMode");
					}
				}
				if (intensityUniform == null) {
					ChromaticAberrationClient.LOGGER.warn("Shader pass has no 'Intensity' uniform; strength will stay at the JSON default");
				}
				if (debugModeUniform == null) {
					ChromaticAberrationClient.LOGGER.warn("Shader pass has no 'DebugMode' uniform; F8 debug switch will do nothing");
				}
				ChromaticAberrationClient.LOGGER.info("Chromatic aberration shader loaded");
			} catch (IOException | JsonSyntaxException e) {
				shaderBroken = true;
				processor = null;
				ChromaticAberrationClient.LOGGER.error("Failed to load chromatic aberration shader; effect disabled", e);
				return;
			}
		}

		// Follow window resolution changes.
		if (processor != null && (framebuffer.textureWidth != lastWidth || framebuffer.textureHeight != lastHeight)) {
			processor.setupDimensions(framebuffer.textureWidth, framebuffer.textureHeight);
			lastWidth = framebuffer.textureWidth;
			lastHeight = framebuffer.textureHeight;
		}
	}

	private void closeProcessor() {
		if (processor != null) {
			try {
				processor.close();
			} catch (Throwable ignored) {
				// Closing must never throw into the reload/render path.
			}
			processor = null;
		}
		intensityUniform = null;
		deadzoneUniform = null;
		debugModeUniform = null;
	}
}
