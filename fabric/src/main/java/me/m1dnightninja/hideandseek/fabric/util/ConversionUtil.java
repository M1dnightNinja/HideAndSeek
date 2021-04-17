package me.m1dnightninja.hideandseek.fabric.util;

import me.m1dnightninja.hideandseek.api.game.DamageSource;


public final class ConversionUtil {

    private static final DamageSource[] midnight = { DamageSource.IN_FIRE, DamageSource.LIGHTNING, DamageSource.FIRE, DamageSource.LAVA, DamageSource.MAGMA, DamageSource.SUFFOCATE, DamageSource.CRAMMING, DamageSource.DROWN, DamageSource.STARVE, DamageSource.CACTUS, DamageSource.FALL, DamageSource.FLY_INTO_WALL, DamageSource.VOID, DamageSource.GENERIC, DamageSource.MAGIC, DamageSource.WITHER, DamageSource.ANVIL, DamageSource.FALLING_BLOCK, DamageSource.DRAGON_BREATH, DamageSource.DRY_OUT, DamageSource.SWEET_BERRY_BUSH };
    private static final net.minecraft.world.damagesource.DamageSource[] minecraft = { net.minecraft.world.damagesource.DamageSource.IN_FIRE, net.minecraft.world.damagesource.DamageSource.LIGHTNING_BOLT, net.minecraft.world.damagesource.DamageSource.ON_FIRE, net.minecraft.world.damagesource.DamageSource.LAVA, net.minecraft.world.damagesource.DamageSource.HOT_FLOOR, net.minecraft.world.damagesource.DamageSource.IN_WALL, net.minecraft.world.damagesource.DamageSource.CRAMMING, net.minecraft.world.damagesource.DamageSource.DROWN, net.minecraft.world.damagesource.DamageSource.STARVE, net.minecraft.world.damagesource.DamageSource.CACTUS, net.minecraft.world.damagesource.DamageSource.FALL, net.minecraft.world.damagesource.DamageSource.FLY_INTO_WALL, net.minecraft.world.damagesource.DamageSource.OUT_OF_WORLD, net.minecraft.world.damagesource.DamageSource.GENERIC, net.minecraft.world.damagesource.DamageSource.MAGIC, net.minecraft.world.damagesource.DamageSource.WITHER, net.minecraft.world.damagesource.DamageSource.ANVIL, net.minecraft.world.damagesource.DamageSource.FALLING_BLOCK, net.minecraft.world.damagesource.DamageSource.DRAGON_BREATH, net.minecraft.world.damagesource.DamageSource.DRY_OUT, net.minecraft.world.damagesource.DamageSource.SWEET_BERRY_BUSH };
    
    public static DamageSource convertDamageSource(net.minecraft.world.damagesource.DamageSource src) {

        for(int i = 0 ; i < minecraft.length ; i++) {
            net.minecraft.world.damagesource.DamageSource out = minecraft[i];
            if(out == src) {
                return midnight[i];
            }
        }

        return DamageSource.GENERIC;

    }

    public static net.minecraft.world.damagesource.DamageSource convertDamageSource(DamageSource src) {

        for(int i = 0 ; i < midnight.length ; i++) {
            DamageSource out = midnight[i];
            if(out == src) {
                return minecraft[i];
            }
        }

        return net.minecraft.world.damagesource.DamageSource.GENERIC;

    }

}
