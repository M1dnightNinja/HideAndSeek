package me.m1dnightninja.hideandseek.api;

import java.util.HashMap;
import java.util.List;
import java.util.UUID;

public abstract class AbstractGameInstance extends AbstractSession {

    protected HashMap<UUID, PositionType> positions = new HashMap<>();
    protected HashMap<UUID, AbstractClass> classes = new HashMap<>();
    protected final AbstractLobbySession lobby;


    public AbstractGameInstance(AbstractLobbySession lobby) {
        this.lobby = lobby;
    }

    public PositionType getPosition(UUID u) {
        return positions.get(u);
    }

    @Override
    protected final boolean shouldAddPlayer(UUID u) {
        return false;
    }

    @Override
    protected final void onPlayerAdded(UUID u) {
        removePlayer(u);
    }

    public List<UUID> getPlayerIds() {
        return lobby.getPlayerIds();
    }

    public int getPlayerCount() {
        return lobby.getPlayerCount();
    }

    @Override
    protected void onShutdown() {
        lobby.shutdown();
    }

    protected void setPosition(UUID u, PositionType type) {
        positions.put(u, type);
    }

    public abstract void start();

    protected abstract void endGame(PositionType winner);

    public abstract AbstractMap getMap();

    @Override
    protected void onPlayerRemoved(UUID u) {

        positions.remove(u);
        checkVictory();

        lobby.removePlayer(u);
    }

    public int countPosition(PositionType t) {
        int out = 0;
        for(PositionType t1 : positions.values()) {
            if(t == t1) out++;
        }
        return out;
    }

    protected abstract void checkVictory();

}
