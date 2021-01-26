package me.m1dnightninja.hideandseek.api;

import java.util.UUID;

public interface GameType {

    AbstractGameInstance create(AbstractLobbySession lobby, UUID player, AbstractMap map);

}
