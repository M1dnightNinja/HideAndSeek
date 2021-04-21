package me.m1dnightninja.hideandseek.fabric.game;

import me.m1dnightninja.hideandseek.api.game.AbstractMap;
import me.m1dnightninja.hideandseek.api.game.PositionData;
import me.m1dnightninja.hideandseek.api.game.PositionType;
import me.m1dnightninja.midnightcore.api.config.ConfigSection;
import me.m1dnightninja.midnightcore.api.math.Vec3d;
import me.m1dnightninja.midnightcore.fabric.MidnightCore;
import me.m1dnightninja.midnightcore.fabric.api.PermissionHelper;
import net.minecraft.server.level.ServerPlayer;

import java.io.File;
import java.util.List;
import java.util.UUID;

public class Map extends AbstractMap {

    public Map(String id, File worldFolder, Vec3d hiderSpawn, Vec3d seekerSpawn) {
        super(id, worldFolder, hiderSpawn, seekerSpawn);
    }

    @Override
    public boolean canEdit(UUID u) {
        ServerPlayer player = MidnightCore.getServer().getPlayerList().getPlayer(u);
        if(player == null) return false;

        return player.getUUID().equals(author) || editors.contains(player.getUUID()) || PermissionHelper.check(u, "hideandseek.edit." + getId());
    }

    @Override
    public void fromConfig(ConfigSection sec) {
        super.fromConfig(sec);

        if(sec.has("classes", List.class)) {
            for(Object o : sec.get("classes", List.class)) {
                if(!(o instanceof ConfigSection)) continue;

                GameClass clazz = GameClass.parse((ConfigSection) o);
                mapClasses.put(clazz.getId(), clazz);
            }
        }

        for(PositionType type : PositionType.values()) {
            if(sec.has(type.getId(), ConfigSection.class)) {
                positionData.put(type, PositionData.parse(sec.getSection(type.getId()), type, this));
            }
        }

    }

    public static Map parse(ConfigSection sec, String id, File f) {

        Vec3d hiderSpawn = Vec3d.parse(sec.getString("hider_spawn"));
        Vec3d seekerSpawn = Vec3d.parse(sec.getString("seeker_spawn"));

        Map out = new Map(id, f, hiderSpawn, seekerSpawn);
        out.fromConfig(sec);

        return out;
    }

}
