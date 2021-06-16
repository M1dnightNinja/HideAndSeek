package me.m1dnightninja.hideandseek.spigot.util;

import me.m1dnightninja.hideandseek.api.game.DamageSource;
import org.bukkit.event.entity.EntityDamageEvent;

public class ConversionUtil {

    public static DamageSource toDamageSource(EntityDamageEvent.DamageCause cause) {

        switch (cause) {
            case SUFFOCATION:
                return DamageSource.SUFFOCATE;
            case FALL:
                return DamageSource.FALL;
            case FIRE:
                return DamageSource.IN_FIRE;
            case FIRE_TICK:
                return DamageSource.FIRE;
            case MELTING:
            case LAVA:
                return DamageSource.LAVA;
            case DROWNING:
                return DamageSource.DROWN;
            case VOID:
                return DamageSource.VOID;
            case LIGHTNING:
                return DamageSource.LIGHTNING;
            case SUICIDE:
            case MAGIC:
            case POISON:
                return DamageSource.MAGIC;
            case STARVATION:
                return DamageSource.STARVE;
            case WITHER:
                return DamageSource.WITHER;
            case FALLING_BLOCK:
                return DamageSource.FALLING_BLOCK;
            case DRAGON_BREATH:
                return DamageSource.DRAGON_BREATH;
            case FLY_INTO_WALL:
                return DamageSource.FLY_INTO_WALL;
            case HOT_FLOOR:
                return DamageSource.MAGMA;
            case CRAMMING:
                return DamageSource.CRAMMING;
            case DRYOUT:
                return DamageSource.DRY_OUT;
            case FREEZE:
                return DamageSource.FREEZE;
            default:
                return DamageSource.GENERIC;
        }

    }


}
