package me.m1dnightninja.hideandseek.api;

import me.m1dnightninja.midnightcore.api.Color;
import me.m1dnightninja.midnightcore.api.math.Vec3d;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public abstract class AbstractLobby {

    protected final String id;
    protected final Vec3d location;

    protected String name;
    protected List<String> description = new ArrayList<>();

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
        return new ArrayList<>(description);
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
}
