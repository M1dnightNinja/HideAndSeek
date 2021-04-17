package me.m1dnightninja.hideandseek.fabric.game;

import me.m1dnightninja.hideandseek.fabric.util.ParseUtil;
import me.m1dnightninja.hideandseek.api.game.AbstractLobby;
import me.m1dnightninja.midnightcore.api.MidnightCoreAPI;
import me.m1dnightninja.midnightcore.api.config.ConfigSection;
import me.m1dnightninja.midnightcore.api.math.Vec3d;
import me.m1dnightninja.midnightcore.api.module.ILangModule;
import me.m1dnightninja.midnightcore.fabric.util.TextUtil;
import net.minecraft.core.Registry;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

import java.util.List;
import java.util.UUID;

public class Lobby extends AbstractLobby {

    public static void registerPlaceholders(ILangModule<Component> mod) {

        mod.registerStringPlaceholder("hideandseek_lobby_id", mod.createSupplier(AbstractLobby.class, AbstractLobby::getId));
        mod.registerStringPlaceholder("hideandseek_lobby_color", mod.createSupplier(AbstractLobby.class, lby -> lby.getColor().toHex()));
        mod.registerStringPlaceholder("hideandseek_lobby_color_legacy", mod.createSupplier(AbstractLobby.class, lby -> "§" + Integer.toHexString(lby.getColor().toRGBI())));
        mod.registerStringPlaceholder("hideandseek_lobby_min_players", mod.createSupplier(AbstractLobby.class, lby -> lby.getMinPlayers() + ""));
        mod.registerStringPlaceholder("hideandseek_lobby_max_players", mod.createSupplier(AbstractLobby.class, lby -> lby.getMaxPlayers() + ""));
        mod.registerStringPlaceholder("hideandseek_lobby_game_mode", mod.createSupplier(AbstractLobby.class, lby -> lby.getGameType().getId()));

        mod.registerRawPlaceholder("hideandseek_lobby_name", mod.createSupplier(AbstractLobby.class, lby -> TextUtil.parse(lby.getName())));
        mod.registerRawPlaceholder("hideandseek_lobby_game_mode_name", mod.createSupplier(AbstractLobby.class, lby -> TextUtil.parse(lby.getGameType().getName())));

    }

    private ItemStack displayStack;

    public Lobby(String id, Vec3d location) {
        super(id, location);
    }

    @Override
    public boolean canAccess(UUID u) {

        return permission == null || MidnightCoreAPI.getInstance().hasPermission(u, permission);
    }

    public ItemStack getDisplayStack() {
        if(displayStack == null) {
            displayStack = createDefaultItem(this);
        }
        return displayStack;
    }

    @Override
    public void fromConfig(ConfigSection sec) {
        super.fromConfig(sec);

        if(sec.has("item", ConfigSection.class)) {
            displayStack = ParseUtil.parseItemStack(sec.get("item", ConfigSection.class));
            if(displayStack != null) {
                CompoundTag tag = displayStack.getOrCreateTag();

                tag.put("display", createDisplayTag(this));
                displayStack.setTag(tag);
            }
        }
    }

    public static Lobby parse(ConfigSection sec) {

        String id = sec.getString("id");
        Vec3d location = Vec3d.parse(sec.getString("location"));

        Lobby out = new Lobby(id, location);
        out.fromConfig(sec);

        return out;
    }

    public static ItemStack createDefaultItem(AbstractLobby lobby) {

        ItemStack out = new ItemStack(Registry.ITEM.get(new ResourceLocation(lobby.getColor().toDyeColor() + "_wool")));

        CompoundTag tag = new CompoundTag();

        tag.put("display", createDisplayTag(lobby));
        out.setTag(tag);

        return out;
    }

    private static CompoundTag createDisplayTag(AbstractLobby lobby) {

        CompoundTag display = new CompoundTag();

        MutableComponent base = new TextComponent("").setStyle(Style.EMPTY.withItalic(Boolean.FALSE));

        display.put("Name", StringTag.valueOf(Component.Serializer.toJson(base.copy().append(TextUtil.parse(lobby.getName())))));

        ListTag lore = new ListTag();
        List<String> desc = lobby.getDescription();

        if(desc != null) {
            for (String s : desc){
                lore.add(StringTag.valueOf(Component.Serializer.toJson(base.copy().append(TextUtil.parse(s)))));
            }
        }

        display.put("Lore", lore);

        return display;
    }

}
