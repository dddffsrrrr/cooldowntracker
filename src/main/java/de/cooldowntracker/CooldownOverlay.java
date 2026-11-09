package de.cooldowntracker;

import de.cooldowntracker.config.ModConfig;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.joml.Vector4f;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

/**
 * Zeichnet die Cooldowns über den Köpfen der Spieler (über dem Namensschild).
 * Die Weltposition wird jeden Frame exakt so wie von Minecraft (inkl. FOV und Kamera-Wackeln) projiziert.
 */
public final class CooldownOverlay {
	private static final int BG_COLOR = 0x60000000;
	/** Pixelgröße der Schrift in der Welt – gleich wie beim Namensschild. */
	private static final double WORLD_PIXEL = 0.025;
	/** Höhe über dem Hitbox-Oberrand (Namensschild sitzt bei +0.5). */
	private static final double HEIGHT_ABOVE = 0.78;

	private record Entry(CooldownType type, String text, int color) {}

	private record Target(double depth, float sx, float sy, float scale, List<Entry> entries) {}

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
		Matrix4f bob = null;
		if (cfg.compensateBobbing && CameraCapture.bob != null && now - CameraCapture.bobTimeMillis < 250
				&& isPlausibleBob(CameraCapture.bob)) {
			bob = CameraCapture.bob;
		}

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
		Vector4f v = new Vector4f();

		// --- Ziele sammeln
		List<Target> targets = new ArrayList<>();
		for (Player p : mc.level.players()) {
			if (p == mc.player || p == camEntity) continue;
			if (p.isSpectator() || p.isInvisibleTo(mc.player)) continue;
			if (p.distanceTo(camEntity) > cfg.maxDistance) continue;
			if (cfg.onlyWithLineOfSight && !mc.player.hasLineOfSight(p)) continue;

			List<Entry> entries = collect(p, cfg);
			if (entries.isEmpty()) continue;

			Vec3 head = p.getPosition(pt).add(0, p.getBbHeight() + HEIGHT_ABOVE, 0);
			double dx = head.x - cam.x, dy = head.y - cam.y, dz = head.z - cam.z;
			// Kamera-Raum (OpenGL: x rechts, y oben, -z nach vorne)
			v.set((float) (dx * rx + dy * ry + dz * rz),
					(float) (dx * ux + dy * uy + dz * uz),
					(float) -(dx * fx + dy * fy + dz * fz),
					1f);
			if (bob != null) bob.transform(v);
			double depth = -v.z;
			if (depth < 0.1) continue; // hinter der Kamera
			double ndcX = (v.x * focal) / (aspect * depth);
			double ndcY = (v.y * focal) / depth;
			if (Math.abs(ndcX) > 1.3 || Math.abs(ndcY) > 1.3) continue;

			float screenX = (float) ((ndcX + 1.0) * 0.5 * guiW);
			float screenY = (float) ((1.0 - ndcY) * 0.5 * guiH);
			// Gleiche Größe wie das Namensschild (wird mit Entfernung kleiner)
			float scale = (float) (WORLD_PIXEL * guiH * 0.5 * focal / depth) * cfg.scale;
			scale = Math.max(0.3f, Math.min(4f, scale));
			targets.add(new Target(depth, screenX, screenY, scale, entries));
		}
		if (targets.isEmpty()) return;

		// Weit entfernte zuerst, damit nahe Spieler oben liegen
		targets.sort(Comparator.comparingDouble(Target::depth).reversed());
		for (Target t : targets) drawTarget(g, mc.font, cfg, t);
	}

	/**
	 * Das Kamera-Wackeln ist nur eine kleine Drehung/Verschiebung. Sieht die Matrix anders aus
	 * (z.B. weil Minecraft dort mehr hineinrechnet), wird sie ignoriert, statt die Anzeige zu verschieben.
	 */
	private static boolean isPlausibleBob(Matrix4f m) {
		if (!Float.isFinite(m.m00()) || !Float.isFinite(m.m11()) || !Float.isFinite(m.m22())) return false;
		boolean nearRotation = m.m00() > 0.9f && m.m11() > 0.9f && m.m22() > 0.9f
				&& Math.abs(m.m00()) < 1.1f && Math.abs(m.m11()) < 1.1f && Math.abs(m.m22()) < 1.1f;
		boolean noPerspective = Math.abs(m.m03()) < 1e-3f && Math.abs(m.m13()) < 1e-3f
				&& Math.abs(m.m23()) < 1e-3f && Math.abs(m.m33() - 1f) < 1e-3f;
		boolean smallShift = Math.abs(m.m30()) < 0.3f && Math.abs(m.m31()) < 0.3f && Math.abs(m.m32()) < 0.3f;
		return nearRotation && noPerspective && smallShift;
	}

	private static List<Entry> collect(Player p, ModConfig cfg) {
		List<Entry> list = new ArrayList<>(4);
		for (CooldownType type : CooldownType.values()) {
			if (!type.isEnabled(cfg)) continue;
			long rem = CooldownTracker.remaining(p.getUUID(), type);
			if (rem <= 0) continue;
			int color;
			if (cfg.colorByTime) {
				double frac = rem / (type.seconds(cfg) * 1000.0);
				color = frac > 0.5 ? 0xFF5555 : frac > 0.2 ? 0xFFDD55 : 0x55FF55;
			} else {
				color = type.color(cfg);
			}
			list.add(new Entry(type, format(rem, cfg.showDecimals), 0xFF000000 | color));
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
		g.pose().pushMatrix();
		g.pose().translate(t.sx(), t.sy());
		g.pose().scale(t.scale(), t.scale());

		if (cfg.style == ModConfig.Style.ICONS) {
			int[] widths = new int[t.entries().size()];
			int total = 0;
			for (int i = 0; i < widths.length; i++) {
				widths[i] = Math.max(16, font.width(t.entries().get(i).text())) + 4;
				total += widths[i];
			}
			int height = 28;
			int left = -total / 2;
			if (cfg.background) g.fill(left - 1, -height - 1, left + total + 1, 1, BG_COLOR);
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
			// Text-Stil wie "Pearl: 8s" – eine Zeile pro Cooldown, von unten nach oben
			int lineH = 10;
			int n = t.entries().size();
			for (int i = 0; i < n; i++) {
				Entry e = t.entries().get(i);
				String line = e.type().displayName().getString() + ": " + e.text();
				int w = font.width(line);
				int y = -(n - i) * lineH;
				if (cfg.background) g.fill(-w / 2 - 1, y - 1, w / 2 + 1, y + 9, BG_COLOR);
				g.drawString(font, line, -w / 2, y, e.color(), true);
			}
		}

		g.pose().popMatrix();
	}
}
