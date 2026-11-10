package com.mormkillen.chromaticaberration.client;

import com.mormkillen.chromaticaberration.AberrationConfig;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.ClickableWidget;
import net.minecraft.client.gui.widget.CyclingButtonWidget;
import net.minecraft.client.gui.widget.ElementListWidget;
import net.minecraft.client.gui.widget.SliderWidget;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.screen.ScreenTexts;
import net.minecraft.text.Text;

import java.util.ArrayList;
import java.util.List;

/**
 * Config screen shown through Mod Menu. Vanilla widgets only — no Cloth
 * Config dependency. Changes apply live; the file is saved on Done.
 */
public class AberrationConfigScreen extends Screen {
	private static final int ROW_WIDTH = 220;
	private static final int ROW_HEIGHT = 18;
	private static final int ROW_GAP = 20;

	private final Screen parent;
	private final List<ButtonWidget> stateButtons = new ArrayList<>();

	public AberrationConfigScreen(Screen parent) {
		super(Text.translatable("config.chromaticaberration.title"));
		this.parent = parent;
	}

	@Override
	protected void init() {
		AberrationConfig config = AberrationConfig.get();
		this.stateButtons.clear();
		ConfigList list = new ConfigList(this.client, this.width, this.height, 30, this.height - 30);
		this.addDrawableChild(list);

		// 1. Intensity multiplier slider (0% - 200%)
		list.addWidget(new IntensitySlider(0, 0, ROW_WIDTH, ROW_HEIGHT, config));

		// 2. Disable the clean center zone
		list.addWidget(CyclingButtonWidget.onOffBuilder(config.disableCenterDeadzone)
				.build(0, 0, ROW_WIDTH, ROW_HEIGHT,
						Text.translatable("config.chromaticaberration.disable_center_deadzone"),
						(button, value) -> config.disableCenterDeadzone = value));

		// 3. Keep aberration visible while standing still
		list.addWidget(CyclingButtonWidget.onOffBuilder(config.alwaysVisible)
				.build(0, 0, ROW_WIDTH, ROW_HEIGHT,
						Text.translatable("config.chromaticaberration.always_visible"),
						(button, value) -> config.alwaysVisible = value));

		// 4. Ignore camera speed; slider controls intensity directly
		list.addWidget(CyclingButtonWidget.onOffBuilder(config.disableSpeedScaling)
				.build(0, 0, ROW_WIDTH, ROW_HEIGHT,
						Text.translatable("config.chromaticaberration.disable_speed_scaling"),
						(button, value) -> config.disableSpeedScaling = value));

		// 5. Apply to all states, or limit the effect to selected conditions
		list.addWidget(CyclingButtonWidget.onOffBuilder(config.applyAllStates)
				.build(0, 0, ROW_WIDTH, ROW_HEIGHT,
						Text.translatable("config.chromaticaberration.apply_all_states"),
						(button, value) -> {
							config.applyAllStates = value;
							for (ButtonWidget stateButton : stateButtons) {
								stateButton.active = !value;
							}
						}));

		for (PlayerStates.Group group : PlayerStates.GROUPS) {
			list.addHeader(Text.translatable("config.chromaticaberration.group." + group.id()));
			for (String state : group.states()) {
				ButtonWidget stateButton = ButtonWidget.builder(stateLabel(config, state),
						button -> {
							if (!config.triggerStates.add(state)) {
								config.triggerStates.remove(state);
							}
							button.setMessage(stateLabel(config, state));
						})
						.dimensions(0, 0, ROW_WIDTH, ROW_HEIGHT)
						.build();
				stateButton.active = !config.applyAllStates;
				this.stateButtons.add(stateButton);
				list.addWidget(stateButton);
			}
		}

		// Done
		this.addDrawableChild(ButtonWidget.builder(ScreenTexts.DONE, button -> this.close())
				.dimensions(this.width / 2 - 100, this.height - 24, 200, ROW_HEIGHT)
				.build());
	}

	@Override
	public void render(DrawContext context, int mouseX, int mouseY, float delta) {
		this.renderBackground(context);
		super.render(context, mouseX, mouseY, delta);
		AberrationConfig config = AberrationConfig.get();
		Text stateMode = config.applyAllStates
				? Text.translatable("config.chromaticaberration.state_mode.all")
				: config.triggerStates.isEmpty()
						? Text.translatable("config.chromaticaberration.state_mode.none")
						: Text.translatable("config.chromaticaberration.state_mode.selected_count",
								config.triggerStates.size());
		context.drawCenteredTextWithShadow(this.textRenderer, this.title, this.width / 2, 8, 0xFFFFFF);
		context.drawCenteredTextWithShadow(this.textRenderer, stateMode, this.width / 2, 20, 0xAAAAAA);
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

	private static Text stateLabel(AberrationConfig config, String state) {
		Text name = Text.translatable("config.chromaticaberration.state." + state);
		return Text.translatable(config.triggerStates.contains(state)
				? "config.chromaticaberration.state.selected"
				: "config.chromaticaberration.state.unselected", name);
	}

	private static final class ConfigList extends ElementListWidget<ConfigEntry> {
		ConfigList(MinecraftClient client, int width, int height, int top, int bottom) {
			super(client, width, height, top, bottom, ROW_GAP);
			this.setRenderBackground(false);
			this.setRenderHorizontalShadows(false);
		}

		void addWidget(ClickableWidget widget) {
			this.addEntry(new ConfigEntry(this.client.textRenderer, widget));
		}

		void addHeader(Text text) {
			this.addEntry(new ConfigEntry(this.client.textRenderer, text));
		}

		@Override
		public int getRowWidth() {
			return Math.min(ROW_WIDTH, this.width - 40);
		}
	}

	private static final class ConfigEntry extends ElementListWidget.Entry<ConfigEntry> {
		private final TextRenderer textRenderer;
		private final ClickableWidget widget;
		private final Text header;

		ConfigEntry(TextRenderer textRenderer, ClickableWidget widget) {
			this.textRenderer = textRenderer;
			this.widget = widget;
			this.header = null;
		}

		ConfigEntry(TextRenderer textRenderer, Text header) {
			this.textRenderer = textRenderer;
			this.widget = null;
			this.header = header;
		}

		@Override
		public void render(DrawContext context, int index, int y, int x, int entryWidth,
				int entryHeight, int mouseX, int mouseY, boolean hovered, float delta) {
			if (this.widget != null) {
				this.widget.setX(x + (entryWidth - this.widget.getWidth()) / 2);
				this.widget.setY(y);
				this.widget.render(context, mouseX, mouseY, delta);
			} else if (this.header != null) {
				context.drawCenteredTextWithShadow(this.textRenderer, this.header,
						x + entryWidth / 2, y + 5, 0xFFFFFF);
			}
		}

		@Override
		public List<? extends net.minecraft.client.gui.Selectable> selectableChildren() {
			return this.widget == null ? List.of() : List.of(this.widget);
		}

		@Override
		public List<? extends net.minecraft.client.gui.Element> children() {
			return this.widget == null ? List.of() : List.of(this.widget);
		}
	}
}
