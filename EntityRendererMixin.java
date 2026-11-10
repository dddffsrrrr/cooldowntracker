package de.cooldowntracker.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import de.cooldowntracker.CooldownWorldRenderer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.state.CameraRenderState;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Hängt sich in Minecrafts eigenes Entity-Rendering: Die Cooldowns werden genau wie das
 * Namensschild in der Welt gezeichnet. Dadurch sitzen sie immer am Kopf – auch mit Freelook,
 * Freecam, Third Person, beim Laufen usw.
 */
@Mixin(EntityRenderer.class)
public abstract class EntityRendererMixin<T extends Entity, S extends EntityRenderState> {

	@Inject(method = "extractRenderState(Lnet/minecraft/world/entity/Entity;Lnet/minecraft/client/renderer/entity/state/EntityRenderState;F)V",
			at = @At("TAIL"))
	private void cooldowntracker$extract(T entity, S state, float partialTick, CallbackInfo ci) {
		CooldownWorldRenderer.extract(entity, state);
	}

	@Inject(method = "submit(Lnet/minecraft/client/renderer/entity/state/EntityRenderState;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;Lnet/minecraft/client/renderer/state/CameraRenderState;)V",
			at = @At("HEAD"))
	private void cooldowntracker$submit(S state, PoseStack poseStack, SubmitNodeCollector collector,
										CameraRenderState camera, CallbackInfo ci) {
		CooldownWorldRenderer.submit(state, poseStack, collector, camera);
	}
}
