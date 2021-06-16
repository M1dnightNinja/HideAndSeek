package me.m1dnightninja.hideandseek.spigot.event;

import me.m1dnightninja.hideandseek.api.game.AbstractGameInstance;
import me.m1dnightninja.hideandseek.api.game.AbstractLobbySession;
import me.m1dnightninja.hideandseek.api.game.PositionType;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.HandlerList;
import org.bukkit.event.player.PlayerEvent;

import javax.annotation.Nonnull;

public class HideAndSeekRoleUpdatedEvent extends PlayerEvent implements Cancellable {

    private static final HandlerList handlers = new HandlerList();

    public static HandlerList getHandlerList() {
        return handlers;
    }

    @Override
    @Nonnull
    public HandlerList getHandlers() {
        return handlers;
    }

    private final AbstractLobbySession lobby;
    private final AbstractGameInstance gameInstance;
    private final PositionType oldPos;
    private final PositionType newPos;

    private boolean cancelled = false;

    public HideAndSeekRoleUpdatedEvent(Player player, AbstractLobbySession lobby, AbstractGameInstance gameInstance, PositionType oldPos, PositionType newPos) {
        super(player);
        this.player = player;
        this.lobby = lobby;
        this.gameInstance = gameInstance;
        this.oldPos = oldPos;
        this.newPos = newPos;
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

    @Override
    public boolean isCancelled() {
        return cancelled;
    }

    @Override
    public void setCancelled(boolean b) {
        cancelled = b;
    }
}
