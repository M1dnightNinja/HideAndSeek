package me.m1dnightninja.hideandseek.api.game;

import me.m1dnightninja.hideandseek.api.HideAndSeekAPI;
import me.m1dnightninja.midnightcore.api.Color;
import me.m1dnightninja.midnightcore.api.config.ConfigSection;
import me.m1dnightninja.midnightcore.api.math.Vec3d;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public abstract class AbstractLobby {

    protected final String id;
    protected final Vec3d location;

    protected String name;
    protected List<String> description;

    protected String permission;

    protected int minPlayers = 2;
    protected int maxPlayers = 16;

    protected Color color = new Color(255,255,255);
    protected GameType gameType;

    protected float rotation;
    protected String world;

    protected final List<AbstractMap> maps = new ArrayList<>();

    public AbstractLobby(String id, Vec3d location) {
        this.id = id;
        this.name = id;
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

    public String getName() {
        return name;
    }

    public List<String> getDescription() {
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

    public abstract boolean canAccess(UUID u);

    public void fromConfig(ConfigSection sec) {

        if(sec.has("name", String.class)) {
            name = sec.getString("name");
        }

        if(sec.has("description", List.class)) {
            description = sec.getStringList("description");
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



    }

}
