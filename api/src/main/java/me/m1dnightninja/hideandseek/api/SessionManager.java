package me.m1dnightninja.hideandseek.api;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class SessionManager {

    private final List<AbstractSession> sessions = new ArrayList<>();

    public void startSession(AbstractSession sess) {
        sessions.add(sess);
        sess.addCallback(() -> sessions.remove(sess));
    }

    public AbstractSession getSession(UUID u) {

        for(AbstractSession sess : sessions) {
            if(sess.getPlayerIds().contains(u)) return sess;
        }
        return null;
    }

    public AbstractLobbySession getActiveSession(AbstractLobby lobby) {

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

    public boolean isLobbyOpen(AbstractLobby lobby) {
        for(AbstractSession sess : sessions) {
            if(sess instanceof AbstractLobbySession && ((AbstractLobbySession) sess).getLobby() == lobby && ((AbstractLobbySession) sess).isRunning()) {
                return false;
            }
        }
        return true;
    }

    public List<AbstractLobby> getOpenLobbies() {
        return getOpenLobbies(null);
    }

    public List<AbstractLobby> getOpenLobbies(UUID player) {
        List<AbstractLobby> out = new ArrayList<>();
        for(AbstractLobby l : HideAndSeekAPI.getInstance().getRegistry().getLobbies(player)) {
            if(isLobbyOpen(l)) out.add(l);
        }
        return out;
    }

    public List<String> getLobbyNames(List<AbstractLobby> lobbies) {
        List<String> out = new ArrayList<>();
        for(AbstractLobby l : lobbies) {
            out.add(l.getId());
        }
        return out;
    }

    public List<String> getEditableMapNames(UUID u) {
        List<String> out = new ArrayList<>();
        for(AbstractMap map : HideAndSeekAPI.getInstance().getRegistry().getMaps()) {
            if(u == null || map.canEdit(u)) out.add(map.getId());
        }
        return out;
    }

    public boolean onDamaged(UUID u, UUID damager, DamageSource source, float amount) {

        boolean out = false;
        for(AbstractSession sess : sessions) {
            if(sess.getPlayerIds().contains(u)) {
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
