package me.m1dnightninja.hideandseek.api;

import me.m1dnightninja.midnightcore.api.Color;
import me.m1dnightninja.midnightcore.api.config.ConfigSection;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public abstract class AbstractPositionData {

    protected final PositionType type;
    protected final List<AbstractClass> classes;

    protected String name;
    protected String pluralName;
    protected String properName;

    protected Color color;

    public AbstractPositionData(PositionType type) {
        this.type = type;
        this.classes = new ArrayList<>();

        this.name = type.name;
        this.pluralName = name + "s";
        this.properName = "The " + name;
        this.color = type.defaultColor;
    }

    public PositionType getType() {
        return type;
    }

    public List<AbstractClass> getClasses() {
        return classes;
    }

    public String getName() {
        return name;
    }

    public String getPluralName() {
        return pluralName;
    }

    public String getProperName() {
        return properName;
    }

    public Color getColor() {
        return color;
    }

    public void fromConfig(ConfigSection sec, AbstractMap map) {

        if(sec.has("name", String.class)) {
            name = sec.getString("name");
            pluralName = name + "s";
            properName = "The " + name;
        }

        if(sec.has("name_plural", String.class)) {
            pluralName = sec.getString("name_plural");
        }

        if(sec.has("name_proper", String.class)) {
            properName = sec.getString("name_proper");
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
}
