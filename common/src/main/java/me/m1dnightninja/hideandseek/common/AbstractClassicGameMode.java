package me.m1dnightninja.hideandseek.common;

import me.m1dnightninja.hideandseek.api.*;
import me.m1dnightninja.hideandseek.api.game.*;
import me.m1dnightninja.hideandseek.api.game.AbstractMap;
import me.m1dnightninja.midnightcore.api.MidnightCoreAPI;
import me.m1dnightninja.midnightcore.api.math.Color;
import me.m1dnightninja.midnightcore.api.math.Vec3d;
import me.m1dnightninja.midnightcore.api.module.lang.ILangProvider;
import me.m1dnightninja.midnightcore.api.text.AbstractTitle;
import me.m1dnightninja.midnightcore.api.text.AbstractTimer;
import me.m1dnightninja.midnightcore.api.text.MComponent;
import me.m1dnightninja.midnightcore.api.text.MStyle;
import me.m1dnightninja.midnightcore.api.text.AbstractCustomScoreboard;
import org.apache.commons.lang3.RandomStringUtils;

import java.util.*;

public abstract class AbstractClassicGameMode extends AbstractGameInstance {

    protected ClassicGameState state = ClassicGameState.UNINITIALIZED;

    protected final UUID seeker;
    protected final AbstractMap map;

    protected final List<UUID> toTeleport = new ArrayList<>();
    protected final HashMap<UUID, Vec3d> locations = new HashMap<>();
    protected final HashMap<UUID, AbstractCustomScoreboard> scoreboards = new HashMap<>();

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

                AbstractTitle title = MidnightCoreAPI.getInstance().createTitle(HideAndSeekAPI.getInstance().getLangProvider().getMessage(getKey("start_title", u, positions.get(u)), u, u, map.getData(positions.get(u))), AbstractTitle.TITLE);
                AbstractTitle subtitle;

                if(positions.get(u).isSeeker()) {

                    subtitle = MidnightCoreAPI.getInstance().createTitle(HideAndSeekAPI.getInstance().getLangProvider().getMessage(getKey("start_subtitle", u, PositionType.MAIN_SEEKER), u, u, map.getData(PositionType.HIDER)), AbstractTitle.SUBTITLE);

                } else {

                    subtitle = MidnightCoreAPI.getInstance().createTitle(HideAndSeekAPI.getInstance().getLangProvider().getMessage(getKey("start_subtitle", u, PositionType.HIDER), u, u, map.getData(PositionType.MAIN_SEEKER)), AbstractTitle.SUBTITLE);

                }

                title.sendToPlayer(u);
                subtitle.sendToPlayer(u);

                AbstractCustomScoreboard sb = MidnightCoreAPI.getInstance().createScoreboard(RandomStringUtils.random(15, true, true), MComponent.createTextComponent("HideAndSeek").withStyle(new MStyle().withColor(Color.fromRGBI(14)).withBold(true)));

                sb.setLine(7, MComponent.createTextComponent("                         "));
                sb.setLine(6, MComponent.createTextComponent("Phase: ").addChild(HideAndSeekAPI.getInstance().getLangProvider().getMessage("phase.hiding", u, map.getData(PositionType.HIDER))));
                sb.setLine(5, MComponent.createTextComponent("                         "));
                sb.setLine(4, MComponent.createTextComponent("Role: ").addChild(map.getData(positions.get(u)).getName()));
                sb.setLine(3, MComponent.createTextComponent("Map: ").addChild(map.getName()));
                sb.setLine(2, MComponent.createTextComponent("                         "));
                sb.setLine(1, map.getData(PositionType.HIDER).getPluralName().copy().withStyle(new MStyle()).addChild(MComponent.createTextComponent(": ").addChild(MComponent.createTextComponent(getPlayerCount() - 1 + "").withStyle(new MStyle().withColor(map.getData(PositionType.HIDER).getColor())))));

                sb.addPlayer(u);

                scoreboards.put(u, sb);

            } catch (Throwable th) {
                th.printStackTrace();
                shutdown();
                return;
            }
        }

        AbstractTimer startTimer = MidnightCoreAPI.getInstance().createTimer(HideAndSeekAPI.getInstance().getLangProvider().getMessage(getKey("hide_timer", null,null), (UUID) null, this, lobby, map.getData(PositionType.MAIN_SEEKER)), map.getHideTime(), false, timeLeft -> {
            if(timeLeft > 0 && timeLeft < 6) {
                playTickSound();
            } else if(timeLeft == 0) {
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

        hiderTimer = MidnightCoreAPI.getInstance().createTimer(HideAndSeekAPI.getInstance().getLangProvider().getMessage(getKey("seek_timer", null, PositionType.HIDER), (UUID) null, this, lobby, map.getData(PositionType.HIDER)), map.getSeekTime(), false, timeLeft -> {
            if(timeLeft > 0 && timeLeft < 6) {
                playTickSound();
            } else if(timeLeft == 0) {
                endGame(PositionType.HIDER);
            }
        });

        seekerTimer = MidnightCoreAPI.getInstance().createTimer(HideAndSeekAPI.getInstance().getLangProvider().getMessage(getKey("seek_timer", null, PositionType.SEEKER), (UUID) null, this, lobby, map.getData(PositionType.SEEKER)), map.getSeekTime(), false, null);
        AbstractTimer mainSeekerTimer = MidnightCoreAPI.getInstance().createTimer(HideAndSeekAPI.getInstance().getLangProvider().getMessage(getKey("seek_timer", null, PositionType.MAIN_SEEKER), (UUID) null, this, lobby, map.getData(PositionType.MAIN_SEEKER)), map.getSeekTime(), false, null);

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

        for(Map.Entry<UUID,AbstractCustomScoreboard> ent : scoreboards.entrySet()) {

            AbstractCustomScoreboard sb = ent.getValue();

            sb.setLine(6, MComponent.createTextComponent("Phase: ").addChild(HideAndSeekAPI.getInstance().getLangProvider().getMessage("phase.seeking", ent.getKey(), map.getData(PositionType.SEEKER))));
            sb.update();
        }
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

        for(AbstractCustomScoreboard cs : scoreboards.values()) {
            cs.clearPlayers();
        }

        unloadWorld();
    }

    @Override
    protected void endGame(PositionType winner) {

        state = ClassicGameState.ENDING;

        cancelTimers();

        for(UUID u : players) {
            if (winner == null) {
                HideAndSeekAPI.getInstance().getLangProvider().sendTitle(getKey("end_title_draw", u, null), u, AbstractTitle.TITLE);
            } else {
                if (winner.isSeeker() == positions.get(u).isSeeker()) {
                    HideAndSeekAPI.getInstance().getLangProvider().sendTitle(getKey("end_title_win", u, winner), u, AbstractTitle.TITLE, map.getData(winner));
                } else {
                    HideAndSeekAPI.getInstance().getLangProvider().sendTitle(getKey("end_title_lose", u, winner), u, AbstractTitle.TITLE,  map.getData(winner));
                }
            }
        }
        broadcastVictorySound(winner);

        MComponent s = HideAndSeekAPI.getInstance().getLangProvider().getMessage(getKey("end_timer", null,null), (UUID) null, this, lobby, lobby.getLobby());

        AbstractTimer endTimer = MidnightCoreAPI.getInstance().createTimer(s, 15, false, secondsLeft -> {
            if(secondsLeft == 0) shutdown();
        });

        for(UUID u : getPlayerIds()) {
            endTimer.addPlayer(u);
        }

        timers.add(endTimer);
        endTimer.start();

        for(Map.Entry<UUID, AbstractCustomScoreboard> ent : scoreboards.entrySet()) {

            AbstractCustomScoreboard sb = ent.getValue();

            sb.setLine(6, MComponent.createTextComponent("Phase: ").addChild(HideAndSeekAPI.getInstance().getLangProvider().getMessage("phase.ended", ent.getKey())));
            sb.update();
        }
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

        for(AbstractCustomScoreboard sb : scoreboards.values()) {
            sb.setLine(1, map.getData(PositionType.HIDER).getName().copy().withStyle(new MStyle()).addChild(MComponent.createTextComponent(": ").addChild(MComponent.createTextComponent(countPosition(PositionType.HIDER) + "").withStyle(new MStyle().withColor(map.getData(PositionType.HIDER).getColor())))));
            sb.update();
        }

    }

    private void cancelTimers() {
        for(AbstractTimer t : timers) {
            t.cancel();
        }
        timers.clear();
    }

    protected abstract void loadWorld(AbstractMap map, Runnable callback);
    protected abstract void unloadWorld();

    protected abstract void playTickSound();
    protected abstract void playReleaseSound();

    protected abstract void broadcastTagMessage(UUID tagged, UUID tagger, DamageSource src);

    protected abstract void setupPlayer(UUID u);
    protected abstract void broadcastVictorySound(PositionType winner);

    protected void setPlayerSeeker(UUID player) {

        positions.put(player, PositionType.SEEKER);
        hiderTimer.removePlayer(player);
        seekerTimer.addPlayer(player);

        scoreboards.get(player).setLine(4, MComponent.createTextComponent("Role: ").addChild(map.getData(positions.get(player)).getName()));

        checkVictory();
    }

    protected String getKey(String key, UUID player, PositionType optional) {

        // Kinda cringe logic to get cascading messages

        ILangProvider prov = HideAndSeekAPI.getInstance().getLangProvider();

        String out;
        if(optional == null) {

            if(prov.hasKey((out = lobby.getLobby().getGameType().getId() + "." + key), player)) {
                return out;
            }

        } else {

            if(prov.hasKey((out = lobby.getLobby().getGameType().getId() + "." + key + "." + optional.getId()), player)) {
                return out;

            } else if(prov.hasKey((out = lobby.getLobby().getGameType().getId() + "." + key + "." + (optional.isSeeker() ? "seeker" : "hider")), player)) {
                return out;

            } else if(prov.hasKey((out = lobby.getLobby().getGameType().getId() + "." + key), player)) {
                return out;

            } else if(prov.hasKey((out = "game." + key + "." + optional.getId()), player)) {
                return out;

            } else if(prov.hasKey((out = "game." + key + "." + (optional.isSeeker() ? "seeker" : "hider")), player)) {
                return out;
            }
        }

        return "game." + key;
    }

    @Override
    protected void onPlayerRemoved(UUID u) {

        if(scoreboards.containsKey(u)) scoreboards.get(u).removePlayer(u);
        super.onPlayerRemoved(u);
    }

    public enum ClassicGameState {
        UNINITIALIZED,
        HIDING,
        SEEKING,
        ENDING
    }
}
