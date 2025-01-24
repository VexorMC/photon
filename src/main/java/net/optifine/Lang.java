package net.optifine;

import com.google.common.base.Splitter;
import com.google.common.collect.Iterables;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import dev.vexor.photon.Config;
import net.minecraft.client.resource.language.I18n;
import net.minecraft.resource.ResourcePack;
import net.minecraft.util.Identifier;
import org.apache.commons.io.Charsets;
import org.apache.commons.io.IOUtils;

public class Lang {
    private static final Splitter splitter = Splitter.on('=').limit(2);
    private static final Pattern pattern = Pattern.compile("%(\\d+\\$)?[\\d\\.]*[df]");


    private static void loadResources(ResourcePack rp, String[] files, Map localeProperties) {
        try {
            for (int i = 0; i < files.length; i++) {
                String file = files[i];
                Identifier loc = new Identifier(file);
                if (rp.contains(loc)) {
                    InputStream in = rp.open(loc);
                    if (in != null) {
                        loadLocaleData(in, localeProperties);
                    }
                }
            }
        } catch (IOException var7) {
            var7.printStackTrace();
        }
    }

    public static void loadLocaleData(InputStream is, Map localeProperties) throws IOException {
        Iterator it = IOUtils.readLines(is, Charsets.UTF_8).iterator();
        is.close();

        while (it.hasNext()) {
            String line = (String)it.next();
            if (!line.isEmpty() && line.charAt(0) != '#') {
                String[] parts = (String[])Iterables.toArray(splitter.split(line), String.class);
                if (parts != null && parts.length == 2) {
                    String key = parts[0];
                    String value = pattern.matcher(parts[1]).replaceAll("%$1s");
                    localeProperties.put(key, value);
                }
            }
        }
    }

    public static String get(String key) {
        return I18n.translate(key, new Object[0]);
    }

    public static String get(String key, String def) {
        String str = I18n.translate(key, new Object[0]);
        return str != null && !str.equals(key) ? str : def;
    }

    public static String getOn() {
        return I18n.translate("options.on", new Object[0]);
    }

    public static String getOff() {
        return I18n.translate("options.off", new Object[0]);
    }

    public static String getFast() {
        return I18n.translate("options.graphics.fast", new Object[0]);
    }

    public static String getFancy() {
        return I18n.translate("options.graphics.fancy", new Object[0]);
    }

    public static String getDefault() {
        return I18n.translate("generator.default", new Object[0]);
    }
}
