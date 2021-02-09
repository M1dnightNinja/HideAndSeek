package me.m1dnightninja.hideandseek.api;

import java.util.UUID;

public abstract class GameType {

    private final String id;
    private final String name;

    public GameType(String id, String name) {
        this.id = id;
        this.name = name;
    }

    public abstract AbstractGameInstance create(AbstractLobbySession lobby, UUID player, AbstractMap map);

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }
}
