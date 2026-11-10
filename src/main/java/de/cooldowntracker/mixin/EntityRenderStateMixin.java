package de.cooldowntracker.mixin;

import de.cooldowntracker.CooldownRenderData;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

import java.util.UUID;

@Mixin(EntityRenderState.class)
public abstract class EntityRenderStateMixin implements CooldownRenderData {
	@Unique
	private UUID cooldowntracker$player;

	@Override
	public UUID cooldowntracker$getPlayer() {
		return cooldowntracker$player;
	}

	@Override
	public void cooldowntracker$setPlayer(UUID uuid) {
		cooldowntracker$player = uuid;
	}
}
