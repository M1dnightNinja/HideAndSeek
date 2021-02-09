package me.m1dnightninja.hideandseek.api;

import me.m1dnightninja.midnightcore.api.AbstractTimer;
import me.m1dnightninja.midnightcore.api.MidnightCoreAPI;

import java.util.UUID;

public abstract class AbstractLobbySession extends AbstractSession {

    protected final AbstractLobby lobby;
    protected AbstractGameInstance runningInstance;

    private AbstractTimer startTimer;

    public AbstractLobbySession(AbstractLobby lobby) {
        this.lobby = lobby;
    }

    @Override
    protected boolean shouldAddPlayer(UUID u) {
        return getPlayerCount() < lobby.getMaxPlayers() && !getPlayerIds().contains(u);
    }

    @Override
    protected void onPlayerAdded(UUID u) {

        if(startTimer == null) {
            if (getPlayerCount() + 1 == lobby.getMinPlayers()) {
                startTimer = MidnightCoreAPI.getInstance().createTimer(HideAndSeekAPI.getInstance().getLangProvider().getMessage("lobby.start_timer", this, lobby), 180, false, new AbstractTimer.TimerCallback() {
                    @Override
                    public void tick(int secondsLeft) { }

                    @Override
                    public void finish() {
                        if(isRunning()) return;
                        startGame(null, null);
                    }
                });
                startTimer.addPlayer(u);
                for (UUID o : getPlayerIds()) {
                    startTimer.addPlayer(o);
                }
                startTimer.start();
            }
        } else {
            startTimer.addPlayer(u);
        }
    }

    public void startGame(UUID seeker, AbstractMap map) {
        if(startTimer != null) {
            startTimer.cancel();
        }
        doGameStart(seeker, map);
    }

    protected abstract void doGameStart(UUID seeker, AbstractMap map);

    public AbstractLobby getLobby() {
        return lobby;
    }

    @Override
    protected void onShutdown() {
        if(isRunning()) {
            runningInstance.shutdown();
            runningInstance = null;
        }
    }

    @Override
    protected void onPlayerRemoved(UUID u) {
        if(getPlayerCount() < lobby.getMinPlayers() && startTimer != null) {
            startTimer.cancel();
        }
        if(isRunning()) {
            runningInstance.removePlayer(u);
        }
    }

    @Override
    protected void onTick() {
        if(isRunning()) runningInstance.onTick();
    }

    public AbstractGameInstance getRunningGame() {
        return runningInstance;
    }

    public abstract void broadcastMessage(String msg);

    public boolean isRunning() {
        return runningInstance != null && !runningInstance.isShutdown();
    }
}
