package com.mormkillen.chromaticaberration;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * JSON-backed config (config/chromaticaberration.json), edited live through
 * the Mod Menu config screen. All fields have safe defaults; a corrupt or
 * missing file silently falls back to them.
 */
public final class AberrationConfig {
	private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

	private static AberrationConfig instance;

	/** Strength multiplier applied to the speed-based intensity (0.0 - 2.0). */
	public float intensityMultiplier = 1.0f;

	/** When true, the clean center zone is disabled: aberration everywhere. */
	public boolean disableCenterDeadzone = false;

	/** When true, a baseline intensity is applied even while standing still. */
	public boolean alwaysVisible = false;

	/** When true, intensity comes only from the slider, ignoring camera speed. */
	public boolean disableSpeedScaling = false;

	public static synchronized AberrationConfig get() {
		if (instance == null) {
			instance = load();
		}
		return instance;
	}

	private static Path path() {
		return FabricLoader.getInstance().getConfigDir().resolve("chromaticaberration.json");
	}

	private static AberrationConfig load() {
		Path path = path();
		if (Files.isRegularFile(path)) {
			try (Reader reader = Files.newBufferedReader(path)) {
				AberrationConfig config = GSON.fromJson(reader, AberrationConfig.class);
				if (config != null) {
					// Clamp into the supported range; guards against hand-edited junk.
					config.intensityMultiplier = Math.max(0.0f, Math.min(2.0f, config.intensityMultiplier));
					return config;
				}
			} catch (Throwable t) {
				ChromaticAberrationClient.LOGGER.warn("Failed to read config, using defaults", t);
			}
		}
		return new AberrationConfig();
	}

	public void save() {
		try {
			Files.createDirectories(path().getParent());
			try (Writer writer = Files.newBufferedWriter(path())) {
				GSON.toJson(this, writer);
			}
		} catch (IOException e) {
			ChromaticAberrationClient.LOGGER.warn("Failed to save config", e);
		}
	}
}
