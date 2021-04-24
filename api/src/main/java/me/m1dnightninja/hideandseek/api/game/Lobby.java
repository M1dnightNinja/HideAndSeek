package me.m1dnightninja.hideandseek.api.game;

import me.m1dnightninja.hideandseek.api.HideAndSeekAPI;
import me.m1dnightninja.midnightcore.api.MidnightCoreAPI;
import me.m1dnightninja.midnightcore.api.inventory.MItemStack;
import me.m1dnightninja.midnightcore.api.math.Color;
import me.m1dnightninja.midnightcore.api.config.ConfigSection;
import me.m1dnightninja.midnightcore.api.math.Vec3d;
import me.m1dnightninja.midnightcore.api.registry.MIdentifier;
import me.m1dnightninja.midnightcore.api.text.MComponent;
import me.m1dnightninja.midnightcore.api.text.MStyle;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class Lobby {

    protected final String id;
    protected final Vec3d location;

    protected MItemStack displayStack;

    protected MComponent name;
    protected List<MComponent> description = new ArrayList<>();

    protected String permission;

    protected int minPlayers = 2;
    protected int maxPlayers = 16;

    protected Color color = new Color(255,255,255);
    protected GameType gameType;

    protected float rotation;
    protected String world;

    protected final List<AbstractMap> maps = new ArrayList<>();

    public Lobby(String id, Vec3d location) {
        this.id = id;
        this.name = MComponent.createTextComponent(id);
        this.location = location;
    }

    public String getId() {
        return id;
    }

    public int getMinPlayers() {
        return minPlayers;
    }

    public int getMaxPlayers() {
        return maxPlayers;
    }

    public MComponent getName() {
        return name;
    }

    public List<MComponent> getDescription() {
        return description;
    }

    public Color getColor() {
        return color;
    }

    public GameType getGameType() {
        return gameType;
    }

    public Vec3d getLocation() {
        return location;
    }

    public float getRotation() {
        return rotation;
    }

    public String getWorld() {
        return world;
    }

    public List<AbstractMap> getMaps() {
        return maps;
    }

    public MItemStack getDisplayStack() {
        return displayStack;
    }

    public String getPermission() {
        return permission;
    }

    public boolean canAccess(UUID u) {

        return permission == null || MidnightCoreAPI.getInstance().hasPermission(u, permission);
    }

    public void fromConfig(ConfigSection sec) {

        if(sec.has("name", String.class)) {
            name = MComponent.Serializer.parse(sec.getString("name"));
        }

        if(sec.has("description", List.class)) {
            for(String s : sec.getStringList("description")) {

                description.add(MComponent.Serializer.parse(s));
            }
        }

        if(sec.has("permission", String.class)) {
            permission = sec.getString("permission");
        }

        if(sec.has("min_players", Number.class)) {
            minPlayers = sec.getInt("min_players");
        }

        if(sec.has("max_players", Number.class)) {
            maxPlayers = sec.getInt("max_players");
        }

        if(sec.has("color", String.class)) {
            color = Color.parse(sec.getString("color"));
        } else if(sec.has("color", Number.class)) {
            color = new Color(sec.getInt("color"));
        }

        if(sec.has("game_mode", String.class)) {
            gameType = HideAndSeekAPI.getInstance().getRegistry().getGameType(sec.getString("game_mode"));
        }

        if(sec.has("rotation", Number.class)) {
            rotation = sec.getFloat("rotation");
        }

        if(sec.has("world", String.class)) {
            world = sec.getString("world");
        }

        if(sec.has("maps", List.class)) {
            for(Object o : sec.get("maps", List.class)) {
                if(!(o instanceof String)) continue;
                maps.add(HideAndSeekAPI.getInstance().getRegistry().getMap((String) o));
            }
        }

        if(sec.has("item", MItemStack.class)) {
            displayStack = sec.get("item", MItemStack.class);

            ConfigSection tag = displayStack.getTag();

            if(!tag.has("display")) {

                ConfigSection display = new ConfigSection();
                display.set("Name", MComponent.Serializer.toJsonString(MComponent.createTextComponent("").withStyle(MStyle.ITEM_BASE).addChild(name)));

                List<String> strs = new ArrayList<>();
                for(MComponent cmp : description) {
                    strs.add(MComponent.Serializer.toJsonString(MComponent.createTextComponent("").withStyle(MStyle.ITEM_BASE).addChild(cmp)));
                }

                display.set("Lore", strs);
                tag.set("display", display);
            }
        } else {

            List<MComponent> lore = new ArrayList<>();
            for(MComponent comp : description) {
                lore.add(comp.copy().withStyle(comp.getStyle().fill(MStyle.ITEM_BASE)));
            }

            displayStack = MItemStack.Builder.of(MIdentifier.create("minecraft", "white_wool")).withName(name.copy().withStyle(name.getStyle().fill(MStyle.ITEM_BASE))).withLore(lore).build();
        }
    }

    public static Lobby parse(ConfigSection sec) {

        String id = sec.getString("id");
        Vec3d location = Vec3d.parse(sec.getString("location"));

        Lobby out = new Lobby(id, location);
        out.fromConfig(sec);

        return out;
    }

}
