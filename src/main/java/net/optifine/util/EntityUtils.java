package net.optifine.util;

import java.util.HashMap;
import java.util.Map;

import dev.vexor.photon.Photon;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;

public class EntityUtils {
    private static final Map<Class, Integer> mapIdByClass = new HashMap<>();
    private static final Map<String, Integer> mapIdByName = new HashMap<>();
    private static final Map<String, Class> mapClassByName = new HashMap<>();

    public static int getEntityIdByClass(Entity entity) {
        return entity == null ? -1 : getEntityIdByClass(entity.getClass());
    }

    public static int getEntityIdByClass(Class cls) {
        Integer id = mapIdByClass.get(cls);
        return id == null ? -1 : id;
    }

    public static int getEntityIdByName(String name) {
        Integer id = mapIdByName.get(name);
        return id == null ? -1 : id;
    }

    public static Class getEntityClassByName(String name) {
        return mapClassByName.get(name);
    }

    static {
        for (int i = 0; i < 1000; i++) {
            Class cls = EntityType.getEntityById(i);
            if (cls != null) {
                String name = EntityType.getEntityName(i);
                if (name != null) {
                    if (mapIdByClass.containsKey(cls)) {
                        Photon.LOGGER.warn("Duplicate entity class: " + cls + ", id1: " + mapIdByClass.get(cls) + ", id2: " + i);
                    }

                    if (mapIdByName.containsKey(name)) {
                        Photon.LOGGER.warn("Duplicate entity name: " + name + ", id1: " + mapIdByName.get(name) + ", id2: " + i);
                    }

                    if (mapClassByName.containsKey(name)) {
                        Photon.LOGGER.warn("Duplicate entity name: " + name + ", class1: " + mapClassByName.get(name) + ", class2: " + cls);
                    }

                    mapIdByClass.put(cls, i);
                    mapIdByName.put(name, i);
                    mapClassByName.put(name, cls);
                }
            }
        }
    }
}
