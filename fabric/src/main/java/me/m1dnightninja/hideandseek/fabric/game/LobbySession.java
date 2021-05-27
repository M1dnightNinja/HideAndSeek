package me.m1dnightninja.hideandseek.fabric.game;

import me.m1dnightninja.hideandseek.api.game.Lobby;
import me.m1dnightninja.hideandseek.api.game.AbstractLobbySession;
import me.m1dnightninja.hideandseek.api.game.DamageSource;
import me.m1dnightninja.midnightcore.api.MidnightCoreAPI;
import me.m1dnightninja.midnightcore.api.module.ISavePointModule;
import me.m1dnightninja.midnightcore.api.player.MPlayer;
import me.m1dnightninja.midnightcore.api.text.MComponent;
import me.m1dnightninja.midnightcore.fabric.MidnightCore;
import me.m1dnightninja.midnightcore.fabric.api.Location;
import me.m1dnightninja.midnightcore.fabric.player.FabricPlayer;
import me.m1dnightninja.midnightcore.fabric.util.ConversionUtil;
import net.minecraft.Util;
import net.minecraft.core.Registry;
import net.minecraft.network.chat.ChatType;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

public class LobbySession extends AbstractLobbySession {

    private final Location tpLocation;

    public LobbySession(Lobby lobby) {
        super(lobby);

        if(lobby.getWorld() == null) {
            shutdown();
        }

        ResourceLocation world = new ResourceLocation(lobby.getWorld());

        ServerLevel dim = MidnightCore.getServer().getLevel(ResourceKey.create(Registry.DIMENSION_REGISTRY, world));
        if(dim == null) {
            shutdown();
        }

        tpLocation = new Location(world, lobby.getLocation().getX(), lobby.getLocation().getY(), lobby.getLocation().getZ(), lobby.getRotation(), 0);
    }

    @Override
    protected boolean shouldAddPlayer(MPlayer u) {
        return true;
    }

    @Override
    protected void onPlayerAdded(MPlayer u) {
        super.onPlayerAdded(u);

        ServerPlayer ent = ((FabricPlayer) u).getMinecraftPlayer();

        if(ent == null) {
            removePlayer(u);
            return;
        }

        tpLocation.teleport(ent);
        MidnightCoreAPI.getInstance().getModule(ISavePointModule.class).resetPlayer(u);
        ent.setGameMode(net.minecraft.world.level.GameType.ADVENTURE);
    }

    @Override
    public void onDamaged(MPlayer u, MPlayer damager, DamageSource damageSource, float amount) {

        if(isRunning()) {

            runningInstance.onDamaged(u, damager, damageSource, amount);

        } else {

            ServerPlayer player = ((FabricPlayer) u).getMinecraftPlayer();
            if (player == null) return;

            if (damageSource == DamageSource.VOID) {
                tpLocation.teleport(player);
            }
        }
    }

    @Override
    public void broadcastMessage(MComponent message) {

        Component comp = ConversionUtil.toMinecraftComponent(message);

        for (MPlayer u : getPlayers()) {
            ServerPlayer pl = ((FabricPlayer) u).getMinecraftPlayer();
            if (pl == null) continue;
            pl.sendMessage(comp, ChatType.SYSTEM, Util.NIL_UUID);
        }
    }


}
