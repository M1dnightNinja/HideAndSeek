package me.m1dnightninja.hideandseek.fabric.game;

import me.m1dnightninja.hideandseek.api.game.AbstractMap;
import me.m1dnightninja.hideandseek.api.game.PositionType;
import me.m1dnightninja.hideandseek.fabric.util.ParseUtil;
import me.m1dnightninja.midnightcore.api.config.ConfigSection;
import me.m1dnightninja.midnightcore.api.math.Vec3d;
import me.m1dnightninja.midnightcore.api.module.ILangModule;
import me.m1dnightninja.midnightcore.fabric.MidnightCore;
import me.m1dnightninja.midnightcore.fabric.api.PermissionHelper;
import me.m1dnightninja.midnightcore.fabric.util.ItemBuilder;
import me.m1dnightninja.midnightcore.fabric.util.TextUtil;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class Map extends AbstractMap {

    private ItemStack item;

    public static void registerPlaceholders(ILangModule<Component> mod) {

        mod.registerStringPlaceholder("hideandseek_map_id", mod.createSupplier(AbstractMap.class, AbstractMap::getId));
        mod.registerStringPlaceholder("hideandseek_map_hide_time", mod.createSupplier(AbstractMap.class, obj -> obj.getHideTime() + ""));
        mod.registerStringPlaceholder("hideandseek_map_seek_time", mod.createSupplier(AbstractMap.class, obj -> obj.getHideTime() + ""));

        mod.registerRawPlaceholder("hideandseek_map_name", mod.createSupplier(AbstractMap.class, obj -> TextUtil.parse(obj.getName())));

    }

    public Map(String id, File worldFolder, Vec3d hiderSpawn, Vec3d seekerSpawn) {
        super(id, worldFolder, hiderSpawn, seekerSpawn);
    }

    @Override
    public boolean canEdit(UUID u) {
        ServerPlayer player = MidnightCore.getServer().getPlayerList().getPlayer(u);
        if(player == null) return false;

        return player.getUUID().equals(author) || editors.contains(player.getUUID()) || PermissionHelper.check(u, "hideandseek.edit." + getId());
    }

    @Override
    public void fromConfig(ConfigSection sec) {
        super.fromConfig(sec);

        if(sec.has("classes", List.class)) {
            for(Object o : sec.get("classes", List.class)) {
                if(!(o instanceof ConfigSection)) continue;

                GameClass clazz = GameClass.parse((ConfigSection) o);
                mapClasses.put(clazz.getId(), clazz);
            }
        }

        if(sec.has("item", ConfigSection.class)) {
            ConfigSection isec = sec.getSection("item");

            ListTag cmp = new ListTag();
            for(String s : description) {
                cmp.add(StringTag.valueOf(Component.Serializer.toJson(TextUtil.parse(s))));
            }

            item = ParseUtil.parseItemStack(isec);

            if(item == null) {
                item = createDefaultItem(this);
            } else {

                CompoundTag display = new CompoundTag();
                display.putString("Name", Component.Serializer.toJson(new TextComponent("").setStyle(ItemBuilder.BASE_STYLE).append(TextUtil.parse(name))));
                display.put("List", cmp);

                item.getOrCreateTag().put("display", display);
            }
        } else {
            item = createDefaultItem(this);
        }

        for(PositionType type : PositionType.values()) {
            if(sec.has(type.getId(), ConfigSection.class)) {
                positionData.put(type, PositionData.parse(sec.getSection(type.getId()), type, this));
            }
        }
    }

    public static Map parse(ConfigSection sec, String id, File f) {

        Vec3d hiderSpawn = Vec3d.parse(sec.getString("hider_spawn"));
        Vec3d seekerSpawn = Vec3d.parse(sec.getString("seeker_spawn"));

        Map out = new Map(id, f, hiderSpawn, seekerSpawn);
        out.fromConfig(sec);

        return out;
    }

    private static ItemStack createDefaultItem(AbstractMap map) {

        List<Component> cmp = new ArrayList<>();
        for(String s : map.getDescription()) {
            cmp.add(TextUtil.parse(s));
        }

        return ItemBuilder.of(Items.WHITE_WOOL).withName(new TextComponent("").setStyle(ItemBuilder.BASE_STYLE).append(TextUtil.parse(map.getName()))).withLore(cmp).build();
    }

    public static ItemStack getDisplayStack(AbstractMap map) {

        if(!(map instanceof Map)) {
            return createDefaultItem(map);
        }

        Map m = (Map) map;
        return m.item.copy();
    }

}
