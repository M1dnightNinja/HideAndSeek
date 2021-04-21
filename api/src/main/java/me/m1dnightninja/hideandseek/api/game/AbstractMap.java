package me.m1dnightninja.hideandseek.api.game;

import me.m1dnightninja.hideandseek.api.HideAndSeekAPI;
import me.m1dnightninja.midnightcore.api.config.ConfigSection;
import me.m1dnightninja.midnightcore.api.inventory.MItemStack;
import me.m1dnightninja.midnightcore.api.math.Vec3d;
import me.m1dnightninja.midnightcore.api.registry.MIdentifier;
import me.m1dnightninja.midnightcore.api.text.MComponent;
import me.m1dnightninja.midnightcore.api.text.MStyle;

import java.io.File;
import java.util.*;

public abstract class AbstractMap {

    protected final String id;
    protected final File mapFolder;
    protected final File worldFolder;
    protected final Vec3d hiderSpawn;
    protected final Vec3d seekerSpawn;

    protected MComponent name;
    protected List<MComponent> description = new ArrayList<>();

    protected MItemStack displayItem;

    protected UUID author;
    protected List<UUID> editors = new ArrayList<>();

    protected boolean randomTime;
    protected boolean rain;
    protected boolean thunder;

    protected int hideTime = 30;
    protected int seekTime = 240;

    protected final List<Vec3d> fireworkSpawners = new ArrayList<>();
    protected final List<Region> regions = new ArrayList<>();

    protected final HashMap<String, AbstractClass> mapClasses = new HashMap<>();
    protected final HashMap<PositionType, PositionData> positionData = new HashMap<>();

    protected final List<DamageSource> resetSources = new ArrayList<>();
    protected final List<DamageSource> tagSources = new ArrayList<>();

    protected float hiderRotation = 0;
    protected float seekerRotation = 0;

    protected MIdentifier dimensionType;
    protected String resPack;
    protected String resPackHash;

    public AbstractMap(String id, File mapFolder, Vec3d hiderSpawn, Vec3d seekerSpawn) {
        this.id = id;
        this.mapFolder = mapFolder;
        this.worldFolder = new File(mapFolder, "world");
        this.hiderSpawn = hiderSpawn;
        this.seekerSpawn = seekerSpawn;

        this.name = MComponent.createTextComponent(id);
    }

    public String getId() {
        return id;
    }

    public PositionData getData(PositionType type) {
        return positionData.get(type);
    }

    public MComponent getName() {
        return name;
    }

    public List<MComponent> getDescription() {
        return description;
    }

    public UUID getAuthor() {
        return author;
    }

    public List<UUID> getEditors() {
        return editors;
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

    public AbstractClass getClassOrGlobal(String id) {
        AbstractClass clazz = getClass(id);

        if(clazz == null) {
            clazz = HideAndSeekAPI.getInstance().getRegistry().getClass(id);
        }

        return clazz;
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

    public MIdentifier getDimensionType() {
        return dimensionType;
    }

    public List<DamageSource> getResetSources() {
        return resetSources;
    }

    public List<DamageSource> getTagSources() {
        return tagSources;
    }

    public String getResourcePack() { return resPack; }

    public String getResourcePackHash() { return resPackHash == null ? "" : resPackHash; }


    public abstract boolean canEdit(UUID u);


    public void fromConfig(ConfigSection sec) {

        if(sec.has("name", String.class)) {
            name = MComponent.Serializer.parse(sec.getString("name"));
        }

        if(sec.has("description", List.class)) {

            for(String s : sec.getStringList("description")) {
                description.add(MComponent.Serializer.parse(s));
            }
        }

        if(sec.has("random_time", Boolean.TYPE)) {
            randomTime = sec.getBoolean("random_time");
        }

        if(sec.has("author", String.class)) {
            author = UUID.fromString(sec.getString("author"));
        }

        if(sec.has("editors", List.class)) {
            for(Object o : sec.getList("editors")) {
                if(!(o instanceof String)) continue;
                editors.add(UUID.fromString((String) o));
            }
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
            regions.addAll(sec.getList("regions", Region.class));
        }

        if(sec.has("hider_spawn_rotation", Number.class)) {
            hiderRotation = sec.getFloat("hider_spawn_rotation");
        }

        if(sec.has("seeker_spawn_rotation", Number.class)) {
            seekerRotation = sec.getFloat("seeker_spawn_rotation");
        }

        if(sec.has("dimension", String.class)) {
            dimensionType = MIdentifier.parse(sec.getString("dimension"));
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

        if(sec.has("resource_pack", String.class)) {
            resPack = sec.getString("resource_pack");
        }

        if(sec.has("resource_pack_sha1", String.class)) {
            resPackHash = sec.getString("resource_pack_sha1");
        }

        if(sec.has("item", ConfigSection.class)) {
            ConfigSection isec = sec.getSection("item");

            MIdentifier type = MIdentifier.parse(isec.getString("type"));
            int count = isec.getInt("count");

            displayItem = MItemStack.Builder.of(type).withAmount(count).withName(name).withLore(description).build();

        } else {

            displayItem = createDefaultItem(this);
        }

    }

    private static MItemStack createDefaultItem(AbstractMap map) {

        List<MComponent> cmp = map.getDescription();

        return MItemStack.Builder.of(MIdentifier.create("minecraft", "white_wool")).withName(MComponent.createTextComponent("").withStyle(new MStyle().withItalic(false)).addChild(map.getName())).withLore(cmp).build();
    }

    public static MItemStack getDisplayStack(AbstractMap map) {

        return map.displayItem.copy();
    }

}
