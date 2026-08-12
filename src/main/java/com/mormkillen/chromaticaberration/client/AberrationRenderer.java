package com.mormkillen.chromaticaberration.client;

import com.mormkillen.chromaticaberration.AberrationConfig;
import com.mormkillen.chromaticaberration.ChromaticAberrationClient;
import com.mormkillen.chromaticaberration.mixin.PostEffectPassAccessor;
import com.mormkillen.chromaticaberration.mixin.PostEffectProcessorAccessor;
import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.buffers.Std140Builder;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.Framebuffer;
import net.minecraft.client.gl.PostEffectPass;
import net.minecraft.client.gl.PostEffectProcessor;
import net.minecraft.client.render.DefaultFramebufferSet;
import net.minecraft.client.util.memory.ObjectPool;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Vec3d;
import org.lwjgl.system.MemoryStack;

import java.nio.ByteBuffer;

/**
 * Owns the chromatic-aberration post shader and drives its strength from the
 * camera's per-frame velocity.
 *
 * 1.21.2+ rendering notes (the pipeline was rebuilt around FrameGraphBuilder):
 *  - The processor is obtained from {@code ShaderLoader#loadPostEffect}; it
 *    returns null (and logs) when the JSON/GLSL fails to load, so there is no
 *    checked exception to catch here.
 *  - {@code PostEffectProcessor#render(Framebuffer, ObjectAllocator)} builds
 *    and runs a frame graph every call; internal targets are sized from the
 *    passed framebuffer each frame, so window resizes need no explicit
 *    handling. A {@link Pool} recycles the intermediate "swap" framebuffer;
 *    entries expire after 3 frames via {@code decrementLifespan()}, exactly
 *    like vanilla's own post-processing pool.
 *  - Blend/depth state mirrors the vanilla creeper-vision block: the frame
 *    graph does not reset those, and an enabled depth test would kill the
 *    fullscreen quad against the world's depth buffer.
 *  - Teleports / respawns / dimension changes cause huge position jumps;
 *    those frames are ignored so the effect does not flash at full strength.
 *  - When the intensity is effectively zero the shader is not rendered at
 *    all, so the mod costs no GPU time while standing still.
 */
public final class AberrationRenderer {
	/** Post-effect id, resolved to assets/chromaticaberration/post_effect/chromatic_aberration.json. */
	private static final Identifier SHADER_ID = Identifier.of(ChromaticAberrationClient.MOD_ID, "chromatic_aberration");

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
	/** Recycles the intermediate "swap" framebuffer (vanilla uses ObjectPool(3) too). */
	private final ObjectPool pool = new ObjectPool(3);
	/**
	 * The pass's std140 UBO holding Intensity/UseDeadzone/DebugMode. 1.21.6+
	 * bakes JSON uniforms into per-pass GPU buffers; there is no public setter,
	 * so we write the three floats straight into the buffer each frame.
	 */
	private GpuBuffer aberrationUbo = null;
	private boolean shaderBroken = false;

	/** 0 = normal, 1 = passthrough copy, 2 = force alpha 1, 3 = visualize alpha. */
	private int debugMode = 0;

	private Vec3d prevCameraPos = null;
	private long prevFrameNanos = -1L;
	private float intensity = 0.0f;

	public int cycleDebugMode() {
		debugMode = (debugMode + 1) % 4;
		return debugMode;
	}

	/** Called every frame right after {@code GameRenderer#renderWorld} inside {@code GameRenderer#render}. */
	public void onWorldRendered(MinecraftClient client) {
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

		ensureProcessor(client);
		if (processor == null) {
			return;
		}

		try {
			if (aberrationUbo != null) {
				// std140: three consecutive floats pack at offsets 0/4/8.
				try (MemoryStack stack = MemoryStack.stackPush()) {
					ByteBuffer data = Std140Builder.onStack(stack, 16)
							.putFloat(effectiveIntensity)
							.putFloat(config.disableCenterDeadzone ? 0.0f : 1.0f)
							.putFloat((float) debugMode)
							.get();
					RenderSystem.getDevice().createCommandEncoder().writeToBuffer(aberrationUbo.slice(), data);
				}
			}

			processor.render(framebuffer, pool);
			pool.decrementLifespan();
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
	}

	private void updateIntensity(MinecraftClient client) {
		long now = System.nanoTime();
		// Compute the frame delta ONCE, before prevFrameNanos is overwritten.
		double dt = prevFrameNanos > 0L ? (now - prevFrameNanos) / 1_000_000_000.0 : 0.0;
		float target = 0.0f;

		if (client.gameRenderer != null && client.gameRenderer.getCamera() != null && client.world != null) {
			Vec3d cameraPos = client.gameRenderer.getCamera().getCameraPos();

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

	private void ensureProcessor(MinecraftClient client) {
		// Recreate lazily after a reload. loadPostEffect logs and returns null
		// when the pipeline JSON or GLSL fails to compile.
		if (processor == null && !shaderBroken) {
			processor = client.getShaderLoader().loadPostEffect(SHADER_ID, DefaultFramebufferSet.MAIN_ONLY);
			if (processor == null) {
				shaderBroken = true;
				ChromaticAberrationClient.LOGGER.error("Failed to load chromatic aberration shader; effect disabled");
			} else {
				// Find the aberration pass's UBO so we can drive it per frame.
				aberrationUbo = null;
				for (PostEffectPass pass : ((PostEffectProcessorAccessor) processor).chromaticaberration$getPasses()) {
					GpuBuffer ubo = ((PostEffectPassAccessor) pass).chromaticaberration$getUniformBuffers().get("AberrationConfig");
					if (ubo != null) {
						aberrationUbo = ubo;
						break;
					}
				}
				if (aberrationUbo == null) {
					ChromaticAberrationClient.LOGGER.warn("No 'AberrationConfig' UBO found; strength will stay at the JSON default");
				}
				ChromaticAberrationClient.LOGGER.info("Chromatic aberration shader loaded");
			}
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
		aberrationUbo = null;
		try {
			pool.clear();
		} catch (Throwable ignored) {
		}
	}
}
