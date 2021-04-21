package me.m1dnightninja.hideandseek.api.game;

import me.m1dnightninja.hideandseek.api.HideAndSeekAPI;
import me.m1dnightninja.midnightcore.api.math.Color;
import me.m1dnightninja.midnightcore.api.config.ConfigSection;
import me.m1dnightninja.midnightcore.api.text.MComponent;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class PositionData {

    protected final PositionType type;
    protected final List<AbstractClass> classes;

    protected MComponent name;
    protected MComponent pluralName;
    protected MComponent properName;

    protected Color color;

    public PositionData(PositionType type) {
        this.type = type;
        this.classes = new ArrayList<>();

        this.name = type.getName();
        this.color = type.getDefaultColor();
    }

    public PositionType getType() {
        return type;
    }

    public List<AbstractClass> getClasses() {
        return classes;
    }

    public MComponent getName() {
        return name;
    }

    public MComponent getPluralName() {
        return pluralName == null ? name : pluralName;
    }

    public MComponent getProperName() {
        return properName == null ? name : properName;
    }

    public Color getColor() {
        return color;
    }

    public void fromConfig(ConfigSection sec, AbstractMap map) {

        if(sec.has("name", String.class)) {
            name = MComponent.Serializer.parse(sec.getString("name"));
        }

        if(sec.has("name_plural", String.class)) {
            pluralName = MComponent.Serializer.parse(sec.getString("name_plural"));
        }

        if(sec.has("name_proper", String.class)) {
            properName = MComponent.Serializer.parse(sec.getString("name_proper"));
        }

        if(sec.has("color", String.class)) {
            color = Color.parse(sec.getString("color"));
        } else if(sec.has("color", Number.class)) {
            color = new Color(sec.getInt("color"));
        }

        if(sec.has("classes", List.class)) {

            HashMap<String, AbstractClass> tclasses = new HashMap<>();

            for(AbstractClass clazz : HideAndSeekAPI.getInstance().getRegistry().getClasses()) {
                tclasses.put(clazz.getId(), clazz);
            }
            if(map != null) {
                for (AbstractClass clazz : map.getMapClasses()) {
                    tclasses.put(clazz.getId(), clazz);
                }
            }

            for(Object o : sec.get("classes", List.class)) {

                if(!(o instanceof String)) continue;

                classes.add(tclasses.get(o));
            }
        }
    }

    public static PositionData parse(ConfigSection sec, PositionType type, AbstractMap map) {

        PositionData dt = new PositionData(type);
        dt.fromConfig(sec, map);

        return dt;
    }

}
