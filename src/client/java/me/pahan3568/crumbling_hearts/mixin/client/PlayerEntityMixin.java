package me.pahan3568.crumbling_hearts.mixin.client;

import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.player.PlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(PlayerEntity.class)
public class PlayerEntityMixin {
    @Inject(method = "getHeartColor", at = @At("HEAD"), cancellable = true)
    private void onGetHeartColor(CallbackInfoReturnable<Integer> cir) {
        PlayerEntity player = (PlayerEntity)(Object)this;
        
        if (player.hasStatusEffect(StatusEffects.WITHER)) {
            // Темно-серый цвет для эффекта иссушения
            cir.setReturnValue(0x1F1F23);
        } else if (player.hasStatusEffect(StatusEffects.POISON)) {
            // Темно-зеленый цвет для эффекта отравления
            cir.setReturnValue(0x4E9331);
        }
    }
} 