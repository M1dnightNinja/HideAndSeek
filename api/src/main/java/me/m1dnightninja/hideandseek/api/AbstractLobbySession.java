package me.m1dnightninja.hideandseek.api;

import me.m1dnightninja.midnightcore.api.AbstractTimer;
import me.m1dnightninja.midnightcore.api.MidnightCoreAPI;

import java.util.UUID;

public abstract class AbstractLobbySession extends AbstractSession {

    protected final AbstractLobby lobby;
    protected AbstractGameInstance runningInstance;

    private AbstractTimer startTimer;

    public AbstractLobbySession(AbstractLobby lobby) {
        super(lobby.getId());
        this.lobby = lobby;
    }

    @Override
    protected boolean onJoined(UUID u) {

        if(players.contains(u)) return false;
        if(players.size() >= lobby.getMaxPlayers()) return false;

        if(startTimer == null) {
            if (players.size() + 1 == lobby.getMinPlayers()) {
                startTimer = MidnightCoreAPI.getInstance().createTimer("{\"text\":\"Start \",\"color\":\"" + lobby.getColor().toHex() + "\"}", 180, false, new AbstractTimer.TimerCallback() {
                    @Override
                    public void tick(int secondsLeft) { }

                    @Override
                    public void finish() {
                        if(isRunning()) return;
                        startGame(null, null);
                    }
                });
                startTimer.addPlayer(u);
                for (UUID o : players) {
                    startTimer.addPlayer(o);
                }
                startTimer.start();
            }
        } else {
            startTimer.addPlayer(u);
        }

        return true;
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
    protected void onLeft(UUID u) {
        if(players.size() < lobby.getMinPlayers() && startTimer != null) {
            startTimer.cancel();
        }
        if(isRunning()) {
            runningInstance.removePlayer(u);
        }
    }

    public boolean isRunning() {
        return runningInstance != null;
    }
}
