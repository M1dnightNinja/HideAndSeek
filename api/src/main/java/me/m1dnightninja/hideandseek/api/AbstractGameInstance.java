package me.m1dnightninja.hideandseek.api;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;

public abstract class AbstractGameInstance {

    protected HashMap<UUID, PositionType> positions = new HashMap<>();
    protected HashMap<UUID, AbstractClass> classes = new HashMap<>();
    protected final AbstractLobbySession lobby;

    private final List<AbstractSession.SessionListener> listeners = new ArrayList<>();

    public AbstractGameInstance(AbstractLobbySession lobby) {
        this.lobby = lobby;
    }

    public PositionType getPosition(UUID u) {
        return positions.get(u);
    }

    private boolean stopped = false;

    public final void removePlayer(UUID u) {
        positions.remove(u);
        onPlayerRemoved(u);
    }

    public final void shutdown() {

        if(stopped) return;
        stopped = true;

        for(UUID u : new ArrayList<>(positions.keySet())) {
            removePlayer(u);
        }
        onShutdown();
        for(AbstractSession.SessionListener lst : listeners) {
            lst.onShutdown();
        }
    }

    protected void setPosition(UUID u, PositionType type) {
        positions.put(u, type);
    }

    public abstract void start();

    protected abstract void onPlayerRemoved(UUID u);
    public abstract void onDamaged(UUID u, UUID damager, DamageSource source, float amount);
    protected abstract void onShutdown();

    public void addListener(AbstractSession.SessionListener listener) {
        listeners.add(listener);
    }

}
