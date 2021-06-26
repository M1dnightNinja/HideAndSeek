package me.m1dnightninja.hideandseek.api.game;

import me.m1dnightninja.midnightcore.api.config.ConfigSection;
import me.m1dnightninja.midnightcore.api.config.ConfigSerializer;
import me.m1dnightninja.midnightcore.api.math.Vec3d;
import me.m1dnightninja.midnightcore.api.text.MComponent;

import java.util.ArrayList;
import java.util.List;

public class Region {

    public static final ConfigSerializer<Region> SERIALIZER = new ConfigSerializer<Region>() {
        @Override
        public Region deserialize(ConfigSection sec) {

            if(!sec.has("id", String.class) || !sec.has("position", String.class) || !sec.has("size", String.class)) return null;

            String id = sec.getString("id");
            Vec3d pos = Vec3d.parse(sec.getString("position"));
            Vec3d size = Vec3d.parse(sec.getString("size"));

            Region out = new Region(id, pos, size);

            if(sec.has("name", String.class)) {
                out.display = MComponent.Serializer.parse(sec.getString("name"));
            }

            if(sec.has("denied", List.class)) {
                for(Object o : sec.get("denied", List.class)) {
                    if(!(o instanceof String)) continue;

                    out.denied.add(PositionType.getById((String) o));
                }
            }

            return out;
        }

        @Override
        public ConfigSection serialize(Region reg) {

            ConfigSection out = new ConfigSection();

            out.set("id", reg.id);
            out.set("position", reg.pos.toString());
            out.set("size", reg.size.toString());
            out.set("name", MComponent.Serializer.toJsonString(reg.display));

            List<String> denied = new ArrayList<>();
            for(PositionType t : reg.denied) {
                denied.add(t.getId());
            }

            out.set("denied", denied);

            return null;
        }
    };

    private final String id;
    private final Vec3d pos;
    private final Vec3d size;

    private final List<PositionType> denied = new ArrayList<>();

    private MComponent display;

    public Region(String id, Vec3d pos, Vec3d size) {
        this.id = id;
        this.pos = pos;
        this.size = size;
        this.display = MComponent.createTextComponent(id);
    }

    public String getId() {
        return id;
    }

    public List<PositionType> getDenied() {
        return denied;
    }

    public MComponent getDisplay() {
        return display;
    }

    public void setDisplay(MComponent display) {
        this.display = display;
    }

    public boolean isInRegion(Vec3d v) {
        return pos.getX() <= v.getX() && v.getX() <= pos.getX() + size.getX() &&
                pos.getY() <= v.getY() && v.getY() <= pos.getY() + size.getY() &&
                pos.getZ() <= v.getZ() && v.getZ() <= pos.getZ() + size.getZ();
    }
}
