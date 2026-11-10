package de.cooldowntracker;

import java.util.UUID;

/** Wird per Mixin an jeden EntityRenderState gehängt: welcher Spieler gerade gezeichnet wird. */
public interface CooldownRenderData {
	UUID cooldowntracker$getPlayer();

	void cooldowntracker$setPlayer(UUID uuid);
}
