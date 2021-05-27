package me.m1dnightninja.hideandseek.fabric.event;

import me.m1dnightninja.hideandseek.api.game.AbstractGameInstance;
import me.m1dnightninja.hideandseek.api.game.AbstractLobbySession;
import me.m1dnightninja.hideandseek.api.game.PositionType;
import me.m1dnightninja.midnightcore.fabric.event.Event;
import me.m1dnightninja.midnightcore.fabric.player.FabricPlayer;

public class HideAndSeekRoleUpdatedEvent extends Event {

    private final FabricPlayer player;
    private final AbstractLobbySession lobby;
    private final AbstractGameInstance gameInstance;
    private final PositionType oldPos;
    private final PositionType newPos;

    private boolean cancelled = false;

    public HideAndSeekRoleUpdatedEvent(FabricPlayer player, AbstractLobbySession lobby, AbstractGameInstance gameInstance, PositionType oldPos, PositionType newPos) {
        this.player = player;
        this.lobby = lobby;
        this.gameInstance = gameInstance;
        this.oldPos = oldPos;
        this.newPos = newPos;
    }

    public FabricPlayer getPlayer() {
        return player;
    }

    public AbstractLobbySession getLobby() {
        return lobby;
    }

    public AbstractGameInstance getGameInstance() {
        return gameInstance;
    }

    public PositionType getOldPos() {
        return oldPos;
    }

    public PositionType getNewPos() {
        return newPos;
    }

    public boolean isCancelled() {
        return cancelled;
    }

    public void setCancelled(boolean cancelled) {
        this.cancelled = cancelled;
    }
}
