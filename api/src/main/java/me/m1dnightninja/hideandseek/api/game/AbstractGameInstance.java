package me.m1dnightninja.hideandseek.api.game;

import me.m1dnightninja.hideandseek.api.HideAndSeekAPI;
import me.m1dnightninja.hideandseek.api.core.AbstractSession;
import me.m1dnightninja.midnightcore.api.player.MPlayer;

import java.util.HashMap;
import java.util.List;

public abstract class AbstractGameInstance extends AbstractSession {

    protected HashMap<MPlayer, PositionType> positions = new HashMap<>();
    protected HashMap<MPlayer, AbstractClass> classes = new HashMap<>();
    protected final AbstractLobbySession lobby;


    public AbstractGameInstance(AbstractLobbySession lobby) {
        this.lobby = lobby;
        players.addAll(lobby.getPlayers());
    }

    public PositionType getPosition(MPlayer u) {
        return positions.get(u);
    }

    @Override
    protected final boolean shouldAddPlayer(MPlayer u) {
        return false;
    }

    @Override
    protected final void onPlayerAdded(MPlayer u) {
        removePlayer(u);
    }

    public List<MPlayer> getPlayers() {
        return lobby.getPlayers();
    }

    public int getPlayerCount() {
        return lobby.getPlayerCount();
    }

    protected void setPosition(MPlayer u, PositionType type) {

        PositionType old = positions.get(u);

        AbstractClass newClass = null;
        AbstractClass clazz = classes.get(u);
        if(clazz != null) {
            newClass = clazz.getEquivalent(type);
        }

        if(newClass == null) {
            newClass = HideAndSeekAPI.getInstance().getRegistry().chooseClass(u, getMap(), type);
        }

        if(!onPositionChanged(u, old, type)) return;

        positions.put(u, type);
        if(newClass != null) setClass(u, newClass);
    }

    protected boolean tagPlayer(MPlayer u, MPlayer tagger, PositionType type) {

        AbstractClass clazz = classes.get(u);

        if(classes.containsKey(u)) {
            if(tagger == null) {

                classes.get(u).executeCommands(AbstractClass.CommandActivationPoint.TAGGED_ENVIRONMENT, u, u);
            } else {

                classes.get(u).executeCommands(AbstractClass.CommandActivationPoint.TAGGED, u, tagger);
            }
        }
        if(clazz != null && !clazz.isTaggable()) return false;

        onTagged(u, tagger, type);

        setPosition(u, type);
        return true;
    }

    protected void setClass(MPlayer u, AbstractClass clazz) {

        AbstractClass oldClass = classes.get(u);

        if(!onClassChanged(u, oldClass, clazz)) return;

        classes.put(u, clazz);
        clazz.applyToPlayer(u);

    }

    public abstract void start();

    protected abstract void endGame(PositionType winner);
    protected abstract boolean onClassChanged(MPlayer u, AbstractClass oldClass, AbstractClass newClass);
    protected abstract boolean onPositionChanged(MPlayer u, PositionType oldRole, PositionType newRole);
    protected abstract void onTagged(MPlayer u, MPlayer tagger, PositionType newRole);

    public abstract AbstractMap getMap();

    @Override
    protected void onPlayerRemoved(MPlayer u) {

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
