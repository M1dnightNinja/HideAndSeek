package me.m1dnightninja.hideandseek.fabric.mixin;

import net.minecraft.world.entity.projectile.FireworkRocketEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(FireworkRocketEntity.class)
public interface AccessorFireworkRocketEntity {

    @Invoker
    void callExplode();
}
