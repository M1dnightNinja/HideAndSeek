package me.m1dnightninja.hideandseek.common;

import me.m1dnightninja.hideandseek.api.*;
import me.m1dnightninja.midnightcore.api.AbstractTimer;
import me.m1dnightninja.midnightcore.api.MidnightCoreAPI;
import me.m1dnightninja.midnightcore.api.lang.AbstractLangProvider;
import me.m1dnightninja.midnightcore.api.math.Vec3d;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;

public abstract class AbstractClassicGameMode extends AbstractGameInstance {

    protected ClassicGameState state = ClassicGameState.UNINITIALIZED;

    protected final UUID seeker;
    protected final AbstractMap map;

    protected final List<UUID> toTeleport = new ArrayList<>();
    protected final HashMap<UUID, Vec3d> locations = new HashMap<>();

    private final List<AbstractTimer> timers = new ArrayList<>();

    private AbstractTimer seekerTimer;
    private AbstractTimer hiderTimer;

    public AbstractClassicGameMode(AbstractLobbySession lobby, UUID seeker, AbstractMap map) {
        super(lobby);

        this.seeker = seeker == null ? getPlayerIds().get(HideAndSeekAPI.getInstance().getRandom().nextInt(getPlayerCount())) : seeker;
        this.map = map == null ? lobby.getLobby().getMaps().get(HideAndSeekAPI.getInstance().getRandom().nextInt(lobby.getLobby().getMaps().size())) : map;

    }

    @Override
    public final void start() {

        loadWorld(map, this::startHiding);

    }

    @Override
    public AbstractMap getMap() {
        return map;
    }

    protected void startHiding() {

        state = ClassicGameState.HIDING;

        for(UUID u : getPlayerIds()) {
            try {
                setupPlayer(u);
            } catch (Throwable th) {
                th.printStackTrace();
                shutdown();
                return;
            }
        }

        AbstractTimer startTimer = MidnightCoreAPI.getInstance().createTimer(HideAndSeekAPI.getInstance().getLangProvider().getMessage(getKey("hide_timer", null), this, lobby, map.getData(PositionType.MAIN_SEEKER)), map.getHideTime(), false, new AbstractTimer.TimerCallback() {
            @Override
            public void tick(int timeLeft) {
                if(timeLeft > 0 && timeLeft < 6) {
                    playTickSound();
                }
            }

            @Override
            public void finish() {
                playReleaseSound();
                startSeeking();
            }
        });

        for(UUID u : getPlayerIds()) {
            startTimer.addPlayer(u);
        }

        timers.add(startTimer);
        startTimer.start();

    }

    protected void startSeeking() {

        state = ClassicGameState.SEEKING;
        cancelTimers();

        hiderTimer = MidnightCoreAPI.getInstance().createTimer(HideAndSeekAPI.getInstance().getLangProvider().getMessage(getKey("seek_timer", PositionType.HIDER), this, lobby, map.getData(PositionType.HIDER)), map.getSeekTime(), false, new AbstractTimer.TimerCallback() {
            @Override
            public void tick(int timeLeft) {
                if(timeLeft > 0 && timeLeft < 6) {
                    playTickSound();
                }
            }

            @Override
            public void finish() {
                endGame(PositionType.HIDER);
            }
        });

        seekerTimer = MidnightCoreAPI.getInstance().createTimer(HideAndSeekAPI.getInstance().getLangProvider().getMessage(getKey("seek_timer", PositionType.SEEKER), this, lobby, map.getData(PositionType.SEEKER)), map.getSeekTime(), false, null);
        AbstractTimer mainSeekerTimer = MidnightCoreAPI.getInstance().createTimer(HideAndSeekAPI.getInstance().getLangProvider().getMessage(getKey("seek_timer", PositionType.MAIN_SEEKER), this, lobby, map.getData(PositionType.MAIN_SEEKER)), map.getSeekTime(), false, null);

        for(UUID u : getPlayerIds()) {
            switch(getPosition(u)) {
                case HIDER:
                    hiderTimer.addPlayer(u);
                    break;
                case SEEKER:
                    seekerTimer.addPlayer(u);
                    break;
                case MAIN_SEEKER:
                    mainSeekerTimer.addPlayer(u);
            }
        }

        timers.add(hiderTimer);
        timers.add(seekerTimer);
        timers.add(mainSeekerTimer);

        hiderTimer.start();
        seekerTimer.start();
        mainSeekerTimer.start();

    }

    @Override
    public final void onDamaged(UUID u, UUID damager, DamageSource source, float amount) {

        if(state == ClassicGameState.SEEKING && positions.containsKey(u) && !positions.get(u).isSeeker()) {

            if (damager != null) {

                if (!positions.containsKey(damager) || !positions.get(damager).isSeeker()) return;

                setPlayerSeeker(u);
                broadcastTagMessage(u, damager, null);

            } else if (map.getTagSources().contains(source)) {

                setPlayerSeeker(u);
                broadcastTagMessage(u, null, source);

            }
        }

        if(map.getResetSources().contains(source)) {
            if(positions.get(u).isSeeker()) {
                locations.put(u, map.getSeekerSpawn());
            } else {
                locations.put(u, map.getHiderSpawn());
            }
            toTeleport.add(u);
        }

    }

    @Override
    protected void onShutdown() {
        cancelTimers();
        unloadWorld();
    }

    @Override
    protected void endGame(PositionType winner) {

        state = ClassicGameState.ENDING;

        cancelTimers();

        broadcastVictoryTitle(winner);

        String s = HideAndSeekAPI.getInstance().getLangProvider().getMessage(getKey("end_timer", null), this, lobby, lobby.getLobby());
        HideAndSeekAPI.getLogger().warn(s);

        AbstractTimer endTimer = MidnightCoreAPI.getInstance().createTimer(s, 15, false, new AbstractTimer.TimerCallback() {
            @Override
            public void tick(int secondsLeft) { }

            @Override
            public void finish() {
                shutdown();
            }
        });

        for(UUID u : getPlayerIds()) {
            endTimer.addPlayer(u);
        }

        timers.add(endTimer);
        endTimer.start();
    }

    @Override
    protected void checkVictory() {

        if(state == ClassicGameState.ENDING) return;

        if(positions.size() <= 1) {
            endGame(null);
            return;
        }

        int hiders = 0;
        int seekers = 0;
        for(PositionType t : positions.values()) {

            if(t.isSeeker()) {
                seekers++;
            } else {
                hiders++;
            }
        }

        if(hiders == 0) {
            endGame(PositionType.SEEKER);
        } else if(seekers == 0) {
            endGame(PositionType.HIDER);
        }

    }

    private void cancelTimers() {
        for(AbstractTimer t : timers) {
            t.cancel();
        }
        timers.clear();
    }

    //protected abstract AbstractTimer createTimer(String parse, String suffix, int time, Color apply, AbstractTimer.TimerCallback cb);
    protected abstract void loadWorld(AbstractMap map, Runnable callback);
    protected abstract void unloadWorld();

    protected abstract void playTickSound();
    protected abstract void playReleaseSound();

    protected abstract void broadcastTagMessage(UUID tagged, UUID tagger, DamageSource src);

    protected abstract void setupPlayer(UUID u);
    protected abstract void broadcastVictoryTitle(PositionType winner);

    protected void setPlayerSeeker(UUID player) {

        positions.put(player, PositionType.SEEKER);
        hiderTimer.removePlayer(player);
        seekerTimer.addPlayer(player);

        checkVictory();
    }

    protected String getKey(String key, PositionType optional) {

        AbstractLangProvider prov = HideAndSeekAPI.getInstance().getLangProvider();

        String out;
        if(optional == null) {

            if(prov.hasMessage((out = lobby.getLobby().getGameType().getId() + "." + key))) {
                return out;
            }

        } else {

            if(prov.hasMessage((out = lobby.getLobby().getGameType().getId() + "." + key + "." + optional.getId()))) {
                return out;
            } else if(prov.hasMessage((out = lobby.getLobby().getGameType().getId() + "." + key + "." + (optional.isSeeker() ? "seeker" : "hider")))) {
                return out;
            } else if(prov.hasMessage((out = lobby.getLobby().getGameType().getId() + "." + key))) {
                return out;
            } else if(prov.hasMessage((out = "game." + key + "." + optional.getId()))) {
                return out;
            } else if(prov.hasMessage((out = "game." + key + "." + (optional.isSeeker() ? "seeker" : "hider")))) {
                return out;
            }
        }

        return "game." + key;
    }


    public enum ClassicGameState {
        UNINITIALIZED,
        HIDING,
        SEEKING,
        ENDING
    }
}
