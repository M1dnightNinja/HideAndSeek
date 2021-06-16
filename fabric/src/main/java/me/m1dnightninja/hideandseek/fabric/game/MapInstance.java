package me.m1dnightninja.hideandseek.fabric.game;

import me.m1dnightninja.hideandseek.api.*;
import me.m1dnightninja.hideandseek.api.game.Map;
import me.m1dnightninja.hideandseek.api.game.PositionData;
import me.m1dnightninja.hideandseek.api.core.AbstractSession;
import me.m1dnightninja.hideandseek.api.game.PositionType;
import me.m1dnightninja.midnightcore.api.player.MPlayer;
import me.m1dnightninja.midnightcore.api.text.AbstractCustomScoreboard;
import me.m1dnightninja.midnightcore.fabric.api.Location;
import me.m1dnightninja.midnightcore.fabric.player.FabricPlayer;
import net.minecraft.ChatFormatting;
import net.minecraft.network.protocol.game.ClientboundResourcePackPacket;
import net.minecraft.network.protocol.game.ClientboundSetPlayerTeamPacket;
import net.minecraft.server.ServerScoreboard;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.scores.PlayerTeam;
import net.minecraft.world.scores.Team;

import java.util.HashMap;

public class MapInstance {

    private static final String emptyResPack = "https://github.com/M1dnightNinja/HideAndSeek/blob/master/empty.zip?raw=true";
    private static final String emptyHash = "F8CC3481867628951AD312B9FB886223856F7AB0";

    private final Map base;
    private final ServerLevel world;

    private final Location hiderSpawn;
    private final Location seekerSpawn;

    private ServerScoreboard scoreboard;
    private final HashMap<PositionType, PlayerTeam> teams = new HashMap<>();

    private final AbstractSession session;

    public MapInstance(AbstractSession session, Map base, ServerLevel world, boolean createTeams) {
        this.base = base;
        this.world = world;
        this.session = session;

        hiderSpawn = new Location(world.dimension().location(), base.getHiderSpawn().getX(), base.getHiderSpawn().getY(), base.getHiderSpawn().getZ(), base.getHiderRotation(), 0);
        seekerSpawn = new Location(world.dimension().location(), base.getSeekerSpawn().getX(), base.getSeekerSpawn().getY(), base.getSeekerSpawn().getZ(), base.getSeekerRotation(), 0);

        if(createTeams) {
            scoreboard = world.getScoreboard();

            for (PositionType t : PositionType.values()) {
                PositionData data = base.getData(t);
                if (data == null) continue;

                PlayerTeam team = new PlayerTeam(scoreboard, AbstractCustomScoreboard.generateRandomId());
                team.setColor(ChatFormatting.getById(data.getColor().toRGBI()));

                if (!t.isSeeker()) {
                    team.setNameTagVisibility(Team.Visibility.NEVER);
                }

                teams.put(t, team);

                for(MPlayer u : session.getPlayers()) {

                    ServerPlayer pl = ((FabricPlayer) u).getMinecraftPlayer();
                    if(pl == null) continue;

                    pl.connection.send(ClientboundSetPlayerTeamPacket.createAddOrModifyPacket(team, true));
                }
            }
        }

        if(base.hasRandomTime()) {
            world.setDayTime(HideAndSeekAPI.getInstance().getRandom().nextInt(24000));
        }

        if(base.hasRain() || base.hasThunder()) {
            world.setWeatherParameters(0, Integer.MAX_VALUE, base.hasRain(), base.hasThunder());
        }
    }

    public ServerScoreboard getScoreboard() {
        return scoreboard;
    }

    public Map getBaseMap() {
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
            PlayerTeam t = teams.get(type);
            t.getPlayers().add(name);
            for(MPlayer u : session.getPlayers()) {

                ServerPlayer pl = ((FabricPlayer) u).getMinecraftPlayer();
                if(pl == null) continue;

                pl.connection.send(ClientboundSetPlayerTeamPacket.createPlayerPacket(t, name, ClientboundSetPlayerTeamPacket.Action.ADD));
            }
        }
    }

    public void removeFromTeam(String name) {
        for(PlayerTeam t : teams.values()) {
            if(t.getPlayers().contains(name)) {
                t.getPlayers().remove(name);
                for(MPlayer u : session.getPlayers()) {

                    ServerPlayer pl = ((FabricPlayer) u).getMinecraftPlayer();
                    if(pl == null) continue;

                    pl.connection.send(ClientboundSetPlayerTeamPacket.createPlayerPacket(t, name, ClientboundSetPlayerTeamPacket.Action.REMOVE));
                }
            }
        }
    }

    public void onJoin(ServerPlayer pl) {
        if(getBaseMap().getResourcePack() != null) {
            HideAndSeekAPI.getLogger().warn("Sending pack to " + pl.getName());
            pl.connection.send(new ClientboundResourcePackPacket(getBaseMap().getResourcePack(), getBaseMap().getResourcePackHash(), true, null));
        }
    }

    public void onLeave(ServerPlayer pl) {
        for(PlayerTeam t : teams.values()) {
            pl.connection.send(ClientboundSetPlayerTeamPacket.createRemovePacket(t));
        }
        if(getBaseMap().getResourcePack() != null) {
            pl.connection.send(new ClientboundResourcePackPacket(emptyResPack, emptyHash, true, null));
        }
    }

    public void clearTeams() {
        for(PlayerTeam t : teams.values()) {
            scoreboard.removePlayerTeam(t);
        }
    }
}
