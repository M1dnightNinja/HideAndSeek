package me.m1dnightninja.hideandseek.fabric.manager;

import me.m1dnightninja.hideandseek.api.game.AbstractMap;
import me.m1dnightninja.hideandseek.api.HideAndSeekAPI;
import me.m1dnightninja.hideandseek.common.AbstractDimensionManager;
import me.m1dnightninja.midnightcore.api.MidnightCoreAPI;
import me.m1dnightninja.midnightcore.fabric.api.WorldCreator;
import me.m1dnightninja.midnightcore.fabric.dimension.EmptyGenerator;
import me.m1dnightninja.midnightcore.fabric.module.DimensionModule;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.dimension.LevelStem;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;

public class DimensionManager extends AbstractDimensionManager<ServerLevel> {

    @Override
    protected void doWorldLoad(AbstractMap map, String name, String folderName, InternalCallback<ServerLevel> callback) {

        File newDir = new File(map.getMapFolder(), folderName);

        if(!newDir.equals(map.getWorldFolder())) {
            try {
                if (newDir.exists()) FileUtils.deleteDirectory(newDir);
                FileUtils.copyDirectory(map.getWorldFolder(), newDir);
            } catch (IOException ex) {
                HideAndSeekAPI.getLogger().warn("Unable to load world for map " + map.getId());
                ex.printStackTrace();
                callback.onLoaded(null, newDir);
                return;
            }
        }

        ResourceKey<LevelStem> opts;
        if(map.getDimensionType() != null) {
            ResourceLocation id = new ResourceLocation(map.getDimensionType());
            opts = ResourceKey.create(Registry.LEVEL_STEM_REGISTRY, id);
        } else {
            opts = LevelStem.OVERWORLD;
        }

        WorldCreator creator = new WorldCreator(new ResourceLocation("hideandseek", name), opts, new EmptyGenerator(new ResourceLocation("forest")));
        creator.setFolderName(folderName);

        MidnightCoreAPI.getInstance().getModule(DimensionModule.class).createWorld(creator, map.getMapFolder().toPath(), serverWorld -> callback.onLoaded(serverWorld, newDir));
    }

    @Override
    protected void doWorldUnload(ServerLevel world, boolean save) {
        MidnightCoreAPI.getInstance().getModule(DimensionModule.class).unloadDimension(world.dimension().location(), save);
    }
}
