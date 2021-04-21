package me.m1dnightninja.hideandseek.fabric.game;

import me.m1dnightninja.hideandseek.fabric.util.ParseUtil;
import me.m1dnightninja.hideandseek.api.game.AbstractClass;
import me.m1dnightninja.hideandseek.api.HideAndSeekAPI;
import me.m1dnightninja.hideandseek.api.game.SkinOption;
import me.m1dnightninja.midnightcore.api.MidnightCoreAPI;
import me.m1dnightninja.midnightcore.api.config.ConfigSection;
import me.m1dnightninja.midnightcore.api.module.skin.ISkinModule;
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

    @Override
    public void fromConfig(ConfigSection sec) {
        super.fromConfig(sec);

        effects.clear();
        equipment.clear();
        items.clear();

        if(sec.has("effects", ConfigSection.class)) {
            for(Map.Entry<String, Object> ele : sec.get("effects", ConfigSection.class).getEntries().entrySet()) {

                if(!(ele.getValue() instanceof Number)) return;

                int amplifier = ((Number) ele.getValue()).intValue();
                MobEffect eff = Registry.MOB_EFFECT.get(new ResourceLocation(ele.getKey()));

                effects.put(eff, amplifier);
            }
        }

        if(sec.has("equipment", ConfigSection.class)) {
            for(Map.Entry<String, Object> ele : sec.get("equipment", ConfigSection.class).getEntries().entrySet()) {

                EquipmentSlot slot = EquipmentSlot.byName(ele.getKey());
                if(slot == null || !(ele.getValue() instanceof ConfigSection)) continue;

                ItemStack is = ParseUtil.parseItemStack((ConfigSection) ele.getValue());
                if(is != null) equipment.put(slot, is);
            }
        }

        if(sec.has("items", List.class)) {
            for(Object o : sec.get("items", List.class)) {
                if(!(o instanceof ConfigSection)) continue;

                items.add(ParseUtil.parseItemStack((ConfigSection) o));
            }
        }

    }

    public static GameClass parse(ConfigSection conf) {

        String id = conf.get("id", String.class);

        GameClass out = new GameClass(id);
        out.fromConfig(conf);

        return out;
    }
}
