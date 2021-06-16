package me.m1dnightninja.hideandseek.spigot.game;

import me.m1dnightninja.hideandseek.api.HideAndSeekAPI;
import me.m1dnightninja.hideandseek.api.game.AbstractClass;
import me.m1dnightninja.hideandseek.api.game.SavedSkin;
import me.m1dnightninja.midnightcore.api.MidnightCoreAPI;
import me.m1dnightninja.midnightcore.api.config.ConfigSection;
import me.m1dnightninja.midnightcore.api.config.ConfigSerializer;
import me.m1dnightninja.midnightcore.api.inventory.MItemStack;
import me.m1dnightninja.midnightcore.api.module.skin.ISkinModule;
import me.m1dnightninja.midnightcore.api.player.MPlayer;
import me.m1dnightninja.midnightcore.spigot.inventory.SpigotItem;
import me.m1dnightninja.midnightcore.spigot.player.SpigotPlayer;
import me.m1dnightninja.midnightcore.spigot.util.ReflectionUtil;
import org.apache.commons.lang.NotImplementedException;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.HashMap;
import java.util.Map;

public class GameClass extends AbstractClass {

    private final HashMap<PotionEffectType, Integer> effects = new HashMap<>();

    public GameClass(String id) {
        super(id);
    }

    @Override
    protected void executeCommand(String s) {

        Bukkit.getServer().dispatchCommand(Bukkit.getConsoleSender(), s);
    }

    @Override
    public void applyToPlayer(MPlayer uid) {

        Player player = ((SpigotPlayer) uid).getSpigotPlayer();
        if(player == null) return;

        for(PotionEffect effect : player.getActivePotionEffects()) {
            player.removePotionEffect(effect.getType());
        }

        for(Map.Entry<PotionEffectType, Integer> ent : effects.entrySet()) {
            player.addPotionEffect(new PotionEffect(ent.getKey(), Integer.MAX_VALUE, ent.getValue(), true, false, false));
        }

        for(Map.Entry<String, MItemStack> ent : equipment.entrySet()) {

            EquipmentSlot slot = EquipmentSlot.valueOf(ent.getKey());

            ItemStack stack = ((SpigotItem) ent.getValue().copy()).getBukkitStack();

            switch (slot) {
                case HEAD:     player.getInventory().setHelmet(stack);
                case CHEST:    player.getInventory().setChestplate(stack);
                case LEGS:     player.getInventory().setLeggings(stack);
                case FEET:     player.getInventory().setBoots(stack);
            }

            if(ReflectionUtil.MAJOR_VERISON <= 8) {

                if(slot == EquipmentSlot.HAND) {
                    player.getInventory().setItemInHand(stack);
                }

            } else {

                if(slot == EquipmentSlot.HAND) {
                    player.getInventory().setItemInMainHand(stack);

                }  else if(slot == EquipmentSlot.OFF_HAND) {
                    player.getInventory().setItemInOffHand(stack);
                }
            }
        }

        for(MItemStack is : items) {
            player.getInventory().addItem(((SpigotItem) is.copy()).getBukkitStack());
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
                PotionEffectType eff = PotionEffectType.getByName(ele.getKey());

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

    public static final ConfigSerializer<AbstractClass> SERIALIZER = new ConfigSerializer<AbstractClass>() {
        @Override
        public AbstractClass deserialize(ConfigSection section) {
            return parse(section);
        }

        @Override
        public ConfigSection serialize(AbstractClass object) {
            throw new NotImplementedException("Serializing classes is not yet supported!");
        }
    };

}
