package de.cooldowntracker;

import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.joml.Matrix4fc;

/** Kamera-Daten des aktuellen Frames, damit das HUD Weltpositionen exakt auf den Bildschirm projizieren kann. */
public final class CameraCapture {
	public static volatile Vec3 pos;
	public static volatile long posTimeMillis;

	/** Tatsächliches FOV inkl. Sprint-/Effekt-Änderungen (vom GameRenderer-Mixin), -1 = unbekannt. */
	public static volatile float fov = -1f;
	public static volatile long fovTimeMillis;

	/** Kamera-Wackeln (View Bobbing + Schaden-Wackeln), das Minecraft in die Projektion einrechnet. */
	public static volatile Matrix4f bob;
	public static volatile long bobTimeMillis;

	public static void setBob(Matrix4fc m) {
		bob = new Matrix4f(m);
		bobTimeMillis = System.currentTimeMillis();
	}

	private CameraCapture() {}
}
