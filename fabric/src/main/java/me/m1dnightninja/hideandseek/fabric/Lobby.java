package me.m1dnightninja.hideandseek.fabric;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import me.m1dnightninja.hideandseek.fabric.util.ParseUtil;
import me.m1dnightninja.hideandseek.api.AbstractLobby;
import me.m1dnightninja.hideandseek.api.AbstractMap;
import me.m1dnightninja.hideandseek.api.HideAndSeekAPI;
import me.m1dnightninja.midnightcore.api.math.Vec3d;
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

import java.util.UUID;

public class Lobby extends AbstractLobby {

    private ItemStack displayStack;

    public Lobby(String id, Vec3d location) {
        super(id, location);
    }

    @Override
    public boolean canAccess(UUID u) {
        // TODO: perms
        return true;
    }

    public ItemStack getDisplayStack() {
        if(displayStack == null) {
            displayStack = createDefaultItem(this);
        }
        return displayStack;
    }

    public static Lobby parse(JsonObject obj) {

        String id = obj.get("id").getAsString();
        Vec3d location = ParseUtil.parseLocation(obj.get("location").getAsString());

        Lobby out = new Lobby(id, location);

        if(obj.has("world")) {
            out.world = obj.get("world").getAsString();
        } else {
            out.world = "minecraft:overworld";
        }

        if(obj.has("name")) {
            out.name = obj.get("name").getAsString();
        }

        if(obj.has("description") && obj.get("description").isJsonArray()) {

            for(JsonElement ele : obj.get("description").getAsJsonArray()) {
                out.description.add(ele.getAsString());
            }
        }

        if(obj.has("min_players")) {
            out.minPlayers = obj.get("min_players").getAsInt();
        }

        if(obj.has("max_players")) {
            out.maxPlayers = obj.get("max_players").getAsInt();
        }

        if(obj.has("color")) {
            out.color = ParseUtil.parseColor(obj.get("color").getAsString());
        }

        if(obj.has("game_mode")) {
            out.gameType = HideAndSeekAPI.getInstance().getRegistry().getGameType(obj.get("game_mode").getAsString());
        }

        if(obj.has("rotation")) {
            out.rotation = obj.get("rotation").getAsFloat();
        }

        if(obj.has("maps") && obj.get("maps").isJsonArray()) {
            for(JsonElement ele : obj.get("maps").getAsJsonArray()) {
                AbstractMap map = HideAndSeekAPI.getInstance().getRegistry().getMap(ele.getAsString());
                if(map != null) out.maps.add(map);
            }
        }

        if(obj.has("item")) {
            out.displayStack = ParseUtil.parseItemStack(obj.get("item").getAsJsonObject());
            if(out.displayStack != null) {
                CompoundTag tag = out.displayStack.getOrCreateTag();

                tag.put("display", createDisplayTag(out));
                out.displayStack.setTag(tag);
            }
        }

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

        MutableComponent base = new TextComponent("").setStyle(Style.EMPTY.withItalic(false));
        display.put("Name", StringTag.valueOf(Component.Serializer.toJson(base.plainCopy().append(TextUtil.parse(lobby.getName())))));

        ListTag lore = new ListTag();
        for(String s : lobby.getDescription()) {
            lore.add(StringTag.valueOf(Component.Serializer.toJson(base.plainCopy().append(TextUtil.parse(s)))));
        }

        display.put("Lore", lore);

        return display;
    }

}
