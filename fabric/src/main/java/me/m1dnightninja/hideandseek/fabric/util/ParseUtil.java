package me.m1dnightninja.hideandseek.fabric.util;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import me.m1dnightninja.hideandseek.api.PositionType;
import me.m1dnightninja.hideandseek.api.Region;
import me.m1dnightninja.hideandseek.api.SkinOption;
import me.m1dnightninja.hideandseek.fabric.mixin.AccessorTextColor;
import me.m1dnightninja.midnightcore.api.Color;
import me.m1dnightninja.midnightcore.api.math.Vec3d;
import me.m1dnightninja.midnightcore.api.skin.Skin;
import net.minecraft.ChatFormatting;
import net.minecraft.core.Registry;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.TagParser;
import net.minecraft.network.chat.TextColor;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.Optional;
import java.util.UUID;

public final class ParseUtil {

    public static Vec3d parseLocation(String str) {
        if(str == null || str.length() == 0 || !str.contains(",")) return null;
        String[] xyz = str.split(",");

        try {
            double x = Double.parseDouble(xyz[0]);
            double y = Double.parseDouble(xyz[1]);
            double z = Double.parseDouble(xyz[2]);

            return new Vec3d(x,y,z);
        } catch (NumberFormatException ex) {
            return null;
        }

    }

    public static ItemStack parseItemStack(JsonObject obj) {
        if (!obj.has("id")) {
            return null;
        } else {
            ResourceLocation id = new ResourceLocation(obj.get("id").getAsString());
            Optional<Item> i = Registry.ITEM.getOptional(id);
            if (!i.isPresent()) {
                return null;
            } else {
                ItemStack is = new ItemStack(i.get());
                if (obj.has("tag")) {
                    try {
                        CompoundTag tag = (new TagParser(new StringReader(obj.getAsJsonObject("tag").toString()))).readStruct();
                        is.setTag(tag);
                    } catch (CommandSyntaxException var5) {
                        var5.printStackTrace();
                    }
                }

                if (obj.has("amount")) {
                    is.setCount(Mth.clamp(obj.get("amount").getAsInt(), 1, 64));
                }

                return is;
            }
        }
    }

    public static Color parseColor(String str) {
        ChatFormatting fmt = ChatFormatting.getByName(str);
        TextColor clr = fmt == null ? null : TextColor.fromLegacyFormat(fmt);
        if(clr == null) {
            return new Color(str);
        } else {
            return new Color(((AccessorTextColor) (Object) clr).callFormatValue());
        }
    }

    public static SkinOption parseSkinOption(JsonObject obj) {

        if(obj == null) return null;

        UUID u = UUID.fromString(obj.get("uid").getAsString());
        String b64 = obj.get("b64").getAsString();
        String sig = obj.get("sig").getAsString();
        String id = obj.get("id").getAsString();

        SkinOption out = new SkinOption(id, new Skin(u,b64,sig));

        if(obj.has("name")) {
            out.setDisplayName(obj.get("name").getAsString());
        }

        return out;
    }

    public static Region parseRegion(JsonObject obj) {

        String id = obj.get("id").getAsString();
        Vec3d pos = parseLocation(obj.get("position").getAsString());
        Vec3d size = parseLocation(obj.get("size").getAsString());

        Region out = new Region(id, pos, size);

        if(obj.has("name")) {
            out.setDisplay(obj.get("name").getAsString());
        }

        if(obj.has("denied") && obj.get("denied").isJsonArray()) {

            for(JsonElement ele : obj.get("denied").getAsJsonArray()) {
                PositionType t = PositionType.getById(ele.getAsString());
                if(t != null) out.getDenied().add(t);
            }

        }

        return out;
    }

}
