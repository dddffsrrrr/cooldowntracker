package de.cooldowntracker;

import de.cooldowntracker.config.ModConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Erkennt clientseitig, wann andere Spieler eine Wind Charge / Enderperle werfen,
 * mit dem Speer angreifen/aufladen oder einen verzauberten goldenen Apfel essen,
 * und merkt sich, wann der jeweilige Cooldown abläuft.
 */
public final class CooldownTracker {
	/** Spieler-UUID -> (Cooldown -> Endzeit in ms). */
	private static final Map<UUID, EnumMap<CooldownType, Long>> END_TIMES = new HashMap<>();
	/** Pro Spieler: Zustand vom letzten Tick (für Flanken-Erkennung). */
	private static final Map<UUID, PlayerState> STATES = new HashMap<>();

	private static final int GAPPLE_MIN_USE_TICKS = 28; // Essen dauert 32 Ticks
	private static int pruneCounter = 0;

	private static final class PlayerState {
		int gappleUseTicks;
		boolean wasUsingSpear;
		boolean wasSwinging;
		int lastSwingTime;
	}

	private CooldownTracker() {}

	// ------------------------------------------------------------------ Auslösen

	public static void trigger(UUID player, CooldownType type) {
		ModConfig cfg = ModConfig.get();
		long now = System.currentTimeMillis();
		EnumMap<CooldownType, Long> map = END_TIMES.computeIfAbsent(player, k -> new EnumMap<>(CooldownType.class));
		Long end = map.get(type);
		if (!type.restartOnUse && end != null && end > now) return;
		map.put(type, now + type.seconds(cfg) * 1000L);
	}

	/** Restzeit in ms, oder 0 wenn kein Cooldown läuft. */
	public static long remaining(UUID player, CooldownType type) {
		EnumMap<CooldownType, Long> map = END_TIMES.get(player);
		if (map == null) return 0;
		Long end = map.get(type);
		if (end == null) return 0;
		return Math.max(0, end - System.currentTimeMillis());
	}

	public static void clear() {
		END_TIMES.clear();
		STATES.clear();
	}

	// ------------------------------------------------------------------ Projektile

	/** Wird aufgerufen, wenn auf dem Client ein neues Entity geladen wird. */
	public static void onEntityLoad(Entity entity, ClientLevel level) {
		CooldownType type;
		if (entity.getType() == EntityType.ENDER_PEARL) type = CooldownType.ENDER_PEARL;
		else if (entity.getType() == EntityType.WIND_CHARGE) type = CooldownType.WIND_CHARGE;
		else return;

		Minecraft mc = Minecraft.getInstance();
		Player thrower = null;

		// 1) Besitzer steht im Spawn-Paket des Projektils
		if (entity instanceof Projectile projectile) {
			Entity owner = projectile.getOwner();
			if (owner instanceof Player p) thrower = p;
			else if (owner != null) return; // z.B. Breeze -> kein Spieler
		}
		// 2) Fallback: nächster Spieler direkt beim Projektil
		if (thrower == null) {
			double best = 3.5 * 3.5;
			for (Player p : level.players()) {
				double d = p.getEyePosition().distanceToSqr(entity.position());
				if (d < best) {
					best = d;
					thrower = p;
				}
			}
		}
		if (thrower == null || thrower == mc.player) return;

		// Nur frische Würfe zählen (nicht Perlen, die aus der Ferne in Sichtweite fliegen)
		if (thrower.getEyePosition().distanceToSqr(entity.position()) > 6.0 * 6.0) return;

		trigger(thrower.getUUID(), type);
	}

	// ------------------------------------------------------------------ Tick

	public static void tick(Minecraft mc) {
		if (mc.level == null || mc.player == null) return;
		ModConfig cfg = ModConfig.get();

		for (Player p : mc.level.players()) {
			if (p == mc.player) continue;
			UUID id = p.getUUID();
			PlayerState s = STATES.computeIfAbsent(id, k -> new PlayerState());

			// --- Verzauberter goldener Apfel: Essen bis zum Ende
			boolean eatingGapple = p.isUsingItem() && p.getUseItem().is(Items.ENCHANTED_GOLDEN_APPLE);
			if (eatingGapple) {
				s.gappleUseTicks++;
			} else {
				if (s.gappleUseTicks >= GAPPLE_MIN_USE_TICKS) trigger(id, CooldownType.ENCHANTED_GOLDEN_APPLE);
				s.gappleUseTicks = 0;
			}

			// --- Speer: Aufladen (Rechtsklick halten)
			boolean usingSpear = p.isUsingItem() && isSpear(p.getUseItem());
			if (usingSpear && !s.wasUsingSpear && cfg.spearTrigger != ModConfig.SpearTrigger.ATTACK) {
				trigger(id, CooldownType.SPEAR);
			}
			s.wasUsingSpear = usingSpear;

			// --- Speer: Angriff (Schwung mit Speer in der Haupthand)
			boolean newSwing = p.swinging && (!s.wasSwinging || p.swingTime < s.lastSwingTime);
			if (newSwing && p.swingingArm == InteractionHand.MAIN_HAND && isSpear(p.getMainHandItem())
					&& cfg.spearTrigger != ModConfig.SpearTrigger.CHARGE) {
				trigger(id, CooldownType.SPEAR);
			}
			s.wasSwinging = p.swinging;
			s.lastSwingTime = p.swingTime;
		}

		// Ab und zu aufräumen
		if (++pruneCounter >= 200) {
			pruneCounter = 0;
			Set<UUID> present = new HashSet<>();
			for (Player p : mc.level.players()) present.add(p.getUUID());
			STATES.keySet().retainAll(present);
			long now = System.currentTimeMillis();
			Iterator<Map.Entry<UUID, EnumMap<CooldownType, Long>>> it = END_TIMES.entrySet().iterator();
			while (it.hasNext()) {
				EnumMap<CooldownType, Long> m = it.next().getValue();
				m.values().removeIf(end -> end <= now);
				if (m.isEmpty()) it.remove();
			}
		}
	}

	public static boolean isSpear(ItemStack stack) {
		if (stack.isEmpty()) return false;
		return BuiltInRegistries.ITEM.getKey(stack.getItem()).getPath().endsWith("_spear");
	}
}
