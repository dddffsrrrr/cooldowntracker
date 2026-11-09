package de.cooldowntracker;

import de.cooldowntracker.config.ModConfig;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

/**
 * Zeichnet die Cooldowns über den Köpfen der Spieler.
 * Die Weltposition über dem Kopf wird jeden Frame auf den Bildschirm projiziert.
 */
public final class CooldownOverlay {
	private static final int BG_COLOR = 0x90000000;

	private record Entry(CooldownType type, long remainingMs, String text, int color) {}

	private record Target(Player player, double distance, float sx, float sy, List<Entry> entries) {}

	private CooldownOverlay() {}

	public static void render(GuiGraphics g, DeltaTracker deltaTracker) {
		ModConfig cfg = ModConfig.get();
		if (!cfg.enabled) return;
		Minecraft mc = Minecraft.getInstance();
		if (mc.level == null || mc.player == null || mc.options.hideGui) return;
		Entity camEntity = mc.getCameraEntity();
		if (camEntity == null) return;

		float pt = deltaTracker.getGameTimeDeltaPartialTick(false);
		long now = System.currentTimeMillis();

		// --- Kamera
		Vec3 cam = CameraCapture.pos;
		if (cam == null || now - CameraCapture.posTimeMillis > 250 || mc.options.getCameraType().isFirstPerson()) {
			cam = camEntity.getEyePosition(pt);
		}
		float yaw = camEntity.getViewYRot(pt);
		float pitch = camEntity.getViewXRot(pt);
		if (mc.options.getCameraType().isMirrored()) {
			yaw += 180f;
			pitch = -pitch;
		}
		float fov = (CameraCapture.fov > 0 && now - CameraCapture.fovTimeMillis < 250)
				? CameraCapture.fov
				: mc.options.fov().get().floatValue();

		double yawR = Math.toRadians(yaw), pitchR = Math.toRadians(pitch);
		double cy = Math.cos(yawR), sy = Math.sin(yawR), cp = Math.cos(pitchR), sp = Math.sin(pitchR);
		// Blickrichtung, rechts, oben (Minecraft: yaw 0 = +Z)
		double fx = -sy * cp, fy = -sp, fz = cy * cp;
		double rx = -cy, ry = 0, rz = -sy;
		double ux = ry * fz - rz * fy, uy = rz * fx - rx * fz, uz = rx * fy - ry * fx;

		int winW = mc.getWindow().getWidth(), winH = mc.getWindow().getHeight();
		if (winW <= 0 || winH <= 0) return;
		double aspect = (double) winW / winH;
		double focal = 1.0 / Math.tan(Math.toRadians(fov) / 2.0);
		int guiW = g.guiWidth(), guiH = g.guiHeight();

		// --- Ziele sammeln
		List<Target> targets = new ArrayList<>();
		for (Player p : mc.level.players()) {
			if (p == mc.player || p == camEntity) continue;
			if (p.isSpectator() || p.isInvisibleTo(mc.player)) continue;
			double dist = p.distanceTo(camEntity);
			if (dist > cfg.maxDistance) continue;
			if (cfg.onlyWithLineOfSight && !mc.player.hasLineOfSight(p)) continue;

			List<Entry> entries = collect(p, cfg);
			if (entries.isEmpty()) continue;

			Vec3 head = p.getPosition(pt).add(0, p.getBbHeight() + 0.8, 0);
			double dx = head.x - cam.x, dy = head.y - cam.y, dz = head.z - cam.z;
			double z = dx * fx + dy * fy + dz * fz;
			if (z < 0.1) continue; // hinter der Kamera
			double x = dx * rx + dy * ry + dz * rz;
			double y = dx * ux + dy * uy + dz * uz;
			double ndcX = (x * focal) / (aspect * z);
			double ndcY = (y * focal) / z;
			if (Math.abs(ndcX) > 1.3 || Math.abs(ndcY) > 1.3) continue;
			float screenX = (float) ((ndcX + 1.0) * 0.5 * guiW);
			float screenY = (float) ((1.0 - ndcY) * 0.5 * guiH);
			targets.add(new Target(p, dist, screenX, screenY, entries));
		}
		if (targets.isEmpty()) return;

		// Weit entfernte zuerst, damit nahe Spieler oben liegen
		targets.sort(Comparator.comparingDouble(Target::distance).reversed());
		for (Target t : targets) drawTarget(g, mc.font, cfg, t);
	}

	private static List<Entry> collect(Player p, ModConfig cfg) {
		List<Entry> list = new ArrayList<>(4);
		for (CooldownType type : CooldownType.values()) {
			if (!type.isEnabled(cfg)) continue;
			long rem = CooldownTracker.remaining(p.getUUID(), type);
			if (rem <= 0) continue;
			double frac = rem / (type.seconds(cfg) * 1000.0);
			int color = frac > 0.5 ? 0xFFFF5555 : frac > 0.2 ? 0xFFFFDD55 : 0xFF55FF55;
			list.add(new Entry(type, rem, format(rem, cfg.showDecimals), color));
		}
		return list;
	}

	private static String format(long ms, boolean decimals) {
		if (ms >= 60_000) {
			long totalSec = (ms + 999) / 1000;
			return String.format(Locale.ROOT, "%d:%02d", totalSec / 60, totalSec % 60);
		}
		if (decimals && ms < 10_000) return String.format(Locale.ROOT, "%.1fs", ms / 1000.0);
		return ((ms + 999) / 1000) + "s";
	}

	private static void drawTarget(GuiGraphics g, Font font, ModConfig cfg, Target t) {
		float distScale = (float) Math.max(0.45, Math.min(1.25, 7.0 / Math.max(t.distance(), 0.1)));
		float s = cfg.scale * distScale;

		g.pose().pushMatrix();
		g.pose().translate(t.sx(), t.sy());
		g.pose().scale(s, s);

		if (cfg.showIcons) {
			int[] widths = new int[t.entries().size()];
			int total = 0;
			for (int i = 0; i < widths.length; i++) {
				widths[i] = Math.max(16, font.width(t.entries().get(i).text())) + 4;
				total += widths[i];
			}
			int height = 28;
			int left = -total / 2;
			g.fill(left - 1, -height - 1, left + total + 1, 1, BG_COLOR);
			int x = left;
			for (int i = 0; i < widths.length; i++) {
				Entry e = t.entries().get(i);
				int cellCenter = x + widths[i] / 2;
				g.renderItem(e.type().icon(), cellCenter - 8, -height + 1);
				int tw = font.width(e.text());
				g.drawString(font, e.text(), cellCenter - tw / 2, -9, e.color(), true);
				x += widths[i];
			}
		} else {
			List<String> lines = new ArrayList<>();
			int maxW = 0;
			for (Entry e : t.entries()) {
				String line = e.type().displayName().getString() + ": " + e.text();
				lines.add(line);
				maxW = Math.max(maxW, font.width(line));
			}
			int lineH = 10;
			int height = lines.size() * lineH;
			g.fill(-maxW / 2 - 2, -height - 2, maxW / 2 + 2, 1, BG_COLOR);
			for (int i = 0; i < lines.size(); i++) {
				String line = lines.get(i);
				g.drawString(font, line, -font.width(line) / 2, -height + i * lineH, t.entries().get(i).color(), true);
			}
		}

		g.pose().popMatrix();
	}
}
