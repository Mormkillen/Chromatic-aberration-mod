package com.mormkillen.chromaticaberration.client;

import com.mormkillen.chromaticaberration.AberrationConfig;
import com.mormkillen.chromaticaberration.ChromaticAberrationClient;
import com.mormkillen.chromaticaberration.mixin.PostChainAccessor;
import com.mormkillen.chromaticaberration.mixin.PostPassAccessor;
import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.buffers.Std140Builder;
import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.resource.CrossFrameResourcePool;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelTargetBundle;
import net.minecraft.client.renderer.PostChain;
import net.minecraft.client.renderer.PostPass;
import net.minecraft.resources.Identifier;
import net.minecraft.world.phys.Vec3;
import org.lwjgl.system.MemoryStack;

import java.nio.ByteBuffer;

/**
 * Owns the chromatic-aberration post shader and drives its strength from the
 * camera's per-frame velocity.
 *
 * 26.1 rendering notes (Minecraft ships unobfuscated; Mojang names):
 *  - The post chain comes from {@code ShaderManager#getPostChain}; it returns
 *    null (and logs) when the JSON/GLSL fails to load.
 *  - {@code PostChain#process(RenderTarget, GraphicsResourceAllocator)} runs
 *    a frame graph per call; internal targets are sized from the passed
 *    render target each frame, so window resizes need no explicit handling.
 *    A {@link CrossFrameResourcePool} recycles the intermediate "swap"
 *    target; entries expire via {@code endFrame()}, like vanilla's own pool.
 *  - Per-frame strength is written straight into the pass's std140 UBO
 *    ("AberrationConfig"); there is no public uniform setter.
 *  - Teleports / respawns / dimension changes cause huge position jumps;
 *    those frames are ignored so the effect does not flash at full strength.
 *  - When the intensity is effectively zero the shader is not rendered at
 *    all, so the mod costs no GPU time while standing still.
 */
public final class AberrationRenderer {
	/** Post-effect id, resolved to assets/chromaticaberration/post_effect/chromatic_aberration.json. */
	private static final Identifier SHADER_ID = Identifier.fromNamespaceAndPath(ChromaticAberrationClient.MOD_ID, "chromatic_aberration");

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

	private PostChain postChain = null;
	/** Recycles the intermediate "swap" target (vanilla uses a pool of 3 too). */
	private final CrossFrameResourcePool resourcePool = new CrossFrameResourcePool(3);
	/**
	 * The pass's std140 UBO holding Intensity/UseDeadzone/DebugMode. Values
	 * from the pipeline JSON are baked into per-pass GPU buffers; there is no
	 * public setter, so we write the three floats straight into the buffer
	 * each frame.
	 */
	private GpuBuffer aberrationUbo = null;
	private boolean shaderBroken = false;

	/** 0 = normal, 1 = passthrough copy, 2 = force alpha 1, 3 = visualize alpha. */
	private int debugMode = 0;

	private Vec3 prevCameraPos = null;
	private long prevFrameNanos = -1L;
	private float intensity = 0.0f;

	public int cycleDebugMode() {
		debugMode = (debugMode + 1) % 4;
		return debugMode;
	}

	/** Called every frame right after {@code GameRenderer#renderLevel} inside {@code GameRenderer#render}. */
	public void onWorldRendered(Minecraft minecraft) {
		updateIntensity(minecraft);

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

		RenderTarget renderTarget = minecraft.getMainRenderTarget();
		if (renderTarget == null) {
			return;
		}

		ensurePostChain(minecraft);
		if (postChain == null) {
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

			postChain.process(renderTarget, resourcePool);
			resourcePool.endFrame();
		} catch (Throwable t) {
			// Never let a GL problem crash the render loop.
			shaderBroken = true;
			closePostChain();
			ChromaticAberrationClient.LOGGER.error("Chromatic aberration shader failed while rendering; effect disabled", t);
		}
	}

	/** Called on resource reload so the shader is rebuilt from the (re)loaded resources. */
	public void onResourceReload() {
		closePostChain();
		shaderBroken = false;
	}

	private void updateIntensity(Minecraft minecraft) {
		long now = System.nanoTime();
		// Compute the frame delta ONCE, before prevFrameNanos is overwritten.
		double dt = prevFrameNanos > 0L ? (now - prevFrameNanos) / 1_000_000_000.0 : 0.0;
		float target = 0.0f;

		if (minecraft.gameRenderer != null && minecraft.gameRenderer.getMainCamera() != null && minecraft.level != null) {
			Vec3 cameraPos = minecraft.gameRenderer.getMainCamera().position();

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

	private void ensurePostChain(Minecraft minecraft) {
		// Recreate lazily after a reload. getPostChain logs and returns null
		// when the pipeline JSON or GLSL fails to compile.
		if (postChain == null && !shaderBroken) {
			postChain = minecraft.getShaderManager().getPostChain(SHADER_ID, LevelTargetBundle.MAIN_TARGETS);
			if (postChain == null) {
				shaderBroken = true;
				ChromaticAberrationClient.LOGGER.error("Failed to load chromatic aberration shader; effect disabled");
			} else {
				// Find the aberration pass's UBO so we can drive it per frame.
				aberrationUbo = null;
				for (PostPass pass : ((PostChainAccessor) postChain).chromaticaberration$getPasses()) {
					GpuBuffer ubo = ((PostPassAccessor) pass).chromaticaberration$getCustomUniforms().get("AberrationConfig");
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

	private void closePostChain() {
		if (postChain != null) {
			try {
				postChain.close();
			} catch (Throwable ignored) {
				// Closing must never throw into the reload/render path.
			}
			postChain = null;
		}
		aberrationUbo = null;
		try {
			resourcePool.close();
		} catch (Throwable ignored) {
		}
	}
}
