package me.m1dnightninja.hideandseek.fabric;

import me.m1dnightninja.hideandseek.api.AbstractMap;
import me.m1dnightninja.hideandseek.api.AbstractPositionData;
import me.m1dnightninja.hideandseek.api.HideAndSeekAPI;
import me.m1dnightninja.hideandseek.api.PositionType;
import me.m1dnightninja.midnightcore.fabric.api.Location;
import net.minecraft.ChatFormatting;
import net.minecraft.server.ServerScoreboard;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.scores.PlayerTeam;
import net.minecraft.world.scores.Team;
import org.apache.commons.lang3.RandomStringUtils;

import java.util.HashMap;

public class MapInstance {

    private final AbstractMap base;
    private final ServerLevel world;

    private final Location hiderSpawn;
    private final Location seekerSpawn;

    private ServerScoreboard scoreboard;
    private final HashMap<PositionType, PlayerTeam> teams = new HashMap<>();

    public MapInstance(AbstractMap base, ServerLevel world, boolean createTeams) {
        this.base = base;
        this.world = world;

        hiderSpawn = new Location(world.dimension().location(), base.getHiderSpawn().getX(), base.getHiderSpawn().getY(), base.getHiderSpawn().getZ(), base.getHiderRotation(), 0);
        seekerSpawn = new Location(world.dimension().location(), base.getSeekerSpawn().getX(), base.getSeekerSpawn().getY(), base.getSeekerSpawn().getZ(), base.getSeekerRotation(), 0);

        if(createTeams) {
            scoreboard = world.getScoreboard();

            for (PositionType t : PositionType.values()) {
                AbstractPositionData data = base.getData(t);
                if (data == null) continue;

                PlayerTeam team = scoreboard.addPlayerTeam(RandomStringUtils.random(16, true, false));
                team.setColor(ChatFormatting.getById(data.getColor().toRGBI()));

                if (!t.isSeeker()) {
                    team.setNameTagVisibility(Team.Visibility.NEVER);
                }

                teams.put(t, team);
            }
        }

        if(base.hasRandomTime()) {
            world.setDayTime(HideAndSeekAPI.getInstance().getRandom().nextInt(24000));
        }

        if(base.hasRain() || base.hasThunder()) {
            world.setWeatherParameters(0, Integer.MAX_VALUE, base.hasRain(), base.hasThunder());
        }
    }

    public AbstractMap getBaseMap() {
        return base;
    }

    public ServerLevel getWorld() {
        return world;
    }

    public Location getHiderSpawn() {
        return hiderSpawn;
    }

    public Location getSeekerSpawn() {
        return seekerSpawn;
    }

    public void addToTeam(String name, PositionType type) {
        if(teams.containsKey(type)) {
            scoreboard.addPlayerToTeam(name, teams.get(type));
        }
    }

    public void removeFromTeam(String name) {
        for(PlayerTeam t : teams.values()) {
            if(t.getPlayers().contains(name)) scoreboard.removePlayerFromTeam(name, t);
        }
    }

    public void clearTeams() {
        for(PlayerTeam t : teams.values()) {
            scoreboard.removePlayerTeam(t);
        }
    }
}
