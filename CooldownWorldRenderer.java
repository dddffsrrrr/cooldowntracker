package de.cooldowntracker;

import com.mojang.blaze3d.vertex.PoseStack;
import de.cooldowntracker.config.ModConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.state.CameraRenderState;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.scores.DisplaySlot;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

/**
 * Zeichnet "Pearl: 8s" usw. in der Welt über dem Namensschild – mit derselben Technik wie Minecraft
 * selbst das Namensschild zeichnet (zur Kamera gedreht, an der Spielerposition verankert).
 */
public final class CooldownWorldRenderer {
	/** Größe eines Schrift-Pixels in Blöcken (wie beim Namensschild). */
	private static final float PIXEL = 0.025f;
	/** Abstand einer Zeile in Schrift-Pixeln. */
	private static final int LINE = 10;
	/** Volle Helligkeit, damit die Farben auch im Dunkeln stimmen. */
	private static final int FULL_BRIGHT = 0xF000F0;

	private CooldownWorldRenderer() {}

	/** Beim Auslesen des Entities: merken, ob (und welcher) Spieler gezeichnet werden soll. */
	public static void extract(Entity entity, EntityRenderState state) {
		CooldownRenderData data = (CooldownRenderData) (Object) state;
		data.cooldowntracker$setPlayer(null);

		ModConfig cfg = ModConfig.get();
		if (!cfg.enabled || !(entity instanceof Player player)) return;
		Minecraft mc = Minecraft.getInstance();
		if (mc.player == null || player == mc.player) return;
		if (player.isSpectator() || player.isInvisibleTo(mc.player)) return;
		if (state.distanceToCameraSq > (double) cfg.maxDistance * cfg.maxDistance) return;
		if (cfg.onlyWithLineOfSight && !mc.player.hasLineOfSight(player)) return;

		data.cooldowntracker$setPlayer(player.getUUID());
	}

	/** Beim Zeichnen des Entities: Cooldown-Zeilen über dem Namen abgeben. */
	public static void submit(EntityRenderState state, PoseStack poseStack, SubmitNodeCollector collector,
							  CameraRenderState camera) {
		UUID id = ((CooldownRenderData) (Object) state).cooldowntracker$getPlayer();
		if (id == null) return;
		ModConfig cfg = ModConfig.get();
		if (!cfg.enabled) return;

		List<Component> lines = new ArrayList<>(4);
		List<Integer> colors = new ArrayList<>(4);
		for (CooldownType type : CooldownType.values()) {
			if (!type.isEnabled(cfg)) continue;
			long rem = CooldownTracker.remaining(id, type);
			if (rem <= 0) continue;
			int color;
			if (cfg.colorByTime) {
				double frac = rem / (type.seconds(cfg) * 1000.0);
				color = frac > 0.5 ? 0xFF5555 : frac > 0.2 ? 0xFFDD55 : 0x55FF55;
			} else {
				color = type.color(cfg);
			}
			lines.add(Component.literal(type.displayName().getString() + ": " + format(rem, cfg.showDecimals)));
			colors.add(0xFF000000 | color);
		}
		if (lines.isEmpty()) return;

		Minecraft mc = Minecraft.getInstance();
		Font font = mc.font;

		// Verankerung: gleiche Stelle wie das Namensschild (+0.5 wie Vanilla)
		Vec3 attach = state.nameTagAttachment != null ? state.nameTagAttachment : new Vec3(0, state.boundingBoxHeight, 0);
		double up = attach.y + 0.5;
		boolean nameShown = state.nameTag != null;
		// Zeigt der Server eine Zeile unter dem Namen (z.B. HP), rutscht der Name eine Zeile nach oben
		if (nameShown && mc.level != null && mc.level.getScoreboard().getDisplayObjective(DisplaySlot.BELOW_NAME) != null) {
			up += 9 * 1.15f * PIXEL;
		}

		float scale = PIXEL * cfg.scale;
		Font.DisplayMode mode = cfg.seeThroughWalls ? Font.DisplayMode.SEE_THROUGH : Font.DisplayMode.NORMAL;
		int background = cfg.background ? 0x40000000 : 0;

		poseStack.pushPose();
		poseStack.translate(attach.x, up, attach.z);
		poseStack.mulPose(camera.orientation);
		poseStack.scale(scale, -scale, scale);

		// Zeile 0 direkt über dem Namen (Unterkante 1 Pixel über dem Namen), weitere darüber.
		// Ohne sichtbaren Namen steht Zeile 0 dort, wo der Name wäre.
		for (int i = 0; i < lines.size(); i++) {
			Component line = lines.get(i);
			float x = -font.width(line) / 2f;
			float y = nameShown ? -(1f / cfg.scale) - 9 - i * LINE : -i * LINE;
			collector.submitText(poseStack, x, y, line.getVisualOrderText(), true, mode,
					FULL_BRIGHT, colors.get(i), background, 0);
		}
		poseStack.popPose();
	}

	private static String format(long ms, boolean decimals) {
		if (ms >= 60_000) {
			long totalSec = (ms + 999) / 1000;
			return String.format(Locale.ROOT, "%d:%02d", totalSec / 60, totalSec % 60);
		}
		if (decimals && ms < 10_000) return String.format(Locale.ROOT, "%.1fs", ms / 1000.0);
		return ((ms + 999) / 1000) + "s";
	}
}
