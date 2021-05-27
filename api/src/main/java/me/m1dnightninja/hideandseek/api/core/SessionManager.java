package me.m1dnightninja.hideandseek.api.core;

import me.m1dnightninja.hideandseek.api.HideAndSeekAPI;
import me.m1dnightninja.hideandseek.api.game.*;
import me.m1dnightninja.midnightcore.api.player.MPlayer;

import java.util.ArrayList;
import java.util.List;

public class SessionManager {

    private final List<AbstractSession> sessions = new ArrayList<>();

    public void startSession(AbstractSession sess) {
        sessions.add(sess);
        sess.addCallback(() -> sessions.remove(sess));
    }

    public AbstractSession getSession(MPlayer u) {

        for(AbstractSession sess : sessions) {
            if(sess.getPlayers().contains(u)) return sess;
        }
        return null;
    }

    public AbstractLobbySession getActiveSession(Lobby lobby) {

        for(AbstractSession sess : sessions) {
            if(sess instanceof AbstractLobbySession && ((AbstractLobbySession) sess).getLobby() == lobby) {
                return (AbstractLobbySession) sess;
            }
        }
        return null;
    }

    public void shutdownAll() {
        int max = sessions.size();
        for(int i = 0 ; i < max ; i++) {
            sessions.get(0).shutdown();
        }
    }

    public boolean isLobbyOpen(Lobby lobby) {
        for(AbstractSession sess : sessions) {
            if(sess instanceof AbstractLobbySession && ((AbstractLobbySession) sess).getLobby() == lobby && ((AbstractLobbySession) sess).isRunning()) {
                return false;
            }
        }
        return true;
    }

    public List<Lobby> getOpenLobbies() {
        return getOpenLobbies(null);
    }

    public List<Lobby> getOpenLobbies(MPlayer player) {
        List<Lobby> out = new ArrayList<>();
        for(Lobby l : HideAndSeekAPI.getInstance().getRegistry().getLobbies(player)) {
            if(isLobbyOpen(l)) out.add(l);
        }
        return out;
    }

    public List<String> getLobbyNames(List<Lobby> lobbies) {
        List<String> out = new ArrayList<>();
        for(Lobby l : lobbies) {
            out.add(l.getId());
        }
        return out;
    }

    public List<String> getEditableMapNames(MPlayer u) {
        List<String> out = new ArrayList<>();
        for(AbstractMap map : HideAndSeekAPI.getInstance().getRegistry().getMaps()) {
            if(u == null || map.canEdit(u)) out.add(map.getId());
        }
        return out;
    }

    public boolean onDamaged(MPlayer u, MPlayer damager, DamageSource source, float amount) {

        boolean out = false;
        for(AbstractSession sess : sessions) {
            if(sess.getPlayers().contains(u)) {
                sess.onDamaged(u, damager, source, amount);
                out = true;
            }
        }

        return out;
    }

    public void tick() {

        for(AbstractSession sess : sessions) {
            sess.onTick();
        }
    }

}
