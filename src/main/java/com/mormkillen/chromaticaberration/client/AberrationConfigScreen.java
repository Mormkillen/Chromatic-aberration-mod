package com.mormkillen.chromaticaberration.client;

import com.mormkillen.chromaticaberration.AberrationConfig;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.SliderWidget;
import net.minecraft.text.Text;

import java.util.function.BooleanSupplier;
import java.util.function.Consumer;

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
		super(Text.translatable("config.chromaticaberration.title"));
		this.parent = parent;
	}

	@Override
	protected void init() {
		AberrationConfig config = AberrationConfig.get();
		int centerX = this.width / 2 - ROW_WIDTH / 2;
		int y = 60;

		// 1. Intensity multiplier slider (0% - 200%)
		this.addDrawableChild(new IntensitySlider(centerX, y, ROW_WIDTH, ROW_HEIGHT, config));
		y += ROW_GAP;

		// 2. Disable the clean center zone
		this.addDrawableChild(toggleButton(centerX, y,
				Text.translatable("config.chromaticaberration.disable_center_deadzone"),
				() -> config.disableCenterDeadzone,
				value -> config.disableCenterDeadzone = value));
		y += ROW_GAP;

		// 3. Keep aberration visible while standing still
		this.addDrawableChild(toggleButton(centerX, y,
				Text.translatable("config.chromaticaberration.always_visible"),
				() -> config.alwaysVisible,
				value -> config.alwaysVisible = value));
		y += ROW_GAP;

		// 4. Ignore camera speed; slider controls intensity directly
		this.addDrawableChild(toggleButton(centerX, y,
				Text.translatable("config.chromaticaberration.disable_speed_scaling"),
				() -> config.disableSpeedScaling,
				value -> config.disableSpeedScaling = value));

		// Done
		this.addDrawableChild(ButtonWidget.builder(Text.translatable("gui.done"), button -> this.close())
				.dimensions(this.width / 2 - 100, this.height - 40, 200, ROW_HEIGHT)
				.build());
	}

	/**
	 * Plain toggle button. CyclingButtonWidget's onOffBuilder has a different
	 * intermediary method id in 1.21.9 and 1.21.11, so a single jar built for the
	 * whole 1.21.9 - 1.21.11 range cannot call it. ButtonWidget's builder,
	 * dimensions, build and setMessage are stable across those versions.
	 */
	private static ButtonWidget toggleButton(int x, int y, Text label,
			BooleanSupplier getter, Consumer<Boolean> setter) {
		return ButtonWidget.builder(toggleText(label, getter.getAsBoolean()), pressed -> {
			boolean next = !getter.getAsBoolean();
			setter.accept(next);
			pressed.setMessage(toggleText(label, next));
		}).dimensions(x, y, ROW_WIDTH, ROW_HEIGHT).build();
	}

	private static Text toggleText(Text label, boolean on) {
		return Text.translatable("config.chromaticaberration.toggle", label,
				Text.translatable(on
						? "config.chromaticaberration.state.on"
						: "config.chromaticaberration.state.off"));
	}

	@Override
	public void render(DrawContext context, int mouseX, int mouseY, float delta) {
		// 1.21.6+ blurs the background inside super.render(); drawing it here as
		// well throws "Can only blur once per frame".
		super.render(context, mouseX, mouseY, delta);
		context.drawCenteredTextWithShadow(this.textRenderer, this.title, this.width / 2, 20, 0xFFFFFF);
		context.drawCenteredTextWithShadow(this.textRenderer,
				Text.translatable("config.chromaticaberration.intensity_hint"), this.width / 2, 36, 0xAAAAAA);
	}

	@Override
	public void close() {
		AberrationConfig.get().save();
		if (this.client != null) {
			this.client.setScreen(this.parent);
		}
	}

	private static final class IntensitySlider extends SliderWidget {
		private final AberrationConfig config;

		IntensitySlider(int x, int y, int width, int height, AberrationConfig config) {
			super(x, y, width, height, Text.empty(), config.intensityMultiplier / 2.0);
			this.config = config;
			this.updateMessage();
		}

		@Override
		protected void updateMessage() {
			this.setMessage(Text.translatable("config.chromaticaberration.intensity", Math.round(this.value * 200)));
		}

		@Override
		protected void applyValue() {
			config.intensityMultiplier = (float) (this.value * 2.0);
		}
	}
}
