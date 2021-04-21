package me.m1dnightninja.hideandseek.api.game;

import me.m1dnightninja.midnightcore.api.text.MComponent;

import java.util.UUID;

public abstract class GameType {

    private final String id;
    private final MComponent name;

    public GameType(String id, MComponent name) {
        this.id = id;
        this.name = name;
    }

    public abstract AbstractGameInstance create(AbstractLobbySession lobby, UUID player, AbstractMap map);

    public String getId() {
        return id;
    }

    public MComponent getName() {
        return name;
    }
}
