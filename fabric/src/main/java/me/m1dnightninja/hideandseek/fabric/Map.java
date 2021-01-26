package me.m1dnightninja.hideandseek.fabric;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import me.m1dnightninja.hideandseek.api.DamageSource;
import me.m1dnightninja.hideandseek.fabric.util.ParseUtil;
import me.m1dnightninja.hideandseek.api.AbstractMap;
import me.m1dnightninja.hideandseek.api.PositionType;
import me.m1dnightninja.midnightcore.api.math.Vec3d;
import me.m1dnightninja.midnightcore.fabric.MidnightCore;
import net.minecraft.server.level.ServerPlayer;

import java.io.File;
import java.util.UUID;

public class Map extends AbstractMap {

    public Map(String id, File worldFolder, Vec3d hiderSpawn, Vec3d seekerSpawn) {
        super(id, worldFolder, hiderSpawn, seekerSpawn);
    }

    @Override
    public boolean canEdit(UUID u) {
        ServerPlayer player = MidnightCore.getServer().getPlayerList().getPlayer(u);
        return player != null;

        // TODO: Check Permissions
    }

    public static Map parse(String id, JsonObject obj, File mapFolder) {

        Vec3d hiderSpawn = ParseUtil.parseLocation(obj.get("hider_spawn").getAsString());
        Vec3d seekerSpawn = ParseUtil.parseLocation(obj.get("seeker_spawn").getAsString());

        Map out = new Map(id, mapFolder, hiderSpawn, seekerSpawn);

        if(obj.has("name")) {
            out.name = obj.get("name").getAsString();
        }

        if(obj.has("random_time")) {
            out.randomTime = obj.get("random_time").getAsBoolean();
        }

        if(obj.has("rain")) {
            out.rain = obj.get("rain").getAsBoolean();
        }

        if(obj.has("thunder")) {
            out.thunder = obj.get("thunder").getAsBoolean();
        }

        if(obj.has("hide_time")) {
            int time = obj.get("hide_time").getAsInt();
            if(time > 0) out.hideTime = time;
        }

        if(obj.has("seek_time")) {
            int time = obj.get("seek_time").getAsInt();
            if(time > 0) out.seekTime = time;
        }

        if(obj.has("firework_spawners") && obj.get("firework_spawners").isJsonArray()) {
            for(JsonElement ele : obj.get("firework_spawners").getAsJsonArray()) {
                out.fireworkSpawners.add(ParseUtil.parseLocation(ele.getAsString()));
            }
        }

        if(obj.has("regions") && obj.get("regions").isJsonArray()) {
            for(JsonElement ele : obj.get("regions").getAsJsonArray()) {
                out.regions.add(ParseUtil.parseRegion(ele.getAsJsonObject()));
            }
        }

        if(obj.has("classes") && obj.get("classes").isJsonArray()) {
            for(JsonElement ele : obj.get("classes").getAsJsonArray()) {

                GameClass clazz = GameClass.parse(ele.getAsJsonObject());
                out.mapClasses.put(clazz.getId(), clazz);
            }
        }

        for(PositionType type : PositionType.values()) {
            if(obj.has(type.getId()) && obj.get(type.getId()).isJsonObject()) {
                PositionData data = PositionData.parse(obj.get(type.getId()).getAsJsonObject(), type, out);
                out.positionData.put(type, data);
            }
        }

        if(obj.has("hider_spawn_rotation")) {
            out.hiderRotation = obj.get("hider_spawn_rotation").getAsFloat();
        }

        if(obj.has("seeker_spawn_rotation")) {
            out.seekerRotation = obj.get("seeker_spawn_rotation").getAsFloat();
        }

        if(obj.has("dimension")) {
            out.dimensionType = obj.get("dimension").getAsString();
        }

        if(obj.has("reset_sources") && obj.get("reset_sources").isJsonArray()) {
            for(JsonElement ele : obj.getAsJsonArray("reset_sources")) {
                out.resetSources.add(DamageSource.valueOf(ele.getAsString()));
            }
        }

        if(obj.has("tag_sources") && obj.get("tag_sources").isJsonArray()) {
            for(JsonElement ele : obj.getAsJsonArray("tag_sources")) {
                out.tagSources.add(DamageSource.valueOf(ele.getAsString()));
            }
        }

        return out;

    }

}
