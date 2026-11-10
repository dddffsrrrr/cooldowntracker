package de.cooldowntracker;

import de.cooldowntracker.config.ModConfig;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

public enum CooldownType {
	WIND_CHARGE("wind_charge", true),
	ENDER_PEARL("ender_pearl", true),
	SPEAR("spear", false),
	ENCHANTED_GOLDEN_APPLE("enchanted_golden_apple", true);

	public final String id;
	/**
	 * true = ein erneuter Gebrauch beweist, dass der Cooldown vorbei war -> Timer neu starten.
	 * false = während der Cooldown läuft, werden weitere Auslöser ignoriert (Speer-Angriffe passieren oft).
	 */
	public final boolean restartOnUse;
	private ItemStack icon;

	CooldownType(String id, boolean restartOnUse) {
		this.id = id;
		this.restartOnUse = restartOnUse;
	}

	public Component displayName() {
		return Component.translatable("cooldowntracker.type." + id);
	}

	public ItemStack icon() {
		if (icon == null) {
			icon = switch (this) {
				case WIND_CHARGE -> new ItemStack(Items.WIND_CHARGE);
				case ENDER_PEARL -> new ItemStack(Items.ENDER_PEARL);
				case ENCHANTED_GOLDEN_APPLE -> new ItemStack(Items.ENCHANTED_GOLDEN_APPLE);
				case SPEAR -> new ItemStack(BuiltInRegistries.ITEM.getValue(Identifier.withDefaultNamespace("iron_spear")));
			};
		}
		return icon;
	}

	public boolean isEnabled(ModConfig c) {
		return switch (this) {
			case WIND_CHARGE -> c.windCharge;
			case ENDER_PEARL -> c.enderPearl;
			case SPEAR -> c.spear;
			case ENCHANTED_GOLDEN_APPLE -> c.enchantedGoldenApple;
		};
	}

	public void setEnabled(ModConfig c, boolean v) {
		switch (this) {
			case WIND_CHARGE -> c.windCharge = v;
			case ENDER_PEARL -> c.enderPearl = v;
			case SPEAR -> c.spear = v;
			case ENCHANTED_GOLDEN_APPLE -> c.enchantedGoldenApple = v;
		}
	}

	public int seconds(ModConfig c) {
		return switch (this) {
			case WIND_CHARGE -> c.windChargeSeconds;
			case ENDER_PEARL -> c.enderPearlSeconds;
			case SPEAR -> c.spearSeconds;
			case ENCHANTED_GOLDEN_APPLE -> c.enchantedGoldenAppleSeconds;
		};
	}

	public void setSeconds(ModConfig c, int v) {
		v = Math.max(1, Math.min(600, v));
		switch (this) {
			case WIND_CHARGE -> c.windChargeSeconds = v;
			case ENDER_PEARL -> c.enderPearlSeconds = v;
			case SPEAR -> c.spearSeconds = v;
			case ENCHANTED_GOLDEN_APPLE -> c.enchantedGoldenAppleSeconds = v;
		}
	}

	public int color(ModConfig c) {
		return switch (this) {
			case WIND_CHARGE -> c.windChargeColor;
			case ENDER_PEARL -> c.enderPearlColor;
			case SPEAR -> c.spearColor;
			case ENCHANTED_GOLDEN_APPLE -> c.enchantedGoldenAppleColor;
		} & 0xFFFFFF;
	}

	public void setColor(ModConfig c, int rgb) {
		rgb &= 0xFFFFFF;
		switch (this) {
			case WIND_CHARGE -> c.windChargeColor = rgb;
			case ENDER_PEARL -> c.enderPearlColor = rgb;
			case SPEAR -> c.spearColor = rgb;
			case ENCHANTED_GOLDEN_APPLE -> c.enchantedGoldenAppleColor = rgb;
		}
	}

	/** Schrittweite für +/- im Menü. */
	public int step() {
		return this == ENCHANTED_GOLDEN_APPLE ? 10 : 1;
	}
}
