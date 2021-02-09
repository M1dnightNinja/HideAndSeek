package me.m1dnightninja.hideandseek.fabric.util;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import me.m1dnightninja.midnightcore.api.config.ConfigSection;
import net.minecraft.core.Registry;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.TagParser;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.Optional;

public final class ParseUtil {

    public static ItemStack parseItemStack(ConfigSection sec) {
        if (!sec.has("id", String.class)) return null;

        ResourceLocation id = new ResourceLocation(sec.get("id", String.class));
        Optional<Item> i = Registry.ITEM.getOptional(id);
        if (!i.isPresent()) return null;

        ItemStack is = new ItemStack(i.get());
        if (sec.has("tag", String.class)) {
            try {
                CompoundTag tag = (new TagParser(new StringReader(sec.get("tag", String.class)))).readStruct();
                is.setTag(tag);
            } catch (CommandSyntaxException var5) {
                var5.printStackTrace();
            }
        }

        if (sec.has("amount", Number.class)) {
            is.setCount(Mth.clamp((int) sec.get("amount", Number.class), 1, 64));
        }

        return is;

    }

}
