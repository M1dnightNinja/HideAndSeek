package me.m1dnightninja.hideandseek.fabric;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import me.m1dnightninja.hideandseek.fabric.util.ParseUtil;
import me.m1dnightninja.hideandseek.api.AbstractClass;
import me.m1dnightninja.hideandseek.api.HideAndSeekAPI;
import me.m1dnightninja.hideandseek.api.PositionType;
import me.m1dnightninja.hideandseek.api.SkinOption;
import me.m1dnightninja.midnightcore.api.MidnightCoreAPI;
import me.m1dnightninja.midnightcore.api.module.ISkinModule;
import me.m1dnightninja.midnightcore.fabric.MidnightCore;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class GameClass extends AbstractClass {

    private final HashMap<MobEffect, Integer> effects = new HashMap<>();
    private final HashMap<EquipmentSlot, ItemStack> equipment = new HashMap<>();
    private final List<ItemStack> items = new ArrayList<>();

    public GameClass(String id) {
        super(id);
    }

    @Override
    public void applyToPlayer(UUID uid, SkinOption option) {



        ServerPlayer player = MidnightCore.getServer().getPlayerList().getPlayer(uid);
        if(player == null) return;

        player.removeAllEffects();
        for(Map.Entry<MobEffect, Integer> ent : effects.entrySet()) {
            player.addEffect(new MobEffectInstance(ent.getKey(), Integer.MAX_VALUE, ent.getValue(), true, false, false));
        }

        for(Map.Entry<EquipmentSlot, ItemStack> ent : equipment.entrySet()) {
            if(ent.getKey().getType() == EquipmentSlot.Type.ARMOR) {
                player.inventory.armor.set(ent.getKey().getIndex(), ent.getValue());
            } else if(ent.getKey() == EquipmentSlot.OFFHAND) {
                player.setItemInHand(InteractionHand.OFF_HAND, ent.getValue());
            } else {
                player.setItemInHand(InteractionHand.MAIN_HAND, ent.getValue());
            }
        }

        for(ItemStack is : items) {
            player.inventory.add(is);
        }

        if(option == null && skins.size() > 0) option = skins.get(HideAndSeekAPI.getInstance().getRandom().nextInt(skins.size()));

        if(option != null) {
            ISkinModule mod = MidnightCoreAPI.getInstance().getModule(ISkinModule.class);
            mod.setSkin(player.getUUID(), option.getSkin());
            mod.updateSkin(player.getUUID());
        }

    }

    public static GameClass parse(JsonObject obj) {

        String id = obj.get("id").getAsString();

        GameClass out = new GameClass(id);

        if(obj.has("skins") && obj.get("skins").isJsonArray()) {
            for(JsonElement ele : obj.getAsJsonArray("skins")) {
                SkinOption opt = HideAndSeekAPI.getInstance().getRegistry().getSkin(ele.getAsString());
                if(opt != null) out.skins.add(opt);
            }
        }

        if(obj.has("effects") && obj.get("effects").isJsonObject()) {
            JsonObject effects = obj.get("effects").getAsJsonObject();

            for(Map.Entry<String, JsonElement> ele : effects.entrySet()) {

                if(!ele.getValue().isJsonPrimitive()) continue;
                if(!ele.getValue().getAsJsonPrimitive().isNumber()) continue;

                int amplifier = ele.getValue().getAsInt();
                MobEffect eff = Registry.MOB_EFFECT.get(new ResourceLocation(ele.getKey()));

                out.effects.put(eff, amplifier);
            }
        }

        if(obj.has("equipment") && obj.get("equipment").isJsonObject()) {
            JsonObject equip = obj.get("equipment").getAsJsonObject();

            for(Map.Entry<String, JsonElement> ele : equip.entrySet()) {
                EquipmentSlot slot = EquipmentSlot.byName(ele.getKey());
                if(slot == null || !ele.getValue().isJsonObject()) continue;

                ItemStack is = ParseUtil.parseItemStack(ele.getValue().getAsJsonObject());
                if(is == null) continue;

                out.equipment.put(slot, is);
            }
        }

        if(obj.has("items") && obj.get("items").isJsonArray()) {
            for(JsonElement ele : obj.getAsJsonArray("items")) {
                if(!ele.isJsonObject()) continue;
                out.items.add(ParseUtil.parseItemStack(ele.getAsJsonObject()));
            }
        }

        if(obj.has("equivalencies") && obj.get("equivalencies").isJsonObject()) {
            JsonObject equip = obj.get("equivalencies").getAsJsonObject();

            for(Map.Entry<String, JsonElement> ele : equip.entrySet()) {

                for(PositionType type : PositionType.values()) {

                    if(ele.getKey().equals(type.getId())) {
                        out.tempEquivalencies.put(type, ele.getValue().getAsString());
                    }
                }
            }
        }

        return out;
    }
}
