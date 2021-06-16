package me.m1dnightninja.hideandseek.api.game;

import me.m1dnightninja.hideandseek.api.HideAndSeekAPI;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

public abstract class AbstractDimensionManager<T> {

    private final List<LoadedWorld> worlds = new ArrayList<>();

    public void loadMapWorld(Map map, String name, String folderName, DimensionCallback<T> callback) {

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

    public void unloadMapWorld(Map map, String name, boolean save) {

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
                if(backupDir.exists()) deleteDirectory(backupDir);
                copyDirectory(worldDir, backupDir);

                deleteDirectory(worldDir);
                copyDirectory(unload.file, worldDir);

            } catch(IOException ex) {
                HideAndSeekAPI.getLogger().warn("Unable to save world for map " + map.getId() + "!");
                ex.printStackTrace();
            }

        }



        worlds.remove(unload);

        if(!deleteQuietly(unload.file)) {
            HideAndSeekAPI.getLogger().warn("Unable to unload world for map " + map.getId() + "!");
        }
    }

    private LoadedWorld getWorld(Map map, String name) {
        for(LoadedWorld world : worlds) {
            if(world.map == map && world.key.equals(name)) {
                return world;
            }
        }
        return null;
    }

    protected abstract void doWorldLoad(Map map, String name, String folderName, InternalCallback<T> callback);
    protected abstract void doWorldUnload(T world, boolean save);

    private void deleteDirectory(File f) throws IOException {

        if(!f.exists() || !f.isDirectory()) throw new IOException();

        File[] files = f.listFiles();
        if(files != null) {

            for(File f1 : files) {
                if (f1.isDirectory()) {
                    deleteDirectory(f1);
                } else {
                    if (!f1.delete()) throw new IOException();
                }
            }
        }

        if(!f.delete()) throw new IOException();
    }

    private void copyFile(File source, File dest) throws IOException {

        File dir = dest.getParentFile();
        if(!dir.exists() && !dir.mkdirs()) {
            throw new IOException("Unable to create folder!");
        }

        if(!dest.exists() && !dest.createNewFile()) {
            throw new IOException("Unable to create file!");
        }

        try (InputStream is = new FileInputStream(source); OutputStream os = new FileOutputStream(dest)) {
            byte[] buffer = new byte[1024];
            int length;
            while ((length = is.read(buffer)) > 0) {
                os.write(buffer, 0, length);
            }
        }
    }

    private void copyDirectory(File source, File dest) throws IOException {

        if(!dest.exists() && !dest.mkdirs()) throw new IOException();
        File[] files = source.listFiles();

        if(files != null) {

            for(File f : files) {

                File nf = new File(dest, f.getName());
                if(f.isDirectory()) {
                    copyFile(f, nf);
                } else {
                    copyDirectory(f, nf);
                }
            }
        }
    }

    private boolean deleteQuietly(File f) {

        if(!f.exists() || !f.isDirectory()) return false;

        File[] files = f.listFiles();
        if(files != null) {

            for(File f1 : files) {
                if (f1.isDirectory()) {
                    if (!deleteQuietly(f1)) return false;
                } else {
                    if (!f1.delete()) {
                        return false;
                    }
                }
            }
        }

        return f.delete();
    }

    public interface DimensionCallback<T> {

        void onLoaded(T world);

    }

    protected interface InternalCallback<T> {

        void onLoaded(T world, File file);
    }

    private class LoadedWorld {
        private final Map map;
        private final String key;
        private final T world;
        private final File file;

        public LoadedWorld(Map map, String key, T world, File file) {
            this.map = map;
            this.key = key;
            this.world = world;
            this.file = file;
        }
    }

}
