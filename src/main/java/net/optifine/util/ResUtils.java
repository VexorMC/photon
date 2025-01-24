package net.optifine.util;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public class ResUtils {
    public static Properties readProperties(InputStream in, String module) {
        if (in == null) {
            return null;
        } else {
            try {
                Properties props = new PropertiesOrdered();
                props.load(in);
                in.close();
                return props;
            } catch (FileNotFoundException var3) {
                return null;
            } catch (IOException var4) {
                return null;
            }
        }
    }
}
