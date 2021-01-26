package me.m1dnightninja.hideandseek.fabric;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import me.m1dnightninja.hideandseek.fabric.util.ParseUtil;
import me.m1dnightninja.hideandseek.api.AbstractClass;
import me.m1dnightninja.hideandseek.api.AbstractPositionData;
import me.m1dnightninja.hideandseek.api.HideAndSeekAPI;
import me.m1dnightninja.hideandseek.api.PositionType;
import me.m1dnightninja.midnightcore.fabric.util.TextUtil;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;


public class PositionData extends AbstractPositionData {

    public PositionData(PositionType type) {
        super(type);
    }

    public static PositionData parse(JsonObject obj, PositionType type, Map map) {

        PositionData out = new PositionData(type);

        if(obj.has("name")) {
            out.name = obj.get("name").getAsString();
        }

        MutableComponent name = TextUtil.parse(out.name);

        if(obj.has("name_plural")) {
            out.pluralName = obj.get("name_plural").getAsString();
        } else {
            out.pluralName = Component.Serializer.toJson(new TextComponent(name.getString() + "s").setStyle(name.getStyle()));
        }

        if(obj.has("name_proper")) {
            out.properName = obj.get("name_proper").getAsString();
        }  else {
            out.properName = Component.Serializer.toJson(new TextComponent("The " + name.getString()).setStyle(name.getStyle()));
        }

        if(obj.has("color")) {
            out.color = ParseUtil.parseColor(obj.get("color").getAsString());
        }

        if(obj.has("classes") && obj.get("classes").isJsonArray()) {

            HashMap<String, AbstractClass> classes = new HashMap<>();

            for(AbstractClass clazz : HideAndSeekAPI.getInstance().getRegistry().getClasses()) {
                classes.put(clazz.getId(), clazz);
            }
            if(map != null) {
                for (AbstractClass clazz : map.getMapClasses()) {
                    classes.put(clazz.getId(), clazz);
                }
            }

            for(JsonElement ele : obj.get("classes").getAsJsonArray()) {

                out.classes.add(classes.get(ele.getAsString()));

            }
        }

        return out;
    }

}
