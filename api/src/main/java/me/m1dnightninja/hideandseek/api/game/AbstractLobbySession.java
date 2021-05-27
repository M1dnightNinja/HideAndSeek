package me.m1dnightninja.hideandseek.api.game;

import me.m1dnightninja.hideandseek.api.HideAndSeekAPI;
import me.m1dnightninja.hideandseek.api.core.AbstractSession;
import me.m1dnightninja.midnightcore.api.MidnightCoreAPI;
import me.m1dnightninja.midnightcore.api.math.Color;
import me.m1dnightninja.midnightcore.api.player.MPlayer;
import me.m1dnightninja.midnightcore.api.text.AbstractCustomScoreboard;
import me.m1dnightninja.midnightcore.api.text.MComponent;
import me.m1dnightninja.midnightcore.api.text.MStyle;
import me.m1dnightninja.midnightcore.api.text.AbstractTimer;

public abstract class AbstractLobbySession extends AbstractSession {

    protected final Lobby lobby;
    protected AbstractGameInstance runningInstance;

    private AbstractTimer startTimer;
    private final AbstractCustomScoreboard scoreboard;

    public AbstractLobbySession(Lobby lobby) {
        this.lobby = lobby;

        scoreboard = MidnightCoreAPI.getInstance().createScoreboard(AbstractCustomScoreboard.generateRandomId(), MComponent.createTextComponent("HideAndSeek").withStyle(new MStyle().withColor(Color.fromRGBI(14)).withBold(true)));

        scoreboard.setLine(5, MComponent.createTextComponent("                         "));
        scoreboard.setLine(4, MComponent.createTextComponent("Lobby: ").addChild(lobby.getName()));
        scoreboard.setLine(3, MComponent.createTextComponent("Game Mode: ").addChild(lobby.getGameType().getName()));
        scoreboard.setLine(2, MComponent.createTextComponent("                         "));
        scoreboard.setLine(1, MComponent.createTextComponent("Players: ").addChild(MComponent.createTextComponent(getPlayerCount() + " / " + lobby.getMaxPlayers()).withStyle(new MStyle().withColor(Color.fromRGBI(10)))));

    }

    @Override
    protected boolean shouldAddPlayer(MPlayer u) {
        return getPlayerCount() < lobby.getMaxPlayers() && !getPlayers().contains(u);
    }

    @Override
    protected void onPlayerAdded(MPlayer u) {

        if(startTimer == null) {

            if (getPlayerCount() == lobby.getMinPlayers()) {

                startTimer = MidnightCoreAPI.getInstance().createTimer(HideAndSeekAPI.getInstance().getLangProvider().getMessage("lobby.start_timer", (MPlayer) null, this, lobby), 180, false, secondsLeft -> {
                    if(secondsLeft == 0) {
                        if(isRunning()) return;
                        startGame(null, null);
                    }
                });
                for (MPlayer o : getPlayers()) {
                    startTimer.addPlayer(o);
                }
                startTimer.start();
            }
        } else {
            startTimer.addPlayer(u);
        }

        broadcastMessage(HideAndSeekAPI.getInstance().getLangProvider().getMessage("lobby.join", (MPlayer) null, this, u));

        scoreboard.setLine(1, MComponent.createTextComponent("Players: ").addChild(MComponent.createTextComponent(getPlayerCount() + " / " + lobby.getMaxPlayers()).withStyle(new MStyle().withColor(Color.fromRGBI(10)))));
        scoreboard.update();

        scoreboard.addPlayer(u);

    }

    public void startGame(MPlayer seeker, AbstractMap map) {
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

    public Lobby getLobby() {
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
    protected void onPlayerRemoved(MPlayer u) {

        if(runningInstance != null) {

            HideAndSeekAPI.getLogger().info("removing from game " + runningInstance.getClass().getName());
            runningInstance.removePlayer(u);

        } else if(getPlayerCount() < lobby.getMinPlayers() && startTimer != null) {
            startTimer.cancel();
            startTimer = null;
        }

        scoreboard.setLine(1, MComponent.createTextComponent("Players: ").addChild(MComponent.createTextComponent(getPlayerCount() + " / " + lobby.getMaxPlayers()).withStyle(new MStyle().withColor(Color.fromRGBI(10)))));
        scoreboard.update();
        scoreboard.removePlayer(u);
        if(!isShutdown()) broadcastMessage(HideAndSeekAPI.getInstance().getLangProvider().getMessage("lobby.leave", (MPlayer) null, this, u));

    }

    @Override
    public void onTick() {
        if(isRunning()) runningInstance.onTick();
    }

    public AbstractGameInstance getRunningGame() {
        return runningInstance;
    }

    public boolean isRunning() {
        return runningInstance != null && !runningInstance.isShutdown();
    }
}
