package me.m1dnightninja.hideandseek.fabric.game;

import me.m1dnightninja.hideandseek.api.game.AbstractClass;
import me.m1dnightninja.hideandseek.api.HideAndSeekAPI;
import me.m1dnightninja.hideandseek.api.game.SavedSkin;
import me.m1dnightninja.midnightcore.api.MidnightCoreAPI;
import me.m1dnightninja.midnightcore.api.config.ConfigSection;
import me.m1dnightninja.midnightcore.api.inventory.MItemStack;
import me.m1dnightninja.midnightcore.api.module.skin.ISkinModule;
import me.m1dnightninja.midnightcore.api.player.MPlayer;
import me.m1dnightninja.midnightcore.fabric.MidnightCore;
import me.m1dnightninja.midnightcore.fabric.inventory.FabricItem;
import me.m1dnightninja.midnightcore.fabric.player.FabricPlayer;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;

import java.util.HashMap;
import java.util.Map;

public class GameClass extends AbstractClass {

    private final HashMap<MobEffect, Integer> effects = new HashMap<>();

    public GameClass(String id) {
        super(id);
    }

    @Override
    protected void executeCommand(String s) {

        MidnightCore.getServer().getCommands().performCommand(MidnightCore.getServer().createCommandSourceStack(), s);
    }

    @Override
    public void applyToPlayer(MPlayer uid) {

        ServerPlayer player = ((FabricPlayer) uid).getMinecraftPlayer();
        if(player == null) return;

        player.removeAllEffects();
        for(Map.Entry<MobEffect, Integer> ent : effects.entrySet()) {
            player.addEffect(new MobEffectInstance(ent.getKey(), Integer.MAX_VALUE, ent.getValue(), true, false, false));
        }

        for(Map.Entry<String, MItemStack> ent : equipment.entrySet()) {

            EquipmentSlot slot = EquipmentSlot.byName(ent.getKey());
            if(slot == null) continue;

            ItemStack stack = ((FabricItem) ent.getValue().copy()).getMinecraftItem();

            if(slot.getType() == EquipmentSlot.Type.ARMOR) {
                player.inventory.armor.set(slot.getIndex(), stack);
            } else if(slot == EquipmentSlot.OFFHAND) {
                player.setItemInHand(InteractionHand.OFF_HAND, stack);
            } else {
                player.setItemInHand(InteractionHand.MAIN_HAND, stack);
            }
        }

        for(MItemStack is : items) {
            player.inventory.add(((FabricItem) is.copy()).getMinecraftItem());
        }

        SavedSkin skin = null;
        if(skins.size() > 0) skin = HideAndSeekAPI.getInstance().getRegistry().getSkin(skins.get(HideAndSeekAPI.getInstance().getRandom().nextInt(skins.size())));

        if(skin != null) {
            ISkinModule mod = MidnightCoreAPI.getInstance().getModule(ISkinModule.class);
            mod.setSkin(uid, skin.getSkin());
            mod.updateSkin(uid);
        }

        executeCommands(CommandActivationPoint.SETUP, uid, null);

    }

    @Override
    public void fromConfig(ConfigSection sec) {
        super.fromConfig(sec);

        effects.clear();

        if(sec.has("effects", ConfigSection.class)) {
            for(Map.Entry<String, Object> ele : sec.get("effects", ConfigSection.class).getEntries().entrySet()) {

                if(!(ele.getValue() instanceof Number)) return;

                int amplifier = ((Number) ele.getValue()).intValue();
                MobEffect eff = Registry.MOB_EFFECT.get(new ResourceLocation(ele.getKey()));

                effects.put(eff, amplifier);
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
