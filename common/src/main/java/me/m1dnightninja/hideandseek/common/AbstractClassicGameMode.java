package me.m1dnightninja.hideandseek.common;

import me.m1dnightninja.hideandseek.api.*;
import me.m1dnightninja.hideandseek.api.game.*;
import me.m1dnightninja.hideandseek.api.game.Map;
import me.m1dnightninja.midnightcore.api.MidnightCoreAPI;
import me.m1dnightninja.midnightcore.api.math.Color;
import me.m1dnightninja.midnightcore.api.math.Vec3d;
import me.m1dnightninja.midnightcore.api.module.IVanishModule;
import me.m1dnightninja.midnightcore.api.module.lang.CustomPlaceholder;
import me.m1dnightninja.midnightcore.api.module.lang.CustomPlaceholderInline;
import me.m1dnightninja.midnightcore.api.module.lang.ILangProvider;
import me.m1dnightninja.midnightcore.api.player.MPlayer;
import me.m1dnightninja.midnightcore.api.text.*;

import java.util.*;

public abstract class AbstractClassicGameMode extends AbstractGameInstance {

    // Instance Variables

    // Game state
    protected ClassicGameState state = ClassicGameState.UNINITIALIZED;

    // Initial Seeker and Map
    protected final MPlayer seeker;
    protected final Map map;

    // Keep track of locations and who to teleport
    protected final List<MPlayer> toTeleport = new ArrayList<>();
    protected final HashMap<MPlayer, Vec3d> locations = new HashMap<>();

    // Keep track of scoreboards
    protected final HashMap<MPlayer, AbstractCustomScoreboard> scoreboards = new HashMap<>();

    // Timers
    private final List<AbstractTimer> timers = new ArrayList<>();
    private AbstractTimer seekerTimer;
    private AbstractTimer hiderTimer;

    protected final IVanishModule vanishModule;

    // Constructor
    protected AbstractClassicGameMode(AbstractLobbySession lobby, MPlayer seeker, Map map) {
        super(lobby);

        this.seeker = seeker == null ? getPlayers().get(HideAndSeekAPI.getInstance().getRandom().nextInt(getPlayerCount())) : seeker;
        this.map = map == null ? lobby.getLobby().getMaps().get(HideAndSeekAPI.getInstance().getRandom().nextInt(lobby.getLobby().getMaps().size())) : map;

        this.vanishModule = MidnightCoreAPI.getInstance().getModule(IVanishModule.class);

    }

    // Start the game
    @Override
    public final void start() {

        // Load the world, start hiding when done
        loadWorld(map, this::startHiding);

    }

    // Allow other classes to obtain the current map
    @Override
    public Map getMap() {
        return map;
    }

    // Start the game
    protected void startHiding() {

        // Change state
        state = ClassicGameState.HIDING;

        // Create a timer for when the seeker will be released
        AbstractTimer startTimer = MidnightCoreAPI.getInstance().createTimer(HideAndSeekAPI.getInstance().getLangProvider().getMessage(getKey("hide_timer", null,null), (MPlayer) null, this, lobby, map.getData(PositionType.MAIN_SEEKER)), map.getHideTime(), false, timeLeft -> {
            if(timeLeft > 0 && timeLeft < 6) {
                playTickSound();
            } else if(timeLeft == 0) {
                playReleaseSound();
                startSeeking();
            }
        });

        // Loop through each player
        for(MPlayer u : getPlayers()) {

            try {
                setPosition(u, u.equals(seeker) ? PositionType.MAIN_SEEKER : PositionType.HIDER);

                // Tell the implementation to setup the player
                setupPlayer(u);

                // Create and send a title and subtitle
                Title title = new Title(HideAndSeekAPI.getInstance().getLangProvider().getMessage(getKey("start_title", u, positions.get(u)), u, u, map.getData(positions.get(u))), Title.TITLE);
                Title subtitle;

                if (positions.get(u).isSeeker()) {

                    subtitle = new Title(HideAndSeekAPI.getInstance().getLangProvider().getMessage(getKey("start_subtitle", u, PositionType.MAIN_SEEKER), u, u, map.getData(PositionType.HIDER)), Title.SUBTITLE);

                } else {

                    subtitle = new Title(HideAndSeekAPI.getInstance().getLangProvider().getMessage(getKey("start_subtitle", u, PositionType.HIDER), u, u, map.getData(PositionType.MAIN_SEEKER)), Title.SUBTITLE);

                }

                u.sendTitle(title);
                u.sendTitle(subtitle);

                MComponent roleName = map.getData(PositionType.HIDER).getPluralName().copy();
                roleName.clearStyle();

                // Create and send a scoreboard
                AbstractCustomScoreboard sb = MidnightCoreAPI.getInstance().createScoreboard(AbstractCustomScoreboard.generateRandomId(), MComponent.createTextComponent("HideAndSeek").withStyle(new MStyle().withColor(Color.fromRGBI(14)).withBold(true)));

                sb.setLine(7, MComponent.createTextComponent("                         "));
                sb.setLine(6, MComponent.createTextComponent("Phase: ").addChild(HideAndSeekAPI.getInstance().getLangProvider().getMessage("phase.hiding", u, map.getData(PositionType.HIDER))));
                sb.setLine(5, MComponent.createTextComponent("                         "));
                sb.setLine(4, MComponent.createTextComponent("Role: ").addChild(map.getData(positions.get(u)).getName()));
                sb.setLine(3, MComponent.createTextComponent("Map: ").addChild(map.getName()));
                sb.setLine(2, MComponent.createTextComponent("                         "));
                sb.setLine(1, roleName.addChild(MComponent.createTextComponent(": ").addChild(MComponent.createTextComponent(getPlayerCount() - 1 + "").withStyle(new MStyle().withColor(map.getData(PositionType.HIDER).getColor())))));

                sb.addPlayer(u);
                scoreboards.put(u, sb);

                startTimer.addPlayer(u);

                if(!positions.get(u).isSeeker()) vanishModule.hidePlayerFor(u, seeker);

            } catch(Throwable th) {
                th.printStackTrace();
            }
        }

        // Keep track of the timer, start the timer
        timers.add(startTimer);
        startTimer.start();

    }

    // Switch from hiding to seeking
    protected void startSeeking() {

        // Update state
        state = ClassicGameState.SEEKING;

        // Cancel all running timers to prevent thread weirdness
        cancelTimers();

        // Create a new timer for all applicable roles
        hiderTimer = MidnightCoreAPI.getInstance().createTimer(HideAndSeekAPI.getInstance().getLangProvider().getMessage(getKey("seek_timer", null, PositionType.HIDER), (MPlayer) null, this, lobby, map.getData(PositionType.HIDER)), map.getSeekTime(), false, timeLeft -> {
            if(timeLeft > 0 && timeLeft < 6) {
                playTickSound();
            } else if(timeLeft == 0) {
                endGame(PositionType.HIDER);
            }
        });

        seekerTimer = MidnightCoreAPI.getInstance().createTimer(HideAndSeekAPI.getInstance().getLangProvider().getMessage(getKey("seek_timer", null, PositionType.SEEKER), (MPlayer) null, this, lobby, map.getData(PositionType.SEEKER)), map.getSeekTime(), false, null);
        AbstractTimer mainSeekerTimer = MidnightCoreAPI.getInstance().createTimer(HideAndSeekAPI.getInstance().getLangProvider().getMessage(getKey("seek_timer", null, PositionType.MAIN_SEEKER), (MPlayer) null, this, lobby, map.getData(PositionType.MAIN_SEEKER)), map.getSeekTime(), false, null);

        // Loop through all players, show them timers as appropriate
        for(MPlayer u : getPlayers()) {
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

        for(MPlayer pl : getPlayersFiltered(pl -> positions.get(pl).isSeeker())) {
            for(MPlayer hid : getPlayersFiltered(hid -> !positions.get(hid).isSeeker())) {
                MidnightCoreAPI.getLogger().warn("Showing player " + hid.getName() + " for " + pl.getName());
                vanishModule.showPlayerFor(hid, pl);
            }
        }

        // Keep track of new timers
        timers.add(hiderTimer);
        timers.add(seekerTimer);
        timers.add(mainSeekerTimer);

        // Start new timers
        hiderTimer.start();
        seekerTimer.start();
        mainSeekerTimer.start();

        // Update all scoreboards
        for(java.util.Map.Entry<MPlayer,AbstractCustomScoreboard> ent : scoreboards.entrySet()) {

            AbstractCustomScoreboard sb = ent.getValue();

            sb.setLine(6, MComponent.createTextComponent("Phase: ").addChild(HideAndSeekAPI.getInstance().getLangProvider().getMessage("phase.seeking", ent.getKey(), map.getData(PositionType.SEEKER))));
            sb.update();
        }
    }


    // Callback when a player takes damage
    @Override
    public final void onDamaged(MPlayer u, MPlayer damager, DamageSource source, float amount) {

        // Check if the player should be teleported to their spawn or not
        if(map.getResetSources().contains(source)) {
            if(positions.get(u).isSeeker()) {
                locations.put(u, map.getSeekerSpawn());
            } else {
                locations.put(u, map.getHiderSpawn());
            }
            toTeleport.add(u);
        }

        // Check state, do not tag if not seeking yet
        if(state == ClassicGameState.SEEKING && positions.containsKey(u) && !positions.get(u).isSeeker()) {

            // Check for a tagger
            if (damager != null) {

                if (!positions.containsKey(damager) || !positions.get(damager).isSeeker()) return;
                if(!tagPlayer(u, damager, PositionType.SEEKER)) return;

                broadcastTagMessage(u, damager, source);
                checkVictory();

            // Environment tag
            } else if (map.getTagSources().contains(source)) {

                if(!tagPlayer(u, null, PositionType.SEEKER)) return;
                broadcastTagMessage(u, null, source);
                checkVictory();

            }
        }


    }

    // Callback when the game is shutdown
    @Override
    protected void onShutdown() {

        // Cancel all timers
        cancelTimers();

        // Hide the scoreboard from all players
        for(AbstractCustomScoreboard cs : scoreboards.values()) {
            cs.clearPlayers();
        }

        // Unload the temporary world
        unloadWorld();
    }

    // Callback when the game ends
    @Override
    protected void endGame(PositionType winner) {

        // Update state
        state = ClassicGameState.ENDING;

        // Cancel all timers
        cancelTimers();

        // Loop through all players, send titles as appropriate
        for(MPlayer u : players) {
            if(classes.get(u) != null) {

                classes.get(u).executeCommands(AbstractClass.CommandActivationPoint.END, u, null);
            }
            if (winner == null) {
                HideAndSeekAPI.getInstance().getLangProvider().sendTitle(getKey("end_title_draw", u, null), u, Title.TITLE);
            } else {
                if (winner.isSeeker() == positions.get(u).isSeeker()) {
                    HideAndSeekAPI.getInstance().getLangProvider().sendTitle(getKey("end_title_win", u, winner), u, Title.TITLE, map.getData(winner));
                } else {
                    HideAndSeekAPI.getInstance().getLangProvider().sendTitle(getKey("end_title_lose", u, winner), u, Title.TITLE,  map.getData(winner));
                }
            }
        }

        // Tell implementation to broadcast sound
        broadcastVictorySound(winner);

        // Create a timer for when the game will stop
        MComponent s = HideAndSeekAPI.getInstance().getLangProvider().getMessage(getKey("end_timer", null,null), (MPlayer) null, this, lobby, lobby.getLobby());

        AbstractTimer endTimer = MidnightCoreAPI.getInstance().createTimer(s, 15, false, secondsLeft -> {

            if((secondsLeft + 2) % 4 == 0) {

                Color c = winner == null ? new Color(0xFFFFFF) : map.getData(winner).getColor();
                for(Vec3d loc : map.getFireworkSpawners()) {
                    spawnFirework(loc, c, false, HideAndSeekAPI.getInstance().getRandom().nextBoolean(), HideAndSeekAPI.getInstance().getRandom().nextBoolean());
                }
            }
            if(secondsLeft == 0) shutdown();
        });

        // Show the timer to everyone
        for(MPlayer u : getPlayers()) {
            endTimer.addPlayer(u);
        }

        // Keep track of and start timer
        timers.add(endTimer);
        endTimer.start();

        // Update all scoreboards
        for(java.util.Map.Entry<MPlayer, AbstractCustomScoreboard> ent : scoreboards.entrySet()) {

            AbstractCustomScoreboard sb = ent.getValue();

            sb.setLine(6, MComponent.createTextComponent("Phase: ").addChild(HideAndSeekAPI.getInstance().getLangProvider().getMessage("phase.ended", ent.getKey())));
            sb.update();
        }
    }

    // Check to see if the game should end
    @Override
    protected void checkVictory() {

        // Ignore if the game is already ended
        if(state == ClassicGameState.ENDING) return;

        // Create a draw if only one player remains
        if(positions.size() <= 1) {
            endGame(null);
            return;
        }

        // Count hiders and seekers
        int hiders = 0;
        int seekers = 0;
        for(PositionType t : positions.values()) {

            if(t.isSeeker()) {
                seekers++;
            } else {
                hiders++;
            }
        }

        // If either role is empty, the other one wins
        if(hiders == 0) {
            endGame(PositionType.SEEKER);
        } else if(seekers == 0) {
            endGame(PositionType.HIDER);
        }

        // Update all scoreboards to reflect new data
        for(AbstractCustomScoreboard sb : scoreboards.values()) {

            MComponent roleName = map.getData(PositionType.HIDER).getPluralName().copy();
            roleName.clearStyle();

            sb.setLine(1, roleName.addChild(MComponent.createTextComponent(": ").addChild(MComponent.createTextComponent(countPosition(PositionType.HIDER) + "").withStyle(new MStyle().withColor(map.getData(PositionType.HIDER).getColor())))));
            sb.update();
        }

    }

    // Cancel all timers, killing their threads
    private void cancelTimers() {
        // Loop through all timers and cancel them
        for(AbstractTimer t : timers) {
            t.cancel();
        }
        // Remove references to the timers so the JVM can get rid of them
        timers.clear();
    }

    // Broadcasts a tag message to the lobby
    protected void broadcastTagMessage(MPlayer tagged, MPlayer tagger, DamageSource src) {

        if (tagger != null) {

            for(MPlayer u : players) {

                MComponent message = HideAndSeekAPI.getInstance().getLangProvider().getMessage("game.tag", u, tagged, map, map.getData(getPosition(tagged)), new CustomPlaceholderInline("tagger_color", map.getData(getPosition(tagger)).getColor().toHex()), new CustomPlaceholder("tagger_name", tagger.getName()));
                message.addChild(getRemainsText(u));
                message.send(u);

            }

        } else if(src != null) for(MPlayer u : players) {

            MComponent message = MComponent.createTextComponent("");
            message.addChild(tagged.getName().withStyle(new MStyle().withColor(map.getData(positions.get(u)).getColor())));
            message.addChild(map.getDeathMessage(src, u, tagged));
            message.addChild(getRemainsText(u));
            message.send(u);

        }

    }

    // Get the text to send for "x hiders remain."
    private MComponent getRemainsText(MPlayer u) {
        int hiders = 0;
        for(PositionType t : positions.values()) {
            if (t == PositionType.HIDER) hiders++;
        }

        return HideAndSeekAPI.getInstance().getLangProvider().getMessage((hiders == 1 ? "game.remains.singular" : "game.remains"), u, new CustomPlaceholderInline("hider_count", hiders+""), map.getData(PositionType.HIDER), map);
    }


    protected abstract void loadWorld(Map map, Runnable callback);
    protected abstract void unloadWorld();

    protected abstract void playTickSound();
    protected abstract void playReleaseSound();

    protected abstract void setupPlayer(MPlayer u);
    protected abstract void broadcastVictorySound(PositionType winner);
    protected abstract void spawnFirework(Vec3d location, Color c1, boolean instant, boolean large, boolean flicker);

    // Callback when a player's role changes
    @Override
    protected boolean onPositionChanged(MPlayer u, PositionType oldRole, PositionType newRole) {

        if(state != ClassicGameState.SEEKING) return true;

        if(oldRole == PositionType.HIDER) {
            hiderTimer.removePlayer(u);
        }
        if(newRole == PositionType.SEEKER) {

            seekerTimer.addPlayer(u);

            scoreboards.get(u).setLine(4, MComponent.createTextComponent("Role: ").addChild(map.getData(newRole).getName()));

            spawnFirework(u.getLocation(), map.getData(PositionType.SEEKER).getColor(), true, true, false);
            spawnFirework(u.getLocation(), new Color(0xFFFFFF), true, false, false);

            checkVictory();
        }

        return true;
    }

    @Override
    protected boolean onClassChanged(MPlayer u, AbstractClass oldClass, AbstractClass newClass) { return true; }

    @Override
    public int countPosition(PositionType t) {
        return super.countPosition(t);
    }


    // Find the most specific key for sending lang entries to players
    protected String getKey(String key, MPlayer player, PositionType optional) {

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

        out = "game." + key;
        return out;
    }

    // Callback when a player is removed from the game
    @Override
    protected void onPlayerRemoved(MPlayer u) {

        // Remove their scoreboard
        if(scoreboards.containsKey(u)) {
            scoreboards.get(u).removePlayer(u);
            scoreboards.remove(u);
        }
        super.onPlayerRemoved(u);
    }

    // State enum
    public enum ClassicGameState {
        UNINITIALIZED,
        HIDING,
        SEEKING,
        ENDING
    }
}
