package de.cooldowntracker.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import de.cooldowntracker.CooldownTrackerClient;
import net.fabricmc.loader.api.FabricLoader;

import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/** Alle Einstellungen der Mod. Wird als config/cooldowntracker.json gespeichert. */
public final class ModConfig {
	private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
	private static final Path FILE = FabricLoader.getInstance().getConfigDir().resolve("cooldowntracker.json");
	private static ModConfig instance;

	/** Hauptschalter für die ganze Anzeige. */
	public boolean enabled = true;

	// Welche Cooldowns angezeigt werden
	public boolean windCharge = true;
	public boolean enderPearl = true;
	public boolean spear = true;
	public boolean enchantedGoldenApple = true;

	// Cooldown-Dauer in Sekunden
	public int windChargeSeconds = 5;
	public int enderPearlSeconds = 12;
	public int spearSeconds = 10;
	public int enchantedGoldenAppleSeconds = 120;

	/** Wann der Speer-Cooldown startet. */
	public SpearTrigger spearTrigger = SpearTrigger.ATTACK;

	// Farben (RGB, z.B. 0xFF55AA)
	public int windChargeColor = 0x55FFFF;
	public int enderPearlColor = 0xFF55AA;
	public int spearColor = 0xFFAA00;
	public int enchantedGoldenAppleColor = 0xFFFF55;
	/** true = Farbe wechselt nach Restzeit (rot -> gelb -> grün) statt eigener Farbe. */
	public boolean colorByTime = false;

	// Anzeige
	public Style style = Style.TEXT;
	public boolean background = false;
	public boolean showDecimals = false;
	public boolean onlyWithLineOfSight = false;
	public int maxDistance = 64;
	public float scale = 1.0f;
	/** Kamera-Wackeln (Laufen/Schaden) ausgleichen. Bei Problemen ausschalten. */
	public boolean compensateBobbing = true;

	public enum Style {
		TEXT, ICONS;

		public Style next() {
			return values()[(ordinal() + 1) % values().length];
		}
	}

	public enum SpearTrigger {
		ATTACK, CHARGE, BOTH;

		public SpearTrigger next() {
			return values()[(ordinal() + 1) % values().length];
		}
	}

	public static ModConfig get() {
		if (instance == null) load();
		return instance;
	}

	public static void load() {
		ModConfig cfg = null;
		if (Files.exists(FILE)) {
			try (Reader r = Files.newBufferedReader(FILE, StandardCharsets.UTF_8)) {
				cfg = GSON.fromJson(r, ModConfig.class);
			} catch (Exception e) {
				CooldownTrackerClient.LOGGER.warn("Konnte Config nicht laden, nutze Standardwerte", e);
			}
		}
		instance = cfg != null ? cfg : new ModConfig();
		if (instance.spearTrigger == null) instance.spearTrigger = SpearTrigger.ATTACK;
		if (instance.style == null) instance.style = Style.TEXT;
		save();
	}

	public static void save() {
		try {
			Files.createDirectories(FILE.getParent());
			try (Writer w = Files.newBufferedWriter(FILE, StandardCharsets.UTF_8)) {
				GSON.toJson(instance, w);
			}
		} catch (Exception e) {
			CooldownTrackerClient.LOGGER.warn("Konnte Config nicht speichern", e);
		}
	}
}
