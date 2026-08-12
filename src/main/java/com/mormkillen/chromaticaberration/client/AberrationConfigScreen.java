package com.mormkillen.chromaticaberration.client;

import com.mormkillen.chromaticaberration.AberrationConfig;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.CycleButton;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;

/**
 * Config screen shown through Mod Menu. Vanilla widgets only — no Cloth
 * Config dependency. Changes apply live; the file is saved on Done.
 */
public class AberrationConfigScreen extends Screen {
	private static final int ROW_WIDTH = 220;
	private static final int ROW_HEIGHT = 20;
	private static final int ROW_GAP = 26;

	private final Screen parent;

	public AberrationConfigScreen(Screen parent) {
		super(Component.translatable("config.chromaticaberration.title"));
		this.parent = parent;
	}

	@Override
	protected void init() {
		AberrationConfig config = AberrationConfig.get();
		int centerX = this.width / 2 - ROW_WIDTH / 2;
		int y = 60;

		// 1. Intensity multiplier slider (0% - 200%)
		this.addRenderableWidget(new IntensitySlider(centerX, y, ROW_WIDTH, ROW_HEIGHT, config));
		y += ROW_GAP;

		// 2. Disable the clean center zone
		this.addRenderableWidget(CycleButton.onOffBuilder(config.disableCenterDeadzone)
				.create(centerX, y, ROW_WIDTH, ROW_HEIGHT,
						Component.translatable("config.chromaticaberration.disable_center_deadzone"),
						(button, value) -> config.disableCenterDeadzone = value));
		y += ROW_GAP;

		// 3. Keep aberration visible while standing still
		this.addRenderableWidget(CycleButton.onOffBuilder(config.alwaysVisible)
				.create(centerX, y, ROW_WIDTH, ROW_HEIGHT,
						Component.translatable("config.chromaticaberration.always_visible"),
						(button, value) -> config.alwaysVisible = value));
		y += ROW_GAP;

		// 4. Ignore camera speed; slider controls intensity directly
		this.addRenderableWidget(CycleButton.onOffBuilder(config.disableSpeedScaling)
				.create(centerX, y, ROW_WIDTH, ROW_HEIGHT,
						Component.translatable("config.chromaticaberration.disable_speed_scaling"),
						(button, value) -> config.disableSpeedScaling = value));

		// Done
		this.addRenderableWidget(Button.builder(CommonComponents.GUI_DONE, button -> this.onClose())
				.bounds(this.width / 2 - 100, this.height - 40, 200, ROW_HEIGHT)
				.build());
	}

	@Override
	public void extractRenderState(GuiGraphicsExtractor extractor, int mouseX, int mouseY, float delta) {
		this.extractBackground(extractor, mouseX, mouseY, delta);
		super.extractRenderState(extractor, mouseX, mouseY, delta);
		extractor.centeredText(this.font, this.title, this.width / 2, 20, 0xFFFFFF);
		extractor.centeredText(this.font,
				Component.translatable("config.chromaticaberration.intensity_hint"), this.width / 2, 36, 0xAAAAAA);
	}

	@Override
	public void onClose() {
		AberrationConfig.get().save();
		if (this.minecraft != null) {
			this.minecraft.setScreen(this.parent);
		}
	}

	private static final class IntensitySlider extends AbstractSliderButton {
		private final AberrationConfig config;

		IntensitySlider(int x, int y, int width, int height, AberrationConfig config) {
			super(x, y, width, height, Component.empty(), config.intensityMultiplier / 2.0);
			this.config = config;
			this.updateMessage();
		}

		@Override
		protected void updateMessage() {
			this.setMessage(Component.translatable("config.chromaticaberration.intensity", Math.round(this.value * 200)));
		}

		@Override
		protected void applyValue() {
			config.intensityMultiplier = (float) (this.value * 2.0);
		}
	}
}
