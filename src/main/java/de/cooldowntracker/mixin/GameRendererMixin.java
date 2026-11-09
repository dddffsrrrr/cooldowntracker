package de.cooldowntracker.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import de.cooldowntracker.CameraCapture;
import net.minecraft.client.Camera;
import net.minecraft.client.renderer.GameRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Liest das echte FOV und das Kamera-Wackeln aus, damit die Anzeige ruhig über dem Kopf sitzt.
 * require = 0: Falls sich eine Methode ändert, läuft die Mod trotzdem weiter.
 */
@Mixin(GameRenderer.class)
public abstract class GameRendererMixin {
	@Inject(method = "getFov(Lnet/minecraft/client/Camera;FZ)F", at = @At("RETURN"), require = 0)
	private void cooldowntracker$captureFov(Camera camera, float partialTick, boolean useFovSetting,
											CallbackInfoReturnable<Float> cir) {
		if (useFovSetting) {
			CameraCapture.fov = cir.getReturnValue();
			CameraCapture.fovTimeMillis = System.currentTimeMillis();
		}
	}

	@Inject(method = "bobHurt(Lcom/mojang/blaze3d/vertex/PoseStack;F)V", at = @At("RETURN"), require = 0)
	private void cooldowntracker$captureHurt(PoseStack poseStack, float partialTick, CallbackInfo ci) {
		CameraCapture.setBob(poseStack.last().pose());
	}

	@Inject(method = "bobView(Lcom/mojang/blaze3d/vertex/PoseStack;F)V", at = @At("RETURN"), require = 0)
	private void cooldowntracker$captureBob(PoseStack poseStack, float partialTick, CallbackInfo ci) {
		CameraCapture.setBob(poseStack.last().pose());
	}
}
