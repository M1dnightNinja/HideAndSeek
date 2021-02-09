package me.m1dnightninja.hideandseek.api;

import me.m1dnightninja.midnightcore.api.config.ConfigSection;
import me.m1dnightninja.midnightcore.api.math.Vec3d;

import java.io.File;
import java.util.*;

public abstract class AbstractMap {

    protected final String id;
    protected final File mapFolder;
    protected final File worldFolder;
    protected final Vec3d hiderSpawn;
    protected final Vec3d seekerSpawn;

    protected String name;

    protected boolean randomTime;
    protected boolean rain;
    protected boolean thunder;

    protected int hideTime = 30;
    protected int seekTime = 240;

    protected final List<Vec3d> fireworkSpawners = new ArrayList<>();
    protected final List<Region> regions = new ArrayList<>();

    protected final HashMap<String, AbstractClass> mapClasses = new HashMap<>();
    protected final HashMap<PositionType, AbstractPositionData> positionData = new HashMap<>();

    protected final List<DamageSource> resetSources = new ArrayList<>();
    protected final List<DamageSource> tagSources = new ArrayList<>();

    protected float hiderRotation = 0;
    protected float seekerRotation = 0;

    protected String dimensionType;

    public AbstractMap(String id, File mapFolder, Vec3d hiderSpawn, Vec3d seekerSpawn) {
        this.id = id;
        this.mapFolder = mapFolder;
        this.worldFolder = new File(mapFolder, "world");
        this.hiderSpawn = hiderSpawn;
        this.seekerSpawn = seekerSpawn;

        this.name = id;
    }

    public String getId() {
        return id;
    }

    public AbstractPositionData getData(PositionType type) {
        return positionData.get(type);
    }

    public String getName() {
        return name;
    }

    public File getMapFolder() {
        return mapFolder;
    }

    public File getWorldFolder() {
        return worldFolder;
    }

    public boolean hasRandomTime() {
        return randomTime;
    }

    public boolean hasRain() {
        return rain;
    }

    public boolean hasThunder() {
        return thunder;
    }

    public int getHideTime() {
        return hideTime;
    }

    public int getSeekTime() {
        return seekTime;
    }

    public List<Vec3d> getFireworkSpawners() {
        return fireworkSpawners;
    }

    public List<Region> getRegions() {
        return regions;
    }

    public AbstractClass getClass(String id) {
        return mapClasses.get(id);
    }

    public AbstractClass chooseRandomClass(PositionType type) {

        if(positionData.get(type).getClasses().size() == 0) return null;
        return positionData.get(type).getClasses().get(HideAndSeekAPI.getInstance().getRandom().nextInt(positionData.get(type).getClasses().size()));
    }

    public Collection<AbstractClass> getMapClasses() {
        return mapClasses.values();
    }

    public Vec3d getHiderSpawn() {
        return hiderSpawn;
    }

    public float getHiderRotation() {
        return hiderRotation;
    }

    public Vec3d getSeekerSpawn() {
        return seekerSpawn;
    }

    public float getSeekerRotation() {
        return seekerRotation;
    }

    public String getDimensionType() {
        return dimensionType;
    }

    public List<DamageSource> getResetSources() {
        return resetSources;
    }

    public List<DamageSource> getTagSources() {
        return tagSources;
    }

    public abstract boolean canEdit(UUID u);

    public void printDebug() {

        HideAndSeekAPI.getLogger().warn("id: " +  id);
        HideAndSeekAPI.getLogger().warn("name: " + name);
        HideAndSeekAPI.getLogger().warn("random_time: " + randomTime);
        HideAndSeekAPI.getLogger().warn("rain: " + rain);
        HideAndSeekAPI.getLogger().warn("thunder: " + thunder);
        HideAndSeekAPI.getLogger().warn("hide_time: " + hideTime);
        HideAndSeekAPI.getLogger().warn("seek_time: " + seekTime);
        HideAndSeekAPI.getLogger().warn("firework_spawners: ");
        for(Vec3d v : fireworkSpawners) {
            HideAndSeekAPI.getLogger().warn("  - " + v.toString());
        }
        HideAndSeekAPI.getLogger().warn("regions: " + regions.size());
        HideAndSeekAPI.getLogger().warn("hider_spawn_rotation: " + hiderRotation);
        HideAndSeekAPI.getLogger().warn("seeker_spawn_rotation: " + seekerRotation);
        HideAndSeekAPI.getLogger().warn("dimension: " + dimensionType);
        HideAndSeekAPI.getLogger().warn("reset_sources: ");
        for(DamageSource src : resetSources) {
            HideAndSeekAPI.getLogger().warn("  - " + src.name());
        }
        HideAndSeekAPI.getLogger().warn("tag_sources: ");
        for(DamageSource src : tagSources) {
            HideAndSeekAPI.getLogger().warn("  - " + src.name());
        }
        HideAndSeekAPI.getLogger().warn("classes: ");
        for(String cid : mapClasses.keySet()) {
            HideAndSeekAPI.getLogger().warn("  - " + cid);
        }

    }


    public void fromConfig(ConfigSection sec) {

        if(sec.has("name", String.class)) {
            name = sec.getString("name");
        }

        if(sec.has("random_time", Boolean.TYPE)) {
            randomTime = sec.getBoolean("random_time");
        }

        if(sec.has("rain", Boolean.TYPE)) {
            rain = sec.getBoolean("rain");
        }

        if(sec.has("thunder", Boolean.TYPE)) {
            thunder = sec.getBoolean("thunder");
        }

        if(sec.has("hide_time", Number.class)) {
            hideTime = sec.getInt("hide_time");
        }

        if(sec.has("seek_time", Number.class)) {
            seekTime = sec.getInt("seek_time");
        }

        if(sec.has("firework_spawners", List.class)) {
            fireworkSpawners.clear();
            for(Object o : sec.get("firework_spawners", List.class)) {
                if(!(o instanceof String)) continue;
                fireworkSpawners.add(Vec3d.parse((String) o));
            }
        }

        if(sec.has("regions", List.class)) {
            regions.clear();
            for(Object o : sec.get("regions", List.class)) {
                if(!(o instanceof ConfigSection)) continue;

                Region reg = Region.parse((ConfigSection) o);
                if(reg == null) {
                    HideAndSeekAPI.getLogger().warn("Region was parsed as null!");
                } else {
                    regions.add(reg);
                }
            }
        }

        if(sec.has("hider_spawn_rotation", Number.class)) {
            hiderRotation = sec.getFloat("hider_spawn_rotation");
        }

        if(sec.has("seeker_spawn_rotation", Number.class)) {
            seekerRotation = sec.getFloat("seeker_spawn_rotation");
        }

        if(sec.has("dimension", String.class)) {
            dimensionType = sec.getString("dimension");
        }

        if(sec.has("reset_sources", List.class)) {
            resetSources.clear();

            for(Object o : sec.get("reset_sources", List.class)) {
                if(!(o instanceof String)) continue;

                resetSources.add(DamageSource.valueOf((String) o));
            }
        }

        if(sec.has("tag_sources", List.class)) {
            tagSources.clear();

            for(Object o : sec.get("tag_sources", List.class)) {
                if(!(o instanceof String)) continue;

                tagSources.add(DamageSource.valueOf((String) o));
            }
        }

    }

}
