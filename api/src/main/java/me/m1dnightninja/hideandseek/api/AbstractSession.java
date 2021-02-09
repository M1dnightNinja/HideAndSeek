package me.m1dnightninja.hideandseek.api;

import me.m1dnightninja.midnightcore.api.MidnightCoreAPI;
import me.m1dnightninja.midnightcore.api.module.ISavePointModule;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

public abstract class AbstractSession {

    private final UUID sessionId = UUID.randomUUID();

    private final List<UUID> players = new ArrayList<>();
    private final List<SessionCallback> callbacks = new ArrayList<>();

    private boolean shutdown = false;

    public final boolean addPlayer(UUID u) {

        if(players.contains(u) || !shouldAddPlayer(u)) return false;

        players.add(u);

        ISavePointModule mod = MidnightCoreAPI.getInstance().getModule(ISavePointModule.class);

        mod.savePlayer(u, sessionId.toString());
        mod.resetPlayer(u);

        onPlayerAdded(u);

        return true;
    }

    public final void removePlayer(UUID u) {

        players.remove(u);

        onPlayerRemoved(u);

        ISavePointModule mod = MidnightCoreAPI.getInstance().getModule(ISavePointModule.class);
        mod.loadPlayer(u, sessionId.toString());
        mod.removeSavePoint(u, sessionId.toString());

        if(players.size() == 0) {
            shutdown();
        }
    }

    public final void addCallback(SessionCallback cb) {
        callbacks.add(cb);
    }

    public List<UUID> getPlayerIds() {
        return new ArrayList<>(players);
    }

    public int getPlayerCount() {
        return players.size();
    }

    public final boolean isInSession(UUID u) {
        for(UUID u1 : players) {
            if(u.equals(u1)) return true;
        }
        return false;
    }

    public boolean isShutdown() {
        return shutdown;
    }

    public final void shutdown() {

        shutdown = true;

        for(int i = 0 ; i < players.size() ; i++) {
            removePlayer(players.get(0));
        }

        for(SessionCallback cb : callbacks) {
            cb.onShutdown();
        }

        onShutdown();
    }

    protected abstract boolean shouldAddPlayer(UUID u);

    protected abstract void onPlayerAdded(UUID u);
    protected abstract void onPlayerRemoved(UUID u);

    protected abstract void onShutdown();

    protected abstract void onTick();
    public abstract void onDamaged(UUID u, UUID damager, DamageSource src, float amount);

    public interface SessionCallback {
        void onShutdown();
    }

}
