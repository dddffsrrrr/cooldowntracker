package de.cooldowntracker.mixin;

import de.cooldowntracker.CameraCapture;
import net.minecraft.client.Camera;
import net.minecraft.client.renderer.GameRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Liest das echte FOV (inkl. Sprinten, Bogen, Effekte) aus, damit die Anzeige exakt über dem Kopf sitzt.
 * require = 0: Falls sich die Methode ändert, läuft die Mod trotzdem (dann wird das FOV aus den Optionen genutzt).
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
}
