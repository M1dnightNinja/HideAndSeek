package me.m1dnightninja.hideandseek.api.core;

import me.m1dnightninja.hideandseek.api.HideAndSeekAPI;
import me.m1dnightninja.hideandseek.api.game.DamageSource;
import me.m1dnightninja.midnightcore.api.MidnightCoreAPI;
import me.m1dnightninja.midnightcore.api.module.ISavePointModule;
import me.m1dnightninja.midnightcore.api.player.MPlayer;
import me.m1dnightninja.midnightcore.api.text.MComponent;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public abstract class AbstractSession {

    private final UUID sessionId = UUID.randomUUID();

    protected final List<MPlayer> players = new ArrayList<>();
    private final List<SessionCallback> callbacks = new ArrayList<>();

    private boolean shutdown = false;

    public final boolean addPlayer(MPlayer u) {

        if(players.contains(u) || !shouldAddPlayer(u)) return false;

        players.add(u);

        ISavePointModule mod = MidnightCoreAPI.getInstance().getModule(ISavePointModule.class);

        mod.savePlayer(u, sessionId.toString());
        mod.resetPlayer(u);

        onPlayerAdded(u);

        return true;
    }

    public final void removePlayer(MPlayer u) {

        if(!players.contains(u)) return;

        players.remove(u);

        try {
            onPlayerRemoved(u);
        } catch(Exception ex) {
            HideAndSeekAPI.getLogger().warn("An exception occurred while removing a player!");
            ex.printStackTrace();
        }

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

    public List<MPlayer> getPlayers() {
        return new ArrayList<>(players);
    }

    public int getPlayerCount() {
        return players.size();
    }

    public final boolean isInSession(UUID u) {
        for(MPlayer u1 : players) {
            if(u.equals(u1.getUUID())) return true;
        }
        return false;
    }

    public boolean isShutdown() {
        return shutdown;
    }

    public final void shutdown() {

        if(shutdown) return;
        shutdown = true;

        int rem = players.size();
        for(int i = 0 ; i < rem ; i++) {
            try {
                removePlayer(players.get(0));
            } catch(Exception ex) {
                ex.printStackTrace();
            }
        }

        for(SessionCallback cb : callbacks) {
            cb.onShutdown();
        }

        onShutdown();
    }

    protected abstract boolean shouldAddPlayer(MPlayer u);

    protected abstract void onPlayerAdded(MPlayer u);
    protected abstract void onPlayerRemoved(MPlayer u);

    public void broadcastMessage(MComponent comp) {

        for(MPlayer pl : players) {

            pl.sendMessage(comp);
        }
    }

    protected abstract void onShutdown();

    public abstract void onTick();
    public abstract void onDamaged(MPlayer u, MPlayer damager, DamageSource src, float amount);

    public interface SessionCallback {
        void onShutdown();
    }

}
