package me.m1dnightninja.hideandseek.api;

import me.m1dnightninja.midnightcore.api.MidnightCoreAPI;
import me.m1dnightninja.midnightcore.api.module.ISavePointModule;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public abstract class AbstractSession {

    protected final List<UUID> players = new ArrayList<>();
    private final List<SessionListener> listeners = new ArrayList<>();

    private final String id;

    public AbstractSession(String id) {
        this.id = id;
    }

    public abstract void initialize();

    protected abstract boolean onJoined(UUID u);
    protected abstract void onLeft(UUID u);
    protected abstract void onShutdown();
    public abstract void onDamaged(UUID u, UUID damager, DamageSource damageSource, float amount);

    private boolean stopped = false;

    public boolean addPlayer(UUID u) {

        if(HideAndSeekAPI.getInstance().getSessionManager().getSession(u) != null) return false;

        ISavePointModule mod = MidnightCoreAPI.getInstance().getModule(ISavePointModule.class);

        mod.savePlayer(u, id);
        if(onJoined(u)) {
            players.add(u);
            return true;
        }
        mod.removeSavePoint(u, id);
        return false;
    }

    public void removePlayer(UUID u) {
        ISavePointModule mod = MidnightCoreAPI.getInstance().getModule(ISavePointModule.class);

        players.remove(u);
        onLeft(u);
        mod.loadPlayer(u, id);
        mod.removeSavePoint(u, id);
        if(players.size() == 0) {
            shutdown();
        }
    }

    public List<UUID> getPlayers() {
        return players;
    }

    public void shutdown() {
        if(stopped) return;

        stopped = true;
        int max = players.size();
        for(int i = 0 ; i < max ; i++) {
            removePlayer(players.get(0));
        }
        onShutdown();
        for(SessionListener l : listeners) {
            l.onShutdown();
        }
    }

    public abstract void broadcastMessage(String message);

    public final void addListener(SessionListener listener) {
        listeners.add(listener);
    }

    public interface SessionListener {
        void onShutdown();
    }

}
