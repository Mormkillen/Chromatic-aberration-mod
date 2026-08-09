package com.mormkillen.chromaticaberration.mixin;

import com.mormkillen.chromaticaberration.ChromaticAberrationClient;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.util.math.MatrixStack;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GameRenderer.class)
public abstract class GameRendererMixin {
	@Shadow
	@Final
	private MinecraftClient client;

	/**
	 * Apply the chromatic aberration after the world (including the hand) has
	 * been rendered into the main framebuffer, before the HUD is drawn on top.
	 * This mirrors the exact spot vanilla uses for its own post shaders
	 * (creeper / spectator vision), so RenderSystem state stays consistent.
	 */
	@Inject(method = "renderWorld", at = @At("TAIL"))
	private void chromaticaberration$applySpeedAberration(float tickDelta, long limitTime, MatrixStack matrices, CallbackInfo ci) {
		ChromaticAberrationClient.renderer().onWorldRendered(this.client, tickDelta);
	}
}
