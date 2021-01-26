package me.m1dnightninja.hideandseek.api;

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
}
