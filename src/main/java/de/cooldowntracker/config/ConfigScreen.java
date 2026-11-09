package de.cooldowntracker.config;

import de.cooldowntracker.CooldownType;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.options.controls.KeyBindsScreen;
import net.minecraft.network.chat.Component;

import java.util.Locale;

/** Einstellungsmenü – erreichbar über Mod Menu (oder eine eigene Taste). */
public class ConfigScreen extends Screen {
	private static final int ROW = 22;
	private static final int TOP = 28;
	private final Screen parent;

	public ConfigScreen(Screen parent) {
		super(Component.translatable("cooldowntracker.title"));
		this.parent = parent;
	}

	@Override
	protected void init() {
		ModConfig c = ModConfig.get();
		int cx = this.width / 2;
		int leftX = cx - 155;
		int rightX = cx + 5;

		// Hauptschalter
		addRenderableWidget(Button.builder(
						label(Component.translatable("cooldowntracker.config.enabled"), c.enabled),
						b -> { c.enabled = !c.enabled; changed(); })
				.bounds(leftX, TOP, 310, 20).build());

		int startY = TOP + ROW + 12;

		// ---------- Linke Spalte: Cooldowns  [an/aus (Dauer)] [Farbe] [-] [+]
		int y = startY;
		for (CooldownType type : CooldownType.values()) {
			Component text = Component.empty()
					.append(label(type.displayName(), type.isEnabled(c)))
					.append(Component.literal(" " + type.seconds(c) + "s").withStyle(ChatFormatting.GRAY));
			addRenderableWidget(Button.builder(text, b -> { type.setEnabled(c, !type.isEnabled(c)); changed(); })
					.bounds(leftX, y, 84, 20).build());
			int rgb = type.color(c);
			addRenderableWidget(Button.builder(Component.literal("█").withStyle(s -> s.withColor(rgb)),
							b -> this.minecraft.setScreen(new ColorScreen(this, type)))
					.bounds(leftX + 86, y, 20, 20).build());
			addRenderableWidget(Button.builder(Component.literal("-"),
							b -> { type.setSeconds(c, type.seconds(c) - type.step()); changed(); })
					.bounds(leftX + 108, y, 20, 20).build());
			addRenderableWidget(Button.builder(Component.literal("+"),
							b -> { type.setSeconds(c, type.seconds(c) + type.step()); changed(); })
					.bounds(leftX + 130, y, 20, 20).build());
			y += ROW;
		}
		addRenderableWidget(Button.builder(
						Component.translatable("cooldowntracker.config.spear_trigger",
								Component.translatable("cooldowntracker.spear_trigger." + c.spearTrigger.name().toLowerCase(Locale.ROOT))),
						b -> { c.spearTrigger = c.spearTrigger.next(); changed(); })
				.bounds(leftX, y, 150, 20).build());
		y += ROW;
		addRenderableWidget(Button.builder(
						label(Component.translatable("cooldowntracker.config.color_by_time"), c.colorByTime),
						b -> { c.colorByTime = !c.colorByTime; changed(); })
				.bounds(leftX, y, 150, 20).build());

		// ---------- Rechte Spalte: Anzeige
		y = startY;
		addRenderableWidget(Button.builder(
						Component.translatable("cooldowntracker.config.style",
								Component.translatable("cooldowntracker.style." + c.style.name().toLowerCase(Locale.ROOT))),
						b -> { c.style = c.style.next(); changed(); })
				.bounds(rightX, y, 150, 20).build());
		y += ROW;
		addRenderableWidget(Button.builder(
						label(Component.translatable("cooldowntracker.config.background"), c.background),
						b -> { c.background = !c.background; changed(); })
				.bounds(rightX, y, 150, 20).build());
		y += ROW;
		addRenderableWidget(Button.builder(
						label(Component.translatable("cooldowntracker.config.decimals"), c.showDecimals),
						b -> { c.showDecimals = !c.showDecimals; changed(); })
				.bounds(rightX, y, 150, 20).build());
		y += ROW;
		addRenderableWidget(Button.builder(
						label(Component.translatable("cooldowntracker.config.line_of_sight"), c.onlyWithLineOfSight),
						b -> { c.onlyWithLineOfSight = !c.onlyWithLineOfSight; changed(); })
				.bounds(rightX, y, 150, 20).build());
		y += ROW;
		addRenderableWidget(Button.builder(
						label(Component.translatable("cooldowntracker.config.bobbing"), c.compensateBobbing),
						b -> { c.compensateBobbing = !c.compensateBobbing; changed(); })
				.bounds(rightX, y, 150, 20).build());
		y += ROW;

		// Größe
		addRenderableWidget(Button.builder(
						Component.translatable("cooldowntracker.config.scale", String.format(Locale.ROOT, "%.1f", c.scale)),
						b -> { c.scale = 1.0f; changed(); })
				.bounds(rightX, y, 106, 20).build());
		addRenderableWidget(Button.builder(Component.literal("-"),
						b -> { c.scale = Math.max(0.3f, Math.round((c.scale - 0.1f) * 10) / 10f); changed(); })
				.bounds(rightX + 108, y, 20, 20).build());
		addRenderableWidget(Button.builder(Component.literal("+"),
						b -> { c.scale = Math.min(3.0f, Math.round((c.scale + 0.1f) * 10) / 10f); changed(); })
				.bounds(rightX + 130, y, 20, 20).build());
		y += ROW;

		// Reichweite
		addRenderableWidget(Button.builder(
						Component.translatable("cooldowntracker.config.distance", c.maxDistance),
						b -> { c.maxDistance = 64; changed(); })
				.bounds(rightX, y, 106, 20).build());
		addRenderableWidget(Button.builder(Component.literal("-"),
						b -> { c.maxDistance = Math.max(8, c.maxDistance - 8); changed(); })
				.bounds(rightX + 108, y, 20, 20).build());
		addRenderableWidget(Button.builder(Component.literal("+"),
						b -> { c.maxDistance = Math.min(256, c.maxDistance + 8); changed(); })
				.bounds(rightX + 130, y, 20, 20).build());

		// ---------- Unten
		int bottom = this.height - 26;
		addRenderableWidget(Button.builder(Component.translatable("cooldowntracker.config.keybinds"),
						b -> this.minecraft.setScreen(new KeyBindsScreen(this, this.minecraft.options)))
				.bounds(leftX, bottom, 150, 20).build());
		addRenderableWidget(Button.builder(Component.translatable("gui.done"), b -> onClose())
				.bounds(rightX, bottom, 150, 20).build());
	}

	private static Component label(Component name, boolean on) {
		return Component.empty().append(name).append(": ").append(on
				? Component.translatable("cooldowntracker.on").withStyle(ChatFormatting.GREEN)
				: Component.translatable("cooldowntracker.off").withStyle(ChatFormatting.RED));
	}

	private void changed() {
		ModConfig.save();
		rebuildWidgets();
	}

	@Override
	public void render(GuiGraphics g, int mouseX, int mouseY, float delta) {
		super.render(g, mouseX, mouseY, delta);
		g.drawCenteredString(this.font, this.title, this.width / 2, 10, 0xFFFFFFFF);
		int labelY = TOP + ROW + 1;
		g.drawString(this.font, Component.translatable("cooldowntracker.config.section.cooldowns"),
				this.width / 2 - 155, labelY, 0xFFFFDD55, true);
		g.drawString(this.font, Component.translatable("cooldowntracker.config.section.display"),
				this.width / 2 + 5, labelY, 0xFFFFDD55, true);
	}

	@Override
	public void onClose() {
		ModConfig.save();
		this.minecraft.setScreen(parent);
	}
}
