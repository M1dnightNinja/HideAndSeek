package me.m1dnightninja.hideandseek.common;

import me.m1dnightninja.hideandseek.api.game.AbstractMap;
import me.m1dnightninja.hideandseek.api.HideAndSeekAPI;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public abstract class AbstractDimensionManager<T> {

    private final List<LoadedWorld> worlds = new ArrayList<>();

    public void loadMapWorld(AbstractMap map, String name, String folderName, DimensionCallback<T> callback) {

        LoadedWorld w = getWorld(map, name);
        if(w != null) {
            callback.onLoaded(w.world);
            return;
        }

        doWorldLoad(map, name, folderName, (world, file) -> {
            if(world != null) worlds.add(new LoadedWorld(map, name, world, file));
            callback.onLoaded(world);
        });
    }

    public void unloadMapWorld(AbstractMap map, String name, boolean save) {

        LoadedWorld unload = getWorld(map, name);
        if(unload == null) {
            HideAndSeekAPI.getLogger().warn("Unable to find world " + name + " for map " + map.getId() + "!");
            return;
        }

        doWorldUnload(unload.world, save);

        if(save) {

            File worldDir = map.getWorldFolder();
            File backupDir = new File(map.getMapFolder(), "world_bak");

            try {
                if(backupDir.exists()) FileUtils.deleteDirectory(backupDir);
                FileUtils.copyDirectory(worldDir, backupDir);

                FileUtils.deleteDirectory(worldDir);
                FileUtils.copyDirectory(unload.file, worldDir);
            } catch(IOException ex) {
                HideAndSeekAPI.getLogger().warn("Unable to save world for map " + map.getId() + "!");
                ex.printStackTrace();
            }

        }



        worlds.remove(unload);

        if(!FileUtils.deleteQuietly(unload.file)) {
            HideAndSeekAPI.getLogger().warn("Unable to unload world for map " + map.getId() + "!");
        }
    }

    private LoadedWorld getWorld(AbstractMap map, String name) {
        for(LoadedWorld world : worlds) {
            if(world.map == map && world.key.equals(name)) {
                return world;
            }
        }
        return null;
    }

    protected abstract void doWorldLoad(AbstractMap map, String name, String folderName, InternalCallback<T> callback);
    protected abstract void doWorldUnload(T world, boolean save);

    public interface DimensionCallback<T> {

        void onLoaded(T world);

    }

    protected interface InternalCallback<T> {

        void onLoaded(T world, File file);
    }

    private class LoadedWorld {
        private final AbstractMap map;
        private final String key;
        private final T world;
        private final File file;

        public LoadedWorld(AbstractMap map, String key, T world, File file) {
            this.map = map;
            this.key = key;
            this.world = world;
            this.file = file;
        }
    }

}
