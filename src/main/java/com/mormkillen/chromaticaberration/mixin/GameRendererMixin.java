package com.mormkillen.chromaticaberration.mixin;

import com.mormkillen.chromaticaberration.ChromaticAberrationClient;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.render.RenderTickCounter;
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
	@Inject(
			method = "render",
			at = @At(
					value = "INVOKE",
					target = "Lnet/minecraft/client/render/GameRenderer;renderWorld(Lnet/minecraft/client/render/RenderTickCounter;)V",
					shift = At.Shift.AFTER
			)
	)
	private void chromaticaberration$applySpeedAberration(RenderTickCounter tickCounter, boolean tick, CallbackInfo ci) {
		ChromaticAberrationClient.renderer().onWorldRendered(this.client);
	}
}
