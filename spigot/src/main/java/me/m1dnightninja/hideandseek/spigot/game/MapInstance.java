package me.m1dnightninja.hideandseek.spigot.game;

import me.m1dnightninja.hideandseek.api.HideAndSeekAPI;
import me.m1dnightninja.hideandseek.api.core.AbstractSession;
import me.m1dnightninja.hideandseek.api.game.Map;
import me.m1dnightninja.hideandseek.api.game.PositionData;
import me.m1dnightninja.hideandseek.api.game.PositionType;
import me.m1dnightninja.midnightcore.api.math.Vec3d;
import me.m1dnightninja.midnightcore.api.text.AbstractCustomScoreboard;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.ScoreboardManager;
import org.bukkit.scoreboard.Team;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;

public class MapInstance {

    private final Map base;
    private final World world;

    private final Location hiderSpawn;
    private final Location seekerSpawn;

    private final Scoreboard scoreboard;
    private final HashMap<PositionType, Team> teams = new HashMap<>();

    private final AbstractSession session;

    public MapInstance(AbstractSession session, Map base, World world, boolean createTeams) {

        this.base = base;
        this.world = world;
        this.session = session;

        Vec3d hiderLoc = base.getHiderSpawn();
        Vec3d seekerLoc = base.getSeekerSpawn();

        this.hiderSpawn = new Location(world, hiderLoc.getX(), hiderLoc.getY(), hiderLoc.getZ(), base.getHiderRotation(), 0.0f);
        this.seekerSpawn = new Location(world, seekerLoc.getX(), seekerLoc.getY(), seekerLoc.getZ(), base.getSeekerRotation(), 0.0f);

        ScoreboardManager manager = Bukkit.getScoreboardManager();

        if(createTeams && manager != null) {
            scoreboard = manager.getNewScoreboard();

            for(PositionType t : PositionType.values()) {

                PositionData data = base.getData(t);

                ChatColor color = ChatColor.getByChar(data.getColor().toHex());
                if(color == null) color = ChatColor.WHITE;

                Team team = scoreboard.registerNewTeam(AbstractCustomScoreboard.generateRandomId());
                team.setColor(color);

                if(!t.isSeeker()) {
                    team.setOption(Team.Option.NAME_TAG_VISIBILITY, Team.OptionStatus.NEVER);
                }

                teams.put(t, team);
            }

        } else {
            scoreboard = null;
        }

        if(base.hasRandomTime()) {
            world.setTime(HideAndSeekAPI.getInstance().getRandom().nextInt(24000));
        }

        if(base.hasRain()) {
            world.setWeatherDuration(Integer.MAX_VALUE);
        }

        if(base.hasThunder()) {
            world.setThundering(true);
            world.setThunderDuration(Integer.MAX_VALUE);
        }
    }

    public Map getBaseMap() {
        return base;
    }

    public World getWorld() {
        return world;
    }

    public Location getHiderSpawn() {
        return hiderSpawn;
    }

    public Location getSeekerSpawn() {
        return seekerSpawn;
    }

    public Scoreboard getScoreboard() {
        return scoreboard;
    }

    public void addToTeam(Player player, PositionType type) {

        if(scoreboard == null) return;

        if(!teams.containsKey(type)) return;
        player.setScoreboard(scoreboard);

        teams.get(type).addEntry(player.getName());

    }

    public void removeFromTeam(Player player) {

        if(scoreboard == null) return;

        for(Team t : teams.values()) {

            t.removeEntry(player.getName());
        }

    }

    public void onJoin(Player pl) {

        if(scoreboard != null) pl.setScoreboard(scoreboard);

        if(base.getResourcePack() != null) {
            pl.setResourcePack(base.getResourcePack(), base.getResourcePackHash().getBytes(StandardCharsets.UTF_8));
        }
    }

    public void onLeave(Player pl) {

        if(scoreboard != null) {
            ScoreboardManager manager = Bukkit.getScoreboardManager();

            if (manager != null) {
                pl.setScoreboard(manager.getMainScoreboard());
            }
        }

        if(base.getResourcePack() != null) {

            pl.setResourcePack(HideAndSeekAPI.getInstance().getResourcePack(), HideAndSeekAPI.getInstance().getResourcePackHash().getBytes(StandardCharsets.UTF_8));
        }

    }

    public void clearTeams() {

        for(Team t : teams.values()) {
            t.unregister();
        }
    }

}
