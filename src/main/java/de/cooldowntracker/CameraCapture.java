package de.cooldowntracker;

import net.minecraft.world.phys.Vec3;

/** Kamera-Daten des letzten Frames, damit das HUD Weltpositionen auf den Bildschirm projizieren kann. */
public final class CameraCapture {
	public static volatile Vec3 pos;
	public static volatile long posTimeMillis;
	/** Tatsächliches FOV inkl. Sprint-/Effekt-Änderungen (vom GameRenderer-Mixin), -1 = unbekannt. */
	public static volatile float fov = -1f;
	public static volatile long fovTimeMillis;

	private CameraCapture() {}
}
