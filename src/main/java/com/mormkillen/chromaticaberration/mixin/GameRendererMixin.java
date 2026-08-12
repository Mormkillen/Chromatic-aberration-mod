package com.mormkillen.chromaticaberration.mixin;

import com.mormkillen.chromaticaberration.ChromaticAberrationClient;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
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
	private Minecraft minecraft;

	/**
	 * Apply the chromatic aberration after the level (including the hand) has
	 * been rendered into the main render target, before the HUD is drawn on top.
	 * This mirrors the exact spot vanilla uses for its own post effects, so the
	 * render state stays consistent.
	 */
	@Inject(
			method = "render",
			at = @At(
					value = "INVOKE",
					target = "Lnet/minecraft/client/renderer/GameRenderer;renderLevel(Lnet/minecraft/client/DeltaTracker;)V",
					shift = At.Shift.AFTER
			)
	)
	private void chromaticaberration$applySpeedAberration(DeltaTracker deltaTracker, boolean tick, CallbackInfo ci) {
		ChromaticAberrationClient.renderer().onWorldRendered(this.minecraft);
	}
}
