package me.m1dnightninja.hideandseek.api.game;

import me.m1dnightninja.hideandseek.api.HideAndSeekAPI;
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

            if (getPlayerCount() == lobby.getMinPlayers()) {

                startTimer = MidnightCoreAPI.getInstance().createTimer(HideAndSeekAPI.getInstance().getLangProvider().getMessage("lobby.start_timer", this, lobby), 180, false, secondsLeft -> {
                    if(secondsLeft == 0) {
                        if(isRunning()) return;
                        startGame(null, null);
                    }
                });
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

        GameType type = lobby.getGameType();

        if(type == null) {
            shutdown();
            return;
        }

        runningInstance = type.create(this, seeker, map);
        runningInstance.addCallback(this::shutdown);
        runningInstance.start();
    }

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

        if(runningInstance != null) {

            HideAndSeekAPI.getLogger().info("removing from game " + runningInstance.getClass().getName());
            runningInstance.removePlayer(u);

        } else if(getPlayerCount() < lobby.getMinPlayers() && startTimer != null) {
            startTimer.cancel();
            startTimer = null;
        }
    }

    @Override
    public void onTick() {
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
