package dev.vexor.photon;

import com.mojang.blaze3d.platform.GLX;

import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Array;
import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.imageio.ImageIO;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.GameOptions;
import net.minecraft.client.render.LoadingScreenRenderer;
import net.minecraft.client.render.WorldRenderer;
import net.minecraft.client.resource.ResourcePackLoader;
import net.minecraft.client.texture.NativeImageBackedTexture;
import net.minecraft.client.texture.SpriteAtlasTexture;
import net.minecraft.client.texture.TextureManager;
import net.minecraft.client.util.GlAllocationUtils;
import net.minecraft.resource.DefaultResourcePack;
import net.minecraft.resource.Resource;
import net.minecraft.resource.ResourceManager;
import net.minecraft.resource.ResourcePack;
import net.minecraft.util.Identifier;
import net.minecraft.util.MetricsData;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.optifine.config.GlVersion;
import net.optifine.shaders.Shaders;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.lwjgl.opengl.DisplayMode;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL30;
import org.lwjgl.opengl.GLContext;

public class Config {
    private static String build = null;
    private static String newRelease = null;
    private static boolean notify64BitJava = false;
    public static String openGlVersion = null;
    public static String openGlRenderer = null;
    public static String openGlVendor = null;
    public static String[] openGlExtensions = null;
    public static int minecraftVersionInt = -1;
    public static boolean fancyFogAvailable = false;
    public static boolean occlusionAvailable = false;
    private static GameOptions gameSettings = null;
    private static MinecraftClient minecraft = MinecraftClient.getInstance();
    private static boolean initialized = false;
    private static Thread minecraftThread = null;
    private static DisplayMode desktopDisplayMode = null;
    private static DisplayMode[] displayModes = null;
    private static int antialiasingLevel = 0;
    private static int availableProcessors = 0;
    public static boolean zoomMode = false;
    public static boolean zoomSmoothCamera = false;
    private static int texturePackClouds = 0;
    public static boolean waterOpacityChanged = false;
    private static boolean fullscreenModeChecked = false;
    private static boolean desktopModeChecked = false;
    private static DefaultResourcePack defaultResourcePackLazy = null;
    public static final Float DEF_ALPHA_FUNC_LEVEL = 0.1F;
    private static final Logger LOGGER = LogManager.getLogger();
    public static final boolean logDetail = System.getProperty("log.detail", "false").equals("true");
    private static String mcDebugLast = null;
    private static int fpsMinLast = 0;
    public static float renderPartialTicks;
    public static GlVersion glVersion = null;
    public static GlVersion glslVersion = null;

    private Config() {
    }

    public static String[] readLines(File file) throws IOException {
        FileInputStream fis = new FileInputStream(file);
        return readLines(fis);
    }

    public static String[] readLines(InputStream is) throws IOException {
        List<String> list = new ArrayList();
        InputStreamReader isr = new InputStreamReader(is, "ASCII");
        BufferedReader br = new BufferedReader(isr);

        while (true) {
            String line = br.readLine();
            if (line == null) {
                return list.toArray(new String[list.size()]);
            }

            list.add(line);
        }
    }
    public static int getMinecraftVersionInt() {
        if (minecraftVersionInt < 0) {
            String[] verStrs = tokenize("1.8.9", ".");
            int ver = 0;
            if (verStrs.length > 0) {
                ver += 10000 * parseInt(verStrs[0], 0);
            }

            if (verStrs.length > 1) {
                ver += 100 * parseInt(verStrs[1], 0);
            }

            if (verStrs.length > 2) {
                ver += parseInt(verStrs[2], 0);
            }

            minecraftVersionInt = ver;
        }

        return minecraftVersionInt;
    }

    private static GlVersion getGlVersionLwjgl() {
        if (GLContext.getCapabilities().OpenGL44) {
            return new GlVersion(4, 4);
        } else if (GLContext.getCapabilities().OpenGL43) {
            return new GlVersion(4, 3);
        } else if (GLContext.getCapabilities().OpenGL42) {
            return new GlVersion(4, 2);
        } else if (GLContext.getCapabilities().OpenGL41) {
            return new GlVersion(4, 1);
        } else if (GLContext.getCapabilities().OpenGL40) {
            return new GlVersion(4, 0);
        } else if (GLContext.getCapabilities().OpenGL33) {
            return new GlVersion(3, 3);
        } else if (GLContext.getCapabilities().OpenGL32) {
            return new GlVersion(3, 2);
        } else if (GLContext.getCapabilities().OpenGL31) {
            return new GlVersion(3, 1);
        } else if (GLContext.getCapabilities().OpenGL30) {
            return new GlVersion(3, 0);
        } else if (GLContext.getCapabilities().OpenGL21) {
            return new GlVersion(2, 1);
        } else if (GLContext.getCapabilities().OpenGL20) {
            return new GlVersion(2, 0);
        } else if (GLContext.getCapabilities().OpenGL15) {
            return new GlVersion(1, 5);
        } else if (GLContext.getCapabilities().OpenGL14) {
            return new GlVersion(1, 4);
        } else if (GLContext.getCapabilities().OpenGL13) {
            return new GlVersion(1, 3);
        } else if (GLContext.getCapabilities().OpenGL12) {
            return new GlVersion(1, 2);
        } else {
            return GLContext.getCapabilities().OpenGL11 ? new GlVersion(1, 1) : new GlVersion(1, 0);
        }
    }


    public static GlVersion getGlslVersion() {
        if (glslVersion == null) {
            String verStr = GL11.glGetString(35724);
            glslVersion = parseGlVersion(verStr, null);
            if (glslVersion == null) {
                glslVersion = new GlVersion(1, 10);
            }
        }

        return glslVersion;
    }

    public static GlVersion parseGlVersion(String versionString, GlVersion def) {
        try {
            if (versionString == null) {
                return def;
            } else {
                Pattern REGEXP_VERSION = Pattern.compile("([0-9]+)\\.([0-9]+)(\\.([0-9]+))?(.+)?");
                Matcher matcher = REGEXP_VERSION.matcher(versionString);
                if (!matcher.matches()) {
                    return def;
                } else {
                    int major = Integer.parseInt(matcher.group(1));
                    int minor = Integer.parseInt(matcher.group(2));
                    int release = matcher.group(4) != null ? Integer.parseInt(matcher.group(4)) : 0;
                    String suffix = matcher.group(5);
                    return new GlVersion(major, minor, release, suffix);
                }
            }
        } catch (Exception var8) {
            var8.printStackTrace();
            return def;
        }
    }


    public static GlVersion getGlVersion() {
        if (glVersion == null) {
            String verStr = GL11.glGetString(7938);
            glVersion = parseGlVersion(verStr, null);
            if (glVersion == null) {
                glVersion = getGlVersionLwjgl();
            }

            if (glVersion == null) {
                glVersion = new GlVersion(1, 0);
            }
        }

        return glVersion;
    }

    public static String[] getOpenGlExtensions() {
        if (openGlExtensions == null) {
            openGlExtensions = detectOpenGlExtensions();
        }

        return openGlExtensions;
    }

    private static String[] detectOpenGlExtensions() {
        int countExt = GL11.glGetInteger(33309);
        if (countExt > 0) {
            String[] exts = new String[countExt];

            for (int i = 0; i < countExt; i++) {
                exts[i] = GL30.glGetStringi(7939, i);
            }

            return exts;
        }
        return new String[0];
    }

    public static String[] tokenize(String str, String delim) {
        StringTokenizer tok = new StringTokenizer(str, delim);
        List<String> list = new ArrayList<>();

        while (tok.hasMoreTokens()) {
            String token = tok.nextToken();
            list.add(token);
        }

        return list.toArray(new String[0]);
    }

    public static String arrayToString(Object[] arr) {
        return arrayToString(arr, ", ");
    }

    public static String arrayToString(Object[] arr, String separator) {
        if (arr == null) {
            return "";
        } else {
            StringBuffer buf = new StringBuffer(arr.length * 5);

            for (int i = 0; i < arr.length; i++) {
                Object obj = arr[i];
                if (i > 0) {
                    buf.append(separator);
                }

                buf.append(String.valueOf(obj));
            }

            return buf.toString();
        }
    }

    public static String arrayToString(int[] arr) {
        return arrayToString(arr, ", ");
    }

    public static String arrayToString(int[] arr, String separator) {
        if (arr == null) {
            return "";
        } else {
            StringBuffer buf = new StringBuffer(arr.length * 5);

            for (int i = 0; i < arr.length; i++) {
                int x = arr[i];
                if (i > 0) {
                    buf.append(separator);
                }

                buf.append(String.valueOf(x));
            }

            return buf.toString();
        }
    }

    public static String arrayToString(float[] arr) {
        return arrayToString(arr, ", ");
    }

    public static String arrayToString(float[] arr, String separator) {
        if (arr == null) {
            return "";
        } else {
            StringBuffer buf = new StringBuffer(arr.length * 5);

            for (int i = 0; i < arr.length; i++) {
                float x = arr[i];
                if (i > 0) {
                    buf.append(separator);
                }

                buf.append(String.valueOf(x));
            }

            return buf.toString();
        }
    }

    public static MinecraftClient getMinecraft() {
        return minecraft;
    }

    public static TextureManager getTextureManager() {
        return minecraft.getTextureManager();
    }

    public static ResourceManager getResourceManager() {
        return minecraft.getResourceManager();
    }

    public static InputStream getResourceStream(Identifier location) throws IOException {
        return getResourceStream(minecraft.getResourceManager(), location);
    }

    public static InputStream getResourceStream(ResourceManager resourceManager, Identifier location) throws IOException {
        Resource res = resourceManager.getResource(location);
        return res == null ? null : res.getInputStream();
    }

    public static Resource getResource(Identifier location) throws IOException {
        return minecraft.getResourceManager().getResource(location);
    }


    public static boolean hasResource(Identifier location) {
        return hasResource(minecraft.getResourceManager(), location);
    }


    public static boolean hasResource(ResourceManager resourceManager, Identifier location) {
        try {
            Resource res = resourceManager.getResource(location);
            return res != null;
        } catch (IOException var3) {
            return false;
        }
    }

    public static WorldRenderer getRenderGlobal() {
        return minecraft.worldRenderer;
    }

    public static boolean between(int val, int min, int max) {
        return val >= min && val <= max;
    }

    public static boolean between(float val, float min, float max) {
        return val >= min && val <= max;
    }


    public static int parseInt(String str, int defVal) {
        try {
            if (str == null) {
                return defVal;
            } else {
                str = str.trim();
                return Integer.parseInt(str);
            }
        } catch (NumberFormatException var3) {
            return defVal;
        }
    }

    public static float parseFloat(String str, float defVal) {
        try {
            if (str == null) {
                return defVal;
            } else {
                str = str.trim();
                return Float.parseFloat(str);
            }
        } catch (NumberFormatException var3) {
            return defVal;
        }
    }

    public static boolean parseBoolean(String str, boolean defVal) {
        try {
            if (str == null) {
                return defVal;
            } else {
                str = str.trim();
                return Boolean.parseBoolean(str);
            }
        } catch (NumberFormatException var3) {
            return defVal;
        }
    }

    public static Boolean parseBoolean(String str, Boolean defVal) {
        try {
            if (str == null) {
                return defVal;
            } else {
                str = str.trim().toLowerCase();
                if (str.equals("true")) {
                    return Boolean.TRUE;
                } else {
                    return str.equals("false") ? Boolean.FALSE : defVal;
                }
            }
        } catch (NumberFormatException var3) {
            return defVal;
        }
    }

    public static boolean isShaders() {
        return Shaders.shaderPackLoaded;
    }
    public static String readFile(File file) throws IOException {
        FileInputStream fin = new FileInputStream(file);
        return readInputStream(fin, "ASCII");
    }

    public static String readInputStream(InputStream in) throws IOException {
        return readInputStream(in, "ASCII");
    }

    public static String readInputStream(InputStream in, String encoding) throws IOException {
        InputStreamReader inr = new InputStreamReader(in, encoding);
        BufferedReader br = new BufferedReader(inr);
        StringBuffer sb = new StringBuffer();

        while (true) {
            String line = br.readLine();
            if (line == null) {
                return sb.toString();
            }

            sb.append(line);
            sb.append("\n");
        }
    }

    public static byte[] readAll(InputStream is) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        byte[] buf = new byte[1024];

        while (true) {
            int len = is.read(buf);
            if (len < 0) {
                is.close();
                return baos.toByteArray();
            }

            baos.write(buf, 0, len);
        }
    }

    public static GameOptions getGameSettings() {
        return MinecraftClient.getInstance().options;
    }

    public static String getNewRelease() {
        return newRelease;
    }

    public static void setNewRelease(String newRelease) {
        Config.newRelease = newRelease;
    }

    public static int compareRelease(String rel1, String rel2) {
        String[] rels1 = splitRelease(rel1);
        String[] rels2 = splitRelease(rel2);
        String branch1 = rels1[0];
        String branch2 = rels2[0];
        if (!branch1.equals(branch2)) {
            return branch1.compareTo(branch2);
        } else {
            int rev1 = parseInt(rels1[1], -1);
            int rev2 = parseInt(rels2[1], -1);
            if (rev1 != rev2) {
                return rev1 - rev2;
            } else {
                String suf1 = rels1[2];
                String suf2 = rels2[2];
                if (!suf1.equals(suf2)) {
                    if (suf1.isEmpty()) {
                        return 1;
                    }

                    if (suf2.isEmpty()) {
                        return -1;
                    }
                }

                return suf1.compareTo(suf2);
            }
        }
    }

    private static String[] splitRelease(String relStr) {
        if (relStr != null && relStr.length() > 0) {
            Pattern p = Pattern.compile("([A-Z])([0-9]+)(.*)");
            Matcher m = p.matcher(relStr);
            if (!m.matches()) {
                return new String[]{"", "", ""};
            } else {
                String branch = normalize(m.group(1));
                String revision = normalize(m.group(2));
                String suffix = normalize(m.group(3));
                return new String[]{branch, revision, suffix};
            }
        } else {
            return new String[]{"", "", ""};
        }
    }

    public static int intHash(int x) {
        x = x ^ 61 ^ x >> 16;
        x += x << 3;
        x ^= x >> 4;
        x *= 668265261;
        return x ^ x >> 15;
    }

    public static int getRandom(BlockPos blockPos, int face) {
        int rand = intHash(face + 37);
        rand = intHash(rand + blockPos.getX());
        rand = intHash(rand + blockPos.getZ());
        return intHash(rand + blockPos.getY());
    }

    public static int getAvailableProcessors() {
        return availableProcessors;
    }

    public static void updateAvailableProcessors() {
        availableProcessors = Runtime.getRuntime().availableProcessors();
    }

    public static boolean isSingleProcessor() {
        return getAvailableProcessors() <= 1;
    }


    public static int getChunkViewDistance() {
        return gameSettings == null ? 10 : gameSettings.viewDistance;
    }

    public static boolean equals(Object o1, Object o2) {
        if (o1 == o2) {
            return true;
        } else {
            return o1 == null ? false : o1.equals(o2);
        }
    }

    public static boolean equalsOne(Object a, Object[] bs) {
        if (bs == null) {
            return false;
        } else {
            for (int i = 0; i < bs.length; i++) {
                Object b = bs[i];
                if (equals(a, b)) {
                    return true;
                }
            }

            return false;
        }
    }

    public static boolean equalsOne(int val, int[] vals) {
        for (int i = 0; i < vals.length; i++) {
            if (vals[i] == val) {
                return true;
            }
        }

        return false;
    }

    public static boolean isSameOne(Object a, Object[] bs) {
        if (bs == null) {
            return false;
        } else {
            for (int i = 0; i < bs.length; i++) {
                Object b = bs[i];
                if (a == b) {
                    return true;
                }
            }

            return false;
        }
    }

    public static String normalize(String s) {
        return s == null ? "" : s;
    }

    private static ByteBuffer readIconImage(InputStream is) throws IOException {
        BufferedImage var2 = ImageIO.read(is);
        int[] var3 = var2.getRGB(0, 0, var2.getWidth(), var2.getHeight(), (int[])null, 0, var2.getWidth());
        ByteBuffer var4 = ByteBuffer.allocate(4 * var3.length);

        for (int var8 : var3) {
            var4.putInt(var8 << 8 | var8 >> 24 & 0xFF);
        }

        ((Buffer)var4).flip();
        return var4;
    }

    public static void updateFramebufferSize() {
        minecraft.getFramebuffer().resize(minecraft.width, minecraft.height);
        if (minecraft.gameRenderer != null) {
            minecraft.gameRenderer.onResized(minecraft.width, minecraft.height);
        }

        minecraft.loadingScreenRenderer = new LoadingScreenRenderer(minecraft);
    }

    public static Object[] addObjectToArray(Object[] arr, Object obj) {
        if (arr == null) {
            throw new NullPointerException("The given array is NULL");
        } else {
            int arrLen = arr.length;
            int newLen = arrLen + 1;
            Object[] newArr = (Object[])Array.newInstance(arr.getClass().getComponentType(), newLen);
            System.arraycopy(arr, 0, newArr, 0, arrLen);
            newArr[arrLen] = obj;
            return newArr;
        }
    }

    public static Object[] addObjectToArray(Object[] arr, Object obj, int index) {
        List list = new ArrayList<>(Arrays.asList(arr));
        list.add(index, obj);
        Object[] newArr = (Object[])Array.newInstance(arr.getClass().getComponentType(), list.size());
        return list.toArray(newArr);
    }

    public static Object[] addObjectsToArray(Object[] arr, Object[] objs) {
        if (arr == null) {
            throw new NullPointerException("The given array is NULL");
        } else if (objs.length == 0) {
            return arr;
        } else {
            int arrLen = arr.length;
            int newLen = arrLen + objs.length;
            Object[] newArr = (Object[])Array.newInstance(arr.getClass().getComponentType(), newLen);
            System.arraycopy(arr, 0, newArr, 0, arrLen);
            System.arraycopy(objs, 0, newArr, arrLen, objs.length);
            return newArr;
        }
    }

    public static Object[] removeObjectFromArray(Object[] arr, Object obj) {
        List list = new ArrayList<>(Arrays.asList(arr));
        list.remove(obj);
        return collectionToArray(list, arr.getClass().getComponentType());
    }

    public static Object[] collectionToArray(Collection coll, Class elementClass) {
        if (coll == null) {
            return null;
        } else if (elementClass == null) {
            return null;
        } else if (elementClass.isPrimitive()) {
            throw new IllegalArgumentException("Can not make arrays with primitive elements (int, double), element class: " + elementClass);
        } else {
            Object[] array = (Object[])Array.newInstance(elementClass, coll.size());
            return coll.toArray(array);
        }
    }

    public static int getFpsMin() {
        if (minecraft.fpsDebugString == mcDebugLast) {
            return fpsMinLast;
        } else {
            mcDebugLast = minecraft.fpsDebugString;
            MetricsData ft = minecraft.getMetricsData();
            long[] frames = ft.getSamples();
            int index = ft.getCurrentIndex();
            int indexEnd = ft.getStartIndex();
            if (index == indexEnd) {
                return fpsMinLast;
            } else {
                int fps = MinecraftClient.getCurrentFps();
                if (fps <= 0) {
                    fps = 1;
                }

                long timeAvgNs = (long)(1.0 / (double)fps * 1.0E9);
                long timeMaxNs = timeAvgNs;
                long timeTotalNs = 0L;

                for (int ix = MathHelper.floorMod(index - 1, frames.length);
                     ix != indexEnd && (double)timeTotalNs < 1.0E9;
                     ix = MathHelper.floorMod(ix - 1, frames.length)
                ) {
                    long timeNs = frames[ix];
                    if (timeNs > timeMaxNs) {
                        timeMaxNs = timeNs;
                    }

                    timeTotalNs += timeNs;
                }

                double timeMaxSec = (double)timeMaxNs / 1.0E9;
                fpsMinLast = (int)(1.0 / timeMaxSec);
                return fpsMinLast;
            }
        }
    }

    private static String getUpdates(String str) {
        int pos1 = str.indexOf(40);
        if (pos1 < 0) {
            return "";
        } else {
            int pos2 = str.indexOf(32, pos1);
            return pos2 < 0 ? "" : str.substring(pos1 + 1, pos2);
        }
    }

    public static int getBitsOs() {
        String progFiles86 = System.getenv("ProgramFiles(X86)");
        return progFiles86 != null ? 64 : 32;
    }

    public static int getBitsJre() {
        String[] propNames = new String[]{"sun.arch.data.model", "com.ibm.vm.bitmode", "os.arch"};

        for (int i = 0; i < propNames.length; i++) {
            String propName = propNames[i];
            String propVal = System.getProperty(propName);
            if (propVal != null && propVal.contains("64")) {
                return 64;
            }
        }

        return 32;
    }

    public static boolean isNotify64BitJava() {
        return notify64BitJava;
    }

    public static void setNotify64BitJava(boolean flag) {
        notify64BitJava = flag;
    }

    public static boolean isConnectedModels() {
        return false;
    }

    public static int[] addIntToArray(int[] intArray, int intValue) {
        return addIntsToArray(intArray, new int[]{intValue});
    }

    public static int[] addIntsToArray(int[] intArray, int[] copyFrom) {
        if (intArray != null && copyFrom != null) {
            int arrLen = intArray.length;
            int newLen = arrLen + copyFrom.length;
            int[] newArray = new int[newLen];
            System.arraycopy(intArray, 0, newArray, 0, arrLen);

            for (int index = 0; index < copyFrom.length; index++) {
                newArray[index + arrLen] = copyFrom[index];
            }

            return newArray;
        } else {
            throw new NullPointerException("The given array is NULL");
        }
    }

    public static NativeImageBackedTexture getMojangLogoTexture(NativeImageBackedTexture texDefault) {
        try {
            Identifier locationMojangPng = new Identifier("textures/gui/title/mojang.png");
            InputStream in = getResourceStream(locationMojangPng);
            if (in == null) {
                return texDefault;
            } else {
                BufferedImage bi = ImageIO.read(in);
                return bi == null ? texDefault : new NativeImageBackedTexture(bi);
            }
        } catch (Exception var5) {
            Photon.LOGGER.warn(var5.getClass().getName() + ": " + var5.getMessage());
            return texDefault;
        }
    }

    public static void writeFile(File file, String str) throws IOException {
        FileOutputStream fos = new FileOutputStream(file);
        byte[] bytes = str.getBytes("ASCII");
        fos.write(bytes);
        fos.close();
    }

    public static SpriteAtlasTexture getTextureMap() {
        return getMinecraft().getSpriteAtlasTexture();
    }


    public static int[] toPrimitive(Integer[] arr) {
        if (arr == null) {
            return null;
        } else if (arr.length == 0) {
            return new int[0];
        } else {
            int[] intArr = new int[arr.length];

            for (int i = 0; i < intArr.length; i++) {
                intArr[i] = arr[i];
            }

            return intArr;
        }
    }


    public static boolean isVbo() {
        return GLX.supportsVbo();
    }


    public static String arrayToString(boolean[] arr, String separator) {
        if (arr == null) {
            return "";
        } else {
            StringBuffer buf = new StringBuffer(arr.length * 5);

            for (int i = 0; i < arr.length; i++) {
                boolean x = arr[i];
                if (i > 0) {
                    buf.append(separator);
                }

                buf.append(String.valueOf(x));
            }

            return buf.toString();
        }
    }

    public static boolean isIntegratedServerRunning() {
        return minecraft.getServer() == null ? false : minecraft.isIntegratedServerRunning();
    }

    public static IntBuffer createDirectIntBuffer(int capacity) {
        return GlAllocationUtils.allocateByteBuffer(capacity << 2).asIntBuffer();
    }

    public static String getGlErrorString(int err) {
        switch (err) {
            case 0:
                return "No error";
            case 1280:
                return "Invalid enum";
            case 1281:
                return "Invalid value";
            case 1282:
                return "Invalid operation";
            case 1283:
                return "Stack overflow";
            case 1284:
                return "Stack underflow";
            case 1285:
                return "Out of memory";
            case 1286:
                return "Invalid framebuffer operation";
            default:
                return "Unknown";
        }
    }

    public static boolean isTrue(Boolean val) {
        return val != null && val;
    }

    public static boolean isQuadsToTriangles() {
        return !isShaders() ? false : !Shaders.canRenderQuads();
    }

    public static void checkNull(Object obj, String msg) throws NullPointerException {
        if (obj == null) {
            throw new NullPointerException(msg);
        }
    }
}
