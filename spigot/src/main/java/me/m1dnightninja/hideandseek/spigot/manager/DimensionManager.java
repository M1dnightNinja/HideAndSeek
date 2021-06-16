package me.m1dnightninja.hideandseek.spigot.manager;

import me.m1dnightninja.hideandseek.api.HideAndSeekAPI;
import me.m1dnightninja.hideandseek.api.game.AbstractDimensionManager;
import me.m1dnightninja.hideandseek.api.game.Map;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.WorldCreator;
import org.bukkit.craftbukkit.libs.org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;

public class DimensionManager extends AbstractDimensionManager<World> {

    @Override
    protected void doWorldLoad(Map map, String name, String folderName, InternalCallback<World> callback) {

        File newDir = new File(folderName);

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

        WorldCreator creator = new WorldCreator(folderName);
        World w = creator.createWorld();

        callback.onLoaded(w, newDir);
    }

    @Override
    protected void doWorldUnload(World world, boolean save) {

        Bukkit.unloadWorld(world.getWorldFolder().getName(), save);
    }
}
