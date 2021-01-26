package me.m1dnightninja.hideandseek.fabric.util;

import me.m1dnightninja.hideandseek.fabric.mixin.AccessorFireworkRocketEntity;
import me.m1dnightninja.midnightcore.api.Color;
import me.m1dnightninja.midnightcore.fabric.api.Location;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.IntArrayTag;
import net.minecraft.nbt.IntTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.projectile.FireworkRocketEntity;
import net.minecraft.world.item.FireworkRocketItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.List;

public final class FireworkUtil {

    public static void spawnFireworkExplosion(List<Color> color, List<Color> fadeColor, FireworkRocketItem.Shape type, Location location) {

        FireworkRocketEntity ent = spawnFireworkEntity(color,fadeColor,type,location);

        ent.level.broadcastEntityEvent(ent, (byte) 17);
        ((AccessorFireworkRocketEntity) ent).callExplode();
        ent.remove();
    }

    public static FireworkRocketEntity spawnFireworkEntity(List<Color> color, List<Color> fadeColor, FireworkRocketItem.Shape type, Location location) {
        CompoundTag fireworkTag = new CompoundTag();
        CompoundTag fireworks = new CompoundTag();

        ListTag explosions = new ListTag();

        CompoundTag firework1 = new CompoundTag();
        firework1.put("Colors", generateColorArray(color));
        firework1.put("FadeColors", generateColorArray(fadeColor));
        firework1.put("Type", IntTag.valueOf(type.getId()));

        explosions.add(firework1);

        fireworks.put("Explosions", explosions);
        fireworkTag.put("Fireworks", fireworks);
        fireworkTag.put("LifeTime", IntTag.valueOf(30));

        location.setY(location.getY() + 1.5);

        ItemStack fireworkItem = new ItemStack(Items.FIREWORK_ROCKET, 1);
        fireworkItem.setTag(fireworkTag);

        ServerLevel world = location.getWorld();

        FireworkRocketEntity fwk = new FireworkRocketEntity(world, location.getX(), location.getY(), location.getZ(), fireworkItem);
        world.addFreshEntity(fwk);

        return fwk;
    }

    private static IntArrayTag generateColorArray(List<Color> colorList) {
        int[] colors = new int[colorList.size()];
        for (int i = 0; i < colorList.size(); i++) {
            colors[i] = colorList.get(i).toDecimal();
        }

        return new IntArrayTag(colors);
    }

}
