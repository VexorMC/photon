package net.optifine.shaders;

import com.google.common.base.Charsets;
import com.mojang.blaze3d.platform.GLX;
import com.mojang.blaze3d.platform.GlStateManager;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import dev.vexor.photon.Config;
import dev.vexor.photon.Photon;
import dev.vexor.photon.format.VertexFormatHooks;
import dev.vexor.photon.tex.ExtendedTexture;
import net.minecraft.block.Block;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.material.Material;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.GameOptions;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.DiffuseLighting;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.resource.language.I18n;
import net.minecraft.client.resource.metadata.TextureResourceMetadata;
import net.minecraft.client.texture.SpriteAtlasTexture;
import net.minecraft.client.texture.Texture;
import net.minecraft.client.util.GlAllocationUtils;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.text.LiteralText;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import net.optifine.Lang;
import net.optifine.config.ConnectedParser;
import net.optifine.expr.IExpressionBool;
import net.optifine.render.GlAlphaState;
import net.optifine.render.GlBlendState;
import net.optifine.shaders.config.EnumShaderOption;
import net.optifine.shaders.config.MacroProcessor;
import net.optifine.shaders.config.MacroState;
import net.optifine.shaders.config.PropertyDefaultFastFancyOff;
import net.optifine.shaders.config.PropertyDefaultTrueFalse;
import net.optifine.shaders.config.RenderScale;
import net.optifine.shaders.config.ScreenShaderOptions;
import net.optifine.shaders.config.ShaderLine;
import net.optifine.shaders.config.ShaderOption;
import net.optifine.shaders.config.ShaderOptionProfile;
import net.optifine.shaders.config.ShaderOptionRest;
import net.optifine.shaders.config.ShaderPackParser;
import net.optifine.shaders.config.ShaderParser;
import net.optifine.shaders.config.ShaderProfile;
import net.optifine.shaders.uniform.CustomUniforms;
import net.optifine.shaders.uniform.ShaderUniform1f;
import net.optifine.shaders.uniform.ShaderUniform1i;
import net.optifine.shaders.uniform.ShaderUniform2i;
import net.optifine.shaders.uniform.ShaderUniform3f;
import net.optifine.shaders.uniform.ShaderUniform4f;
import net.optifine.shaders.uniform.ShaderUniform4i;
import net.optifine.shaders.uniform.ShaderUniformM4;
import net.optifine.shaders.uniform.ShaderUniforms;
import net.optifine.shaders.uniform.Smoother;
import net.optifine.texture.InternalFormat;
import net.optifine.texture.PixelFormat;
import net.optifine.texture.PixelType;
import net.optifine.texture.TextureType;
import net.optifine.util.*;
import org.apache.commons.io.IOUtils;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.ARBGeometryShader4;
import org.lwjgl.opengl.ARBShaderObjects;
import org.lwjgl.opengl.ARBVertexShader;
import org.lwjgl.opengl.ContextCapabilities;
import org.lwjgl.opengl.EXTFramebufferObject;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL20;
import org.lwjgl.opengl.GL30;
import org.lwjgl.opengl.GLContext;
import org.lwjgl.util.glu.GLU;
import org.lwjgl.util.vector.Vector4f;

public class Shaders {
   static MinecraftClient mc;
   static GameRenderer entityRenderer;
   public static boolean isInitializedOnce = false;
   public static boolean isShaderPackInitialized = false;
   public static ContextCapabilities capabilities;
   public static String glVersionString;
   public static String glVendorString;
   public static String glRendererString;
   public static boolean hasGlGenMipmap = false;
   public static int countResetDisplayLists = 0;
   private static int renderDisplayWidth = 0;
   private static int renderDisplayHeight = 0;
   public static int renderWidth = 0;
   public static int renderHeight = 0;
   public static boolean isRenderingWorld = false;
   public static boolean isRenderingSky = false;
   public static boolean isCompositeRendered = false;
   public static boolean isRenderingDfb = false;
   public static boolean isShadowPass = false;
   public static boolean isEntitiesGlowing = false;
   public static boolean isSleeping;
   private static boolean isRenderingFirstPersonHand;
   private static boolean isHandRenderedMain;
   private static boolean isHandRenderedOff;
   private static boolean skipRenderHandMain;
   private static boolean skipRenderHandOff;
   public static boolean renderItemKeepDepthMask = false;
   public static boolean itemToRenderMainTranslucent = false;
   public static boolean itemToRenderOffTranslucent = false;
   static float[] sunPosition = new float[4];
   static float[] moonPosition = new float[4];
   static float[] shadowLightPosition = new float[4];
   static float[] upPosition = new float[4];
   static float[] shadowLightPositionVector = new float[4];
   static float[] upPosModelView = new float[]{0.0F, 100.0F, 0.0F, 0.0F};
   static float[] sunPosModelView = new float[]{0.0F, 100.0F, 0.0F, 0.0F};
   static float[] moonPosModelView = new float[]{0.0F, -100.0F, 0.0F, 0.0F};
   private static float[] tempMat = new float[16];
   static float clearColorR;
   static float clearColorG;
   static float clearColorB;
   static float skyColorR;
   static float skyColorG;
   static float skyColorB;
   static long worldTime = 0L;
   static long lastWorldTime = 0L;
   static long diffWorldTime = 0L;
   static float celestialAngle = 0.0F;
   static float sunAngle = 0.0F;
   static float shadowAngle = 0.0F;
   static int moonPhase = 0;
   static long systemTime = 0L;
   static long lastSystemTime = 0L;
   static long diffSystemTime = 0L;
   static int frameCounter = 0;
   static float frameTime = 0.0F;
   static float frameTimeCounter = 0.0F;
   static int systemTimeInt32 = 0;
   static float rainStrength = 0.0F;
   static float wetness = 0.0F;
   public static float wetnessHalfLife = 600.0F;
   public static float drynessHalfLife = 200.0F;
   public static float eyeBrightnessHalflife = 10.0F;
   static boolean usewetness = false;
   static int isEyeInWater = 0;
   static int eyeBrightness = 0;
   static float eyeBrightnessFadeX = 0.0F;
   static float eyeBrightnessFadeY = 0.0F;
   static float eyePosY = 0.0F;
   static float centerDepth = 0.0F;
   static float centerDepthSmooth = 0.0F;
   static float centerDepthSmoothHalflife = 1.0F;
   static boolean centerDepthSmoothEnabled = false;
   static int superSamplingLevel = 1;
   static float nightVision = 0.0F;
   static float blindness = 0.0F;
   static boolean lightmapEnabled = false;
   static boolean fogEnabled = true;
   public static int entityAttrib = 10;
   public static int midTexCoordAttrib = 11;
   public static int tangentAttrib = 12;
   public static boolean useEntityAttrib = false;
   public static boolean useMidTexCoordAttrib = false;
   public static boolean useTangentAttrib = false;
   public static boolean progUseEntityAttrib = false;
   public static boolean progUseMidTexCoordAttrib = false;
   public static boolean progUseTangentAttrib = false;
   private static boolean progArbGeometryShader4 = false;
   private static int progMaxVerticesOut = 3;
   private static boolean hasGeometryShaders = false;
   public static int atlasSizeX = 0;
   public static int atlasSizeY = 0;
   private static ShaderUniforms shaderUniforms = new ShaderUniforms();
   public static ShaderUniform4f uniform_entityColor = shaderUniforms.make4f("entityColor");
   public static ShaderUniform1i uniform_entityId = shaderUniforms.make1i("entityId");
   public static ShaderUniform1i uniform_blockEntityId = shaderUniforms.make1i("blockEntityId");
   public static ShaderUniform1i uniform_texture = shaderUniforms.make1i("texture");
   public static ShaderUniform1i uniform_lightmap = shaderUniforms.make1i("lightmap");
   public static ShaderUniform1i uniform_normals = shaderUniforms.make1i("normals");
   public static ShaderUniform1i uniform_specular = shaderUniforms.make1i("specular");
   public static ShaderUniform1i uniform_shadow = shaderUniforms.make1i("shadow");
   public static ShaderUniform1i uniform_watershadow = shaderUniforms.make1i("watershadow");
   public static ShaderUniform1i uniform_shadowtex0 = shaderUniforms.make1i("shadowtex0");
   public static ShaderUniform1i uniform_shadowtex1 = shaderUniforms.make1i("shadowtex1");
   public static ShaderUniform1i uniform_depthtex0 = shaderUniforms.make1i("depthtex0");
   public static ShaderUniform1i uniform_depthtex1 = shaderUniforms.make1i("depthtex1");
   public static ShaderUniform1i uniform_shadowcolor = shaderUniforms.make1i("shadowcolor");
   public static ShaderUniform1i uniform_shadowcolor0 = shaderUniforms.make1i("shadowcolor0");
   public static ShaderUniform1i uniform_shadowcolor1 = shaderUniforms.make1i("shadowcolor1");
   public static ShaderUniform1i uniform_noisetex = shaderUniforms.make1i("noisetex");
   public static ShaderUniform1i uniform_gcolor = shaderUniforms.make1i("gcolor");
   public static ShaderUniform1i uniform_gdepth = shaderUniforms.make1i("gdepth");
   public static ShaderUniform1i uniform_gnormal = shaderUniforms.make1i("gnormal");
   public static ShaderUniform1i uniform_composite = shaderUniforms.make1i("composite");
   public static ShaderUniform1i uniform_gaux1 = shaderUniforms.make1i("gaux1");
   public static ShaderUniform1i uniform_gaux2 = shaderUniforms.make1i("gaux2");
   public static ShaderUniform1i uniform_gaux3 = shaderUniforms.make1i("gaux3");
   public static ShaderUniform1i uniform_gaux4 = shaderUniforms.make1i("gaux4");
   public static ShaderUniform1i uniform_colortex0 = shaderUniforms.make1i("colortex0");
   public static ShaderUniform1i uniform_colortex1 = shaderUniforms.make1i("colortex1");
   public static ShaderUniform1i uniform_colortex2 = shaderUniforms.make1i("colortex2");
   public static ShaderUniform1i uniform_colortex3 = shaderUniforms.make1i("colortex3");
   public static ShaderUniform1i uniform_colortex4 = shaderUniforms.make1i("colortex4");
   public static ShaderUniform1i uniform_colortex5 = shaderUniforms.make1i("colortex5");
   public static ShaderUniform1i uniform_colortex6 = shaderUniforms.make1i("colortex6");
   public static ShaderUniform1i uniform_colortex7 = shaderUniforms.make1i("colortex7");
   public static ShaderUniform1i uniform_gdepthtex = shaderUniforms.make1i("gdepthtex");
   public static ShaderUniform1i uniform_depthtex2 = shaderUniforms.make1i("depthtex2");
   public static ShaderUniform1i uniform_tex = shaderUniforms.make1i("tex");
   public static ShaderUniform1i uniform_heldItemId = shaderUniforms.make1i("heldItemId");
   public static ShaderUniform1i uniform_heldBlockLightValue = shaderUniforms.make1i("heldBlockLightValue");
   public static ShaderUniform1i uniform_heldItemId2 = shaderUniforms.make1i("heldItemId2");
   public static ShaderUniform1i uniform_heldBlockLightValue2 = shaderUniforms.make1i("heldBlockLightValue2");
   public static ShaderUniform1i uniform_fogMode = shaderUniforms.make1i("fogMode");
   public static ShaderUniform1f uniform_fogDensity = shaderUniforms.make1f("fogDensity");
   public static ShaderUniform3f uniform_fogColor = shaderUniforms.make3f("fogColor");
   public static ShaderUniform3f uniform_skyColor = shaderUniforms.make3f("skyColor");
   public static ShaderUniform1i uniform_worldTime = shaderUniforms.make1i("worldTime");
   public static ShaderUniform1i uniform_worldDay = shaderUniforms.make1i("worldDay");
   public static ShaderUniform1i uniform_moonPhase = shaderUniforms.make1i("moonPhase");
   public static ShaderUniform1i uniform_frameCounter = shaderUniforms.make1i("frameCounter");
   public static ShaderUniform1f uniform_frameTime = shaderUniforms.make1f("frameTime");
   public static ShaderUniform1f uniform_frameTimeCounter = shaderUniforms.make1f("frameTimeCounter");
   public static ShaderUniform1f uniform_sunAngle = shaderUniforms.make1f("sunAngle");
   public static ShaderUniform1f uniform_shadowAngle = shaderUniforms.make1f("shadowAngle");
   public static ShaderUniform1f uniform_rainStrength = shaderUniforms.make1f("rainStrength");
   public static ShaderUniform1f uniform_aspectRatio = shaderUniforms.make1f("aspectRatio");
   public static ShaderUniform1f uniform_viewWidth = shaderUniforms.make1f("viewWidth");
   public static ShaderUniform1f uniform_viewHeight = shaderUniforms.make1f("viewHeight");
   public static ShaderUniform1f uniform_near = shaderUniforms.make1f("near");
   public static ShaderUniform1f uniform_far = shaderUniforms.make1f("far");
   public static ShaderUniform3f uniform_sunPosition = shaderUniforms.make3f("sunPosition");
   public static ShaderUniform3f uniform_moonPosition = shaderUniforms.make3f("moonPosition");
   public static ShaderUniform3f uniform_shadowLightPosition = shaderUniforms.make3f("shadowLightPosition");
   public static ShaderUniform3f uniform_upPosition = shaderUniforms.make3f("upPosition");
   public static ShaderUniform3f uniform_previousCameraPosition = shaderUniforms.make3f("previousCameraPosition");
   public static ShaderUniform3f uniform_cameraPosition = shaderUniforms.make3f("cameraPosition");
   public static ShaderUniformM4 uniform_gbufferModelView = shaderUniforms.makeM4("gbufferModelView");
   public static ShaderUniformM4 uniform_gbufferModelViewInverse = shaderUniforms.makeM4("gbufferModelViewInverse");
   public static ShaderUniformM4 uniform_gbufferPreviousProjection = shaderUniforms.makeM4("gbufferPreviousProjection");
   public static ShaderUniformM4 uniform_gbufferProjection = shaderUniforms.makeM4("gbufferProjection");
   public static ShaderUniformM4 uniform_gbufferProjectionInverse = shaderUniforms.makeM4("gbufferProjectionInverse");
   public static ShaderUniformM4 uniform_gbufferPreviousModelView = shaderUniforms.makeM4("gbufferPreviousModelView");
   public static ShaderUniformM4 uniform_shadowProjection = shaderUniforms.makeM4("shadowProjection");
   public static ShaderUniformM4 uniform_shadowProjectionInverse = shaderUniforms.makeM4("shadowProjectionInverse");
   public static ShaderUniformM4 uniform_shadowModelView = shaderUniforms.makeM4("shadowModelView");
   public static ShaderUniformM4 uniform_shadowModelViewInverse = shaderUniforms.makeM4("shadowModelViewInverse");
   public static ShaderUniform1f uniform_wetness = shaderUniforms.make1f("wetness");
   public static ShaderUniform1f uniform_eyeAltitude = shaderUniforms.make1f("eyeAltitude");
   public static ShaderUniform2i uniform_eyeBrightness = shaderUniforms.make2i("eyeBrightness");
   public static ShaderUniform2i uniform_eyeBrightnessSmooth = shaderUniforms.make2i("eyeBrightnessSmooth");
   public static ShaderUniform2i uniform_terrainTextureSize = shaderUniforms.make2i("terrainTextureSize");
   public static ShaderUniform1i uniform_terrainIconSize = shaderUniforms.make1i("terrainIconSize");
   public static ShaderUniform1i uniform_isEyeInWater = shaderUniforms.make1i("isEyeInWater");
   public static ShaderUniform1f uniform_nightVision = shaderUniforms.make1f("nightVision");
   public static ShaderUniform1f uniform_blindness = shaderUniforms.make1f("blindness");
   public static ShaderUniform1f uniform_screenBrightness = shaderUniforms.make1f("screenBrightness");
   public static ShaderUniform1i uniform_hideGUI = shaderUniforms.make1i("hideGUI");
   public static ShaderUniform1f uniform_centerDepthSmooth = shaderUniforms.make1f("centerDepthSmooth");
   public static ShaderUniform2i uniform_atlasSize = shaderUniforms.make2i("atlasSize");
   public static ShaderUniform4i uniform_blendFunc = shaderUniforms.make4i("blendFunc");
   public static ShaderUniform1i uniform_instanceId = shaderUniforms.make1i("instanceId");
   static double previousCameraPositionX;
   static double previousCameraPositionY;
   static double previousCameraPositionZ;
   static double cameraPositionX;
   static double cameraPositionY;
   static double cameraPositionZ;
   static int cameraOffsetX;
   static int cameraOffsetZ;
   static int shadowPassInterval = 0;
   public static boolean needResizeShadow = false;
   static int shadowMapWidth = 1024;
   static int shadowMapHeight = 1024;
   static int spShadowMapWidth = 1024;
   static int spShadowMapHeight = 1024;
   static float shadowMapFOV = 90.0F;
   static float shadowMapHalfPlane = 160.0F;
   static boolean shadowMapIsOrtho = true;
   static float shadowDistanceRenderMul = -1.0F;
   static int shadowPassCounter = 0;
   static int preShadowPassThirdPersonView;
   public static boolean shouldSkipDefaultShadow = false;
   static boolean waterShadowEnabled = false;
   static final int MaxDrawBuffers = 8;
   static final int MaxColorBuffers = 8;
   static final int MaxDepthBuffers = 3;
   static final int MaxShadowColorBuffers = 8;
   static final int MaxShadowDepthBuffers = 2;
   static int usedColorBuffers = 0;
   static int usedDepthBuffers = 0;
   static int usedShadowColorBuffers = 0;
   static int usedShadowDepthBuffers = 0;
   static int usedColorAttachs = 0;
   static int usedDrawBuffers = 0;
   static int dfb = 0;
   static int sfb = 0;
   private static int[] gbuffersFormat = new int[8];
   public static boolean[] gbuffersClear = new boolean[8];
   public static Vector4f[] gbuffersClearColor = new Vector4f[8];
   private static Programs programs = new Programs();
   public static final Program ProgramNone = programs.getProgramNone();
   public static final Program ProgramShadow = programs.makeShadow("shadow", ProgramNone);
   public static final Program ProgramShadowSolid = programs.makeShadow("shadow_solid", ProgramShadow);
   public static final Program ProgramShadowCutout = programs.makeShadow("shadow_cutout", ProgramShadow);
   public static final Program ProgramBasic = programs.makeGbuffers("gbuffers_basic", ProgramNone);
   public static final Program ProgramTextured = programs.makeGbuffers("gbuffers_textured", ProgramBasic);
   public static final Program ProgramTexturedLit = programs.makeGbuffers("gbuffers_textured_lit", ProgramTextured);
   public static final Program ProgramSkyBasic = programs.makeGbuffers("gbuffers_skybasic", ProgramBasic);
   public static final Program ProgramSkyTextured = programs.makeGbuffers("gbuffers_skytextured", ProgramTextured);
   public static final Program ProgramClouds = programs.makeGbuffers("gbuffers_clouds", ProgramTextured);
   public static final Program ProgramTerrain = programs.makeGbuffers("gbuffers_terrain", ProgramTexturedLit);
   public static final Program ProgramTerrainSolid = programs.makeGbuffers("gbuffers_terrain_solid", ProgramTerrain);
   public static final Program ProgramTerrainCutoutMip = programs.makeGbuffers("gbuffers_terrain_cutout_mip", ProgramTerrain);
   public static final Program ProgramTerrainCutout = programs.makeGbuffers("gbuffers_terrain_cutout", ProgramTerrain);
   public static final Program ProgramDamagedBlock = programs.makeGbuffers("gbuffers_damagedblock", ProgramTerrain);
   public static final Program ProgramBlock = programs.makeGbuffers("gbuffers_block", ProgramTerrain);
   public static final Program ProgramBeaconBeam = programs.makeGbuffers("gbuffers_beaconbeam", ProgramTextured);
   public static final Program ProgramItem = programs.makeGbuffers("gbuffers_item", ProgramTexturedLit);
   public static final Program ProgramEntities = programs.makeGbuffers("gbuffers_entities", ProgramTexturedLit);
   public static final Program ProgramEntitiesGlowing = programs.makeGbuffers("gbuffers_entities_glowing", ProgramEntities);
   public static final Program ProgramArmorGlint = programs.makeGbuffers("gbuffers_armor_glint", ProgramTextured);
   public static final Program ProgramSpiderEyes = programs.makeGbuffers("gbuffers_spidereyes", ProgramTextured);
   public static final Program ProgramHand = programs.makeGbuffers("gbuffers_hand", ProgramTexturedLit);
   public static final Program ProgramWeather = programs.makeGbuffers("gbuffers_weather", ProgramTexturedLit);
   public static final Program ProgramDeferredPre = programs.makeVirtual("deferred_pre");
   public static final Program[] ProgramsDeferred = programs.makeDeferreds("deferred", 16);
   public static final Program ProgramDeferred = ProgramsDeferred[0];
   public static final Program ProgramWater = programs.makeGbuffers("gbuffers_water", ProgramTerrain);
   public static final Program ProgramHandWater = programs.makeGbuffers("gbuffers_hand_water", ProgramHand);
   public static final Program ProgramCompositePre = programs.makeVirtual("composite_pre");
   public static final Program[] ProgramsComposite = programs.makeComposites("composite", 16);
   public static final Program ProgramComposite = ProgramsComposite[0];
   public static final Program ProgramFinal = programs.makeComposite("final");
   public static final int ProgramCount = programs.getCount();
   public static final Program[] ProgramsAll = programs.getPrograms();
   public static Program activeProgram = ProgramNone;
   public static int activeProgramID = 0;
   private static ProgramStack programStack = new ProgramStack();
   private static boolean hasDeferredPrograms = false;
   static IntBuffer activeDrawBuffers = null;
   private static int activeCompositeMipmapSetting = 0;
   public static Properties loadedShaders = null;
   public static Properties shadersConfig = null;
   public static Texture defaultTexture = null;
   public static boolean[] shadowHardwareFilteringEnabled = new boolean[2];
   public static boolean[] shadowMipmapEnabled = new boolean[2];
   public static boolean[] shadowFilterNearest = new boolean[2];
   public static boolean[] shadowColorMipmapEnabled = new boolean[8];
   public static boolean[] shadowColorFilterNearest = new boolean[8];
   public static boolean configTweakBlockDamage = false;
   public static boolean configCloudShadow = false;
   public static float configHandDepthMul = 0.125F;
   public static float configRenderResMul = 1.0F;
   public static float configShadowResMul = 1.0F;
   public static int configTexMinFilB = 0;
   public static int configTexMinFilN = 0;
   public static int configTexMinFilS = 0;
   public static int configTexMagFilB = 0;
   public static int configTexMagFilN = 0;
   public static int configTexMagFilS = 0;
   public static boolean configShadowClipFrustrum = true;
   public static boolean configNormalMap = true;
   public static boolean configSpecularMap = true;
   public static PropertyDefaultTrueFalse configOldLighting = new PropertyDefaultTrueFalse("oldLighting", "Classic Lighting", 0);
   public static PropertyDefaultTrueFalse configOldHandLight = new PropertyDefaultTrueFalse("oldHandLight", "Old Hand Light", 0);
   public static int configAntialiasingLevel = 0;
   public static final int texMinFilRange = 3;
   public static final int texMagFilRange = 2;
   public static final String[] texMinFilDesc = new String[]{"Nearest", "Nearest-Nearest", "Nearest-Linear"};
   public static final String[] texMagFilDesc = new String[]{"Nearest", "Linear"};
   public static final int[] texMinFilValue = new int[]{9728, 9984, 9986};
   public static final int[] texMagFilValue = new int[]{9728, 9729};
   private static IShaderPack shaderPack = null;
   public static boolean shaderPackLoaded = false;
   public static String currentShaderName;
   public static final String SHADER_PACK_NAME_NONE = "OFF";
   public static final String SHADER_PACK_NAME_DEFAULT = "(internal)";
   public static final String SHADER_PACKS_DIR_NAME = "shaderpacks";
   public static final String OPTIONS_FILE_NAME = "optionsshaders.txt";
   public static final File shaderPacksDir = new File(MinecraftClient.getInstance().runDirectory, "shaderpacks");
   static File configFile = new File(MinecraftClient.getInstance().runDirectory, "optionsshaders.txt");
   private static ShaderOption[] shaderPackOptions = null;
   private static Set<String> shaderPackOptionSliders = null;
   static ShaderProfile[] shaderPackProfiles = null;
   static Map<String, ScreenShaderOptions> shaderPackGuiScreens = null;
   static Map<String, IExpressionBool> shaderPackProgramConditions = new HashMap<>();
   public static final String PATH_SHADERS_PROPERTIES = "/shaders/shaders.properties";
   public static PropertyDefaultFastFancyOff shaderPackClouds = new PropertyDefaultFastFancyOff("clouds", "Clouds", 0);
   public static PropertyDefaultTrueFalse shaderPackOldLighting = new PropertyDefaultTrueFalse("oldLighting", "Classic Lighting", 0);
   public static PropertyDefaultTrueFalse shaderPackOldHandLight = new PropertyDefaultTrueFalse("oldHandLight", "Old Hand Light", 0);
   public static PropertyDefaultTrueFalse shaderPackDynamicHandLight = new PropertyDefaultTrueFalse("dynamicHandLight", "Dynamic Hand Light", 0);
   public static PropertyDefaultTrueFalse shaderPackShadowTranslucent = new PropertyDefaultTrueFalse("shadowTranslucent", "Shadow Translucent", 0);
   public static PropertyDefaultTrueFalse shaderPackUnderwaterOverlay = new PropertyDefaultTrueFalse("underwaterOverlay", "Underwater Overlay", 0);
   public static PropertyDefaultTrueFalse shaderPackSun = new PropertyDefaultTrueFalse("sun", "Sun", 0);
   public static PropertyDefaultTrueFalse shaderPackMoon = new PropertyDefaultTrueFalse("moon", "Moon", 0);
   public static PropertyDefaultTrueFalse shaderPackVignette = new PropertyDefaultTrueFalse("vignette", "Vignette", 0);
   public static PropertyDefaultTrueFalse shaderPackBackFaceSolid = new PropertyDefaultTrueFalse("backFace.solid", "Back-face Solid", 0);
   public static PropertyDefaultTrueFalse shaderPackBackFaceCutout = new PropertyDefaultTrueFalse("backFace.cutout", "Back-face Cutout", 0);
   public static PropertyDefaultTrueFalse shaderPackBackFaceCutoutMipped = new PropertyDefaultTrueFalse("backFace.cutoutMipped", "Back-face Cutout Mipped", 0);
   public static PropertyDefaultTrueFalse shaderPackBackFaceTranslucent = new PropertyDefaultTrueFalse("backFace.translucent", "Back-face Translucent", 0);
   public static PropertyDefaultTrueFalse shaderPackRainDepth = new PropertyDefaultTrueFalse("rain.depth", "Rain Depth", 0);
   public static PropertyDefaultTrueFalse shaderPackBeaconBeamDepth = new PropertyDefaultTrueFalse("beacon.beam.depth", "Rain Depth", 0);
   public static PropertyDefaultTrueFalse shaderPackSeparateAo = new PropertyDefaultTrueFalse("separateAo", "Separate AO", 0);
   public static PropertyDefaultTrueFalse shaderPackFrustumCulling = new PropertyDefaultTrueFalse("frustum.culling", "Frustum Culling", 0);
   private static Map<String, String> shaderPackResources = new HashMap<>();
   private static World currentWorld = null;
   private static List<Integer> shaderPackDimensions = new ArrayList<>();
   private static ICustomTexture[] customTexturesGbuffers = null;
   private static ICustomTexture[] customTexturesComposite = null;
   private static ICustomTexture[] customTexturesDeferred = null;
   private static String noiseTexturePath = null;
   private static CustomUniforms customUniforms = null;
   private static final int STAGE_GBUFFERS = 0;
   private static final int STAGE_COMPOSITE = 1;
   private static final int STAGE_DEFERRED = 2;
   private static final String[] STAGE_NAMES = new String[]{"gbuffers", "composite", "deferred"};
   public static final boolean enableShadersOption = true;
   private static final boolean enableShadersDebug = true;
   public static final boolean saveFinalShaders = System.getProperty("shaders.debug.save", "false").equals("true");
   public static float blockLightLevel05 = 0.5F;
   public static float blockLightLevel06 = 0.6F;
   public static float blockLightLevel08 = 0.8F;
   public static float aoLevel = -1.0F;
   public static float sunPathRotation = 0.0F;
   public static float shadowAngleInterval = 0.0F;
   public static int fogMode = 0;
   public static float fogDensity = 0.0F;
   public static float fogColorR;
   public static float fogColorG;
   public static float fogColorB;
   public static float shadowIntervalSize = 2.0F;
   public static int terrainIconSize = 16;
   public static int[] terrainTextureSize = new int[2];
   private static ICustomTexture noiseTexture;
   private static boolean noiseTextureEnabled = false;
   private static int noiseTextureResolution = 256;
   static final int[] colorTextureImageUnit = new int[]{0, 1, 2, 3, 7, 8, 9, 10};
   private static final int bigBufferSize = (285 + 8 * ProgramCount) * 4;
   private static final ByteBuffer bigBuffer = (ByteBuffer)((Buffer)BufferUtils.createByteBuffer(bigBufferSize)).limit(0);
   static final float[] faProjection = new float[16];
   static final float[] faProjectionInverse = new float[16];
   static final float[] faModelView = new float[16];
   static final float[] faModelViewInverse = new float[16];
   static final float[] faShadowProjection = new float[16];
   static final float[] faShadowProjectionInverse = new float[16];
   static final float[] faShadowModelView = new float[16];
   static final float[] faShadowModelViewInverse = new float[16];
   static final FloatBuffer projection = nextFloatBuffer(16);
   static final FloatBuffer projectionInverse = nextFloatBuffer(16);
   static final FloatBuffer modelView = nextFloatBuffer(16);
   static final FloatBuffer modelViewInverse = nextFloatBuffer(16);
   static final FloatBuffer shadowProjection = nextFloatBuffer(16);
   static final FloatBuffer shadowProjectionInverse = nextFloatBuffer(16);
   static final FloatBuffer shadowModelView = nextFloatBuffer(16);
   static final FloatBuffer shadowModelViewInverse = nextFloatBuffer(16);
   static final FloatBuffer previousProjection = nextFloatBuffer(16);
   static final FloatBuffer previousModelView = nextFloatBuffer(16);
   static final FloatBuffer tempMatrixDirectBuffer = nextFloatBuffer(16);
   static final FloatBuffer tempDirectFloatBuffer = nextFloatBuffer(16);
   static final IntBuffer dfbColorTextures = nextIntBuffer(16);
   static final IntBuffer dfbDepthTextures = nextIntBuffer(3);
   static final IntBuffer sfbColorTextures = nextIntBuffer(8);
   static final IntBuffer sfbDepthTextures = nextIntBuffer(2);
   static final IntBuffer dfbDrawBuffers = nextIntBuffer(8);
   static final IntBuffer sfbDrawBuffers = nextIntBuffer(8);
   static final IntBuffer drawBuffersNone = (IntBuffer)((Buffer)nextIntBuffer(8)).limit(0);
   static final IntBuffer drawBuffersColorAtt0 = (IntBuffer)((Buffer)nextIntBuffer(8).put(36064)).position(0).limit(1);
   static final FlipTextures dfbColorTexturesFlip = new FlipTextures(dfbColorTextures, 8);
   static Map<Block, Integer> mapBlockToEntityData;
   private static final String[] formatNames = new String[]{
           "R8",
           "RG8",
           "RGB8",
           "RGBA8",
           "R8_SNORM",
           "RG8_SNORM",
           "RGB8_SNORM",
           "RGBA8_SNORM",
           "R16",
           "RG16",
           "RGB16",
           "RGBA16",
           "R16_SNORM",
           "RG16_SNORM",
           "RGB16_SNORM",
           "RGBA16_SNORM",
           "R16F",
           "RG16F",
           "RGB16F",
           "RGBA16F",
           "R32F",
           "RG32F",
           "RGB32F",
           "RGBA32F",
           "R32I",
           "RG32I",
           "RGB32I",
           "RGBA32I",
           "R32UI",
           "RG32UI",
           "RGB32UI",
           "RGBA32UI",
           "R3_G3_B2",
           "RGB5_A1",
           "RGB10_A2",
           "R11F_G11F_B10F",
           "RGB9_E5"
   };
   private static final int[] formatIds = new int[]{
           33321,
           33323,
           32849,
           32856,
           36756,
           36757,
           36758,
           36759,
           33322,
           33324,
           32852,
           32859,
           36760,
           36761,
           36762,
           36763,
           33325,
           33327,
           34843,
           34842,
           33326,
           33328,
           34837,
           34836,
           33333,
           33339,
           36227,
           36226,
           33334,
           33340,
           36209,
           36208,
           10768,
           32855,
           32857,
           35898,
           35901
   };
   private static final Pattern patternLoadEntityDataMap = Pattern.compile("\\s*([\\w:]+)\\s*=\\s*([-]?\\d+)\\s*");
   public static int[] entityData = new int[32];
   public static int entityDataIndex = 0;

   private Shaders() {
   }

   private static ByteBuffer nextByteBuffer(int size) {
      ByteBuffer buffer = bigBuffer;
      int pos = buffer.limit();
      ((Buffer)buffer).position(pos).limit(pos + size);
      return buffer.slice();
   }

   public static IntBuffer nextIntBuffer(int size) {
      ByteBuffer buffer = bigBuffer;
      int pos = buffer.limit();
      ((Buffer)buffer).position(pos).limit(pos + size * 4);
      return buffer.asIntBuffer();
   }

   private static FloatBuffer nextFloatBuffer(int size) {
      ByteBuffer buffer = bigBuffer;
      int pos = buffer.limit();
      ((Buffer)buffer).position(pos).limit(pos + size * 4);
      return buffer.asFloatBuffer();
   }

   private static IntBuffer[] nextIntBufferArray(int count, int size) {
      IntBuffer[] aib = new IntBuffer[count];

      for (int i = 0; i < count; i++) {
         aib[i] = nextIntBuffer(size);
      }

      return aib;
   }

   public static void loadConfig() {
      SMCLog.info("Load shaders configuration.");

      try {
         if (!shaderPacksDir.exists()) {
            shaderPacksDir.mkdir();
         }
      } catch (Exception var8) {
         SMCLog.severe("Failed to open the shaderpacks directory: " + shaderPacksDir);
      }

      shadersConfig = new PropertiesOrdered();
      shadersConfig.setProperty(EnumShaderOption.SHADER_PACK.getPropertyKey(), "");
      if (configFile.exists()) {
         try {
            FileReader reader = new FileReader(configFile);
            shadersConfig.load(reader);
            reader.close();
         } catch (Exception var7) {
         }
      }

      if (!configFile.exists()) {
         try {
            storeConfig();
         } catch (Exception var6) {
         }
      }

      EnumShaderOption[] ops = EnumShaderOption.values();

      for (int i = 0; i < ops.length; i++) {
         EnumShaderOption op = ops[i];
         String key = op.getPropertyKey();
         String def = op.getValueDefault();
         String val = shadersConfig.getProperty(key, def);
         setEnumShaderOption(op, val);
      }

      loadShaderPack();
   }

   private static void setEnumShaderOption(EnumShaderOption eso, String str) {
      if (str == null) {
         str = eso.getValueDefault();
      }

      switch (eso) {
         case ANTIALIASING:
            configAntialiasingLevel = Config.parseInt(str, 0);
            break;
         case NORMAL_MAP:
            configNormalMap = Config.parseBoolean(str, true);
            break;
         case SPECULAR_MAP:
            configSpecularMap = Config.parseBoolean(str, true);
            break;
         case RENDER_RES_MUL:
            configRenderResMul = Config.parseFloat(str, 1.0F);
            break;
         case SHADOW_RES_MUL:
            configShadowResMul = Config.parseFloat(str, 1.0F);
            break;
         case HAND_DEPTH_MUL:
            configHandDepthMul = Config.parseFloat(str, 0.125F);
            break;
         case CLOUD_SHADOW:
            configCloudShadow = Config.parseBoolean(str, true);
            break;
         case OLD_HAND_LIGHT:
            configOldHandLight.setPropertyValue(str);
            break;
         case OLD_LIGHTING:
            configOldLighting.setPropertyValue(str);
            break;
         case SHADER_PACK:
            currentShaderName = str;
            break;
         case TWEAK_BLOCK_DAMAGE:
            configTweakBlockDamage = Config.parseBoolean(str, true);
            break;
         case SHADOW_CLIP_FRUSTRUM:
            configShadowClipFrustrum = Config.parseBoolean(str, true);
            break;
         case TEX_MIN_FIL_B:
            configTexMinFilB = Config.parseInt(str, 0);
            break;
         case TEX_MIN_FIL_N:
            configTexMinFilN = Config.parseInt(str, 0);
            break;
         case TEX_MIN_FIL_S:
            configTexMinFilS = Config.parseInt(str, 0);
            break;
         case TEX_MAG_FIL_B:
            configTexMagFilB = Config.parseInt(str, 0);
            break;
         case TEX_MAG_FIL_N:
            configTexMagFilB = Config.parseInt(str, 0);
            break;
         case TEX_MAG_FIL_S:
            configTexMagFilB = Config.parseInt(str, 0);
            break;
         default:
            throw new IllegalArgumentException("Unknown option: " + eso);
      }
   }

   public static void storeConfig() {
      SMCLog.info("Save shaders configuration.");
      if (shadersConfig == null) {
         shadersConfig = new PropertiesOrdered();
      }

      EnumShaderOption[] ops = EnumShaderOption.values();

      for (int i = 0; i < ops.length; i++) {
         EnumShaderOption op = ops[i];
         String key = op.getPropertyKey();
         String val = getEnumShaderOption(op);
         shadersConfig.setProperty(key, val);
      }

      try {
         FileWriter writer = new FileWriter(configFile);
         shadersConfig.store(writer, null);
         writer.close();
      } catch (Exception var5) {
         SMCLog.severe("Error saving configuration: " + var5.getClass().getName() + ": " + var5.getMessage());
      }
   }

   public static String getEnumShaderOption(EnumShaderOption eso) {
      switch (eso) {
         case ANTIALIASING:
            return Integer.toString(configAntialiasingLevel);
         case NORMAL_MAP:
            return Boolean.toString(configNormalMap);
         case SPECULAR_MAP:
            return Boolean.toString(configSpecularMap);
         case RENDER_RES_MUL:
            return Float.toString(configRenderResMul);
         case SHADOW_RES_MUL:
            return Float.toString(configShadowResMul);
         case HAND_DEPTH_MUL:
            return Float.toString(configHandDepthMul);
         case CLOUD_SHADOW:
            return Boolean.toString(configCloudShadow);
         case OLD_HAND_LIGHT:
            return configOldHandLight.getPropertyValue();
         case OLD_LIGHTING:
            return configOldLighting.getPropertyValue();
         case SHADER_PACK:
            return currentShaderName;
         case TWEAK_BLOCK_DAMAGE:
            return Boolean.toString(configTweakBlockDamage);
         case SHADOW_CLIP_FRUSTRUM:
            return Boolean.toString(configShadowClipFrustrum);
         case TEX_MIN_FIL_B:
            return Integer.toString(configTexMinFilB);
         case TEX_MIN_FIL_N:
            return Integer.toString(configTexMinFilN);
         case TEX_MIN_FIL_S:
            return Integer.toString(configTexMinFilS);
         case TEX_MAG_FIL_B:
            return Integer.toString(configTexMagFilB);
         case TEX_MAG_FIL_N:
            return Integer.toString(configTexMagFilB);
         case TEX_MAG_FIL_S:
            return Integer.toString(configTexMagFilB);
         default:
            throw new IllegalArgumentException("Unknown option: " + eso);
      }
   }

   public static void setShaderPack(String par1name) {
      currentShaderName = par1name;
      shadersConfig.setProperty(EnumShaderOption.SHADER_PACK.getPropertyKey(), par1name);
      loadShaderPack();
   }

   public static void loadShaderPack() {
      boolean shaderPackLoadedPrev = shaderPackLoaded;
      boolean oldLightingPrev = isOldLighting();

      shaderPackLoaded = false;
      if (shaderPack != null) {
         shaderPack.close();
         shaderPack = null;
         shaderPackResources.clear();
         shaderPackDimensions.clear();
         shaderPackOptions = null;
         shaderPackOptionSliders = null;
         shaderPackProfiles = null;
         shaderPackGuiScreens = null;
         shaderPackProgramConditions.clear();
         shaderPackClouds.resetValue();
         shaderPackOldHandLight.resetValue();
         shaderPackDynamicHandLight.resetValue();
         shaderPackOldLighting.resetValue();
         resetCustomTextures();
         noiseTexturePath = null;
      }

      boolean shadersBlocked = false;

      String packName = shadersConfig.getProperty(EnumShaderOption.SHADER_PACK.getPropertyKey(), "(internal)");
      if (!shadersBlocked) {
         shaderPack = getShaderPack(packName);
         shaderPackLoaded = shaderPack != null;
      }

      if (shaderPackLoaded) {
         SMCLog.info("Loaded shaderpack: " + getShaderPackName());
      } else {
         SMCLog.info("No shaderpack loaded.");
         shaderPack = new ShaderPackNone();
      }

      if (saveFinalShaders) {
         clearDirectory(new File(shaderPacksDir, "debug"));
      }

      loadShaderPackResources();
      loadShaderPackDimensions();
      shaderPackOptions = loadShaderPackOptions();
      loadShaderPackProperties();
      boolean formatChanged = shaderPackLoaded != shaderPackLoadedPrev;
      boolean oldLightingChanged = isOldLighting() != oldLightingPrev;
      if (formatChanged || oldLightingChanged) {
         VertexFormatHooks.updateVertexFormats();

         updateBlockLightLevel();
      }

      if ((formatChanged || oldLightingChanged) && mc.getResourceManager() != null) {
         mc.reloadResourcesConcurrently();
      }
   }

   public static IShaderPack getShaderPack(String name) {
      if (name == null) {
         return null;
      } else {
         name = name.trim();
         if (name.isEmpty() || name.equals("OFF")) {
            return null;
         } else if (name.equals("(internal)")) {
            return new ShaderPackDefault();
         } else {
            try {
               File packFile = new File(shaderPacksDir, name);
               if (packFile.isDirectory()) {
                  return new ShaderPackFolder(name, packFile);
               } else {
                  return packFile.isFile() && name.toLowerCase().endsWith(".zip") ? new ShaderPackZip(name, packFile) : null;
               }
            } catch (Exception var2) {
               var2.printStackTrace();
               return null;
            }
         }
      }
   }

   public static IShaderPack getShaderPack() {
      return shaderPack;
   }

   private static void loadShaderPackDimensions() {
      shaderPackDimensions.clear();

      for (int i = -128; i <= 128; i++) {
         String worldDir = "/shaders/world" + i;
         if (shaderPack.hasDirectory(worldDir)) {
            shaderPackDimensions.add(i);
         }
      }

      if (shaderPackDimensions.size() > 0) {
         Integer[] ids = shaderPackDimensions.toArray(new Integer[shaderPackDimensions.size()]);
         Photon.LOGGER.debug("[Shaders] Worlds: " + Config.arrayToString((Object[])ids));
      }
   }

   private static void loadShaderPackProperties() {
      shaderPackClouds.resetValue();
      shaderPackOldHandLight.resetValue();
      shaderPackDynamicHandLight.resetValue();
      shaderPackOldLighting.resetValue();
      shaderPackShadowTranslucent.resetValue();
      shaderPackUnderwaterOverlay.resetValue();
      shaderPackSun.resetValue();
      shaderPackMoon.resetValue();
      shaderPackVignette.resetValue();
      shaderPackBackFaceSolid.resetValue();
      shaderPackBackFaceCutout.resetValue();
      shaderPackBackFaceCutoutMipped.resetValue();
      shaderPackBackFaceTranslucent.resetValue();
      shaderPackRainDepth.resetValue();
      shaderPackBeaconBeamDepth.resetValue();
      shaderPackSeparateAo.resetValue();
      shaderPackFrustumCulling.resetValue();
      BlockAliases.reset();
      ItemAliases.reset();
      EntityAliases.reset();
      customUniforms = null;

      for (int i = 0; i < ProgramsAll.length; i++) {
         Program p = ProgramsAll[i];
         p.resetProperties();
      }

      if (shaderPack != null) {
         BlockAliases.update(shaderPack);
         ItemAliases.update(shaderPack);
         EntityAliases.update(shaderPack);
         String path = "/shaders/shaders.properties";

         try {
            InputStream in = shaderPack.getResourceAsStream(path);
            if (in == null) {
               return;
            }

            in = MacroProcessor.process(in, path);
            Properties props = new PropertiesOrdered();
            props.load(in);
            in.close();
            shaderPackClouds.loadFrom(props);
            shaderPackOldHandLight.loadFrom(props);
            shaderPackDynamicHandLight.loadFrom(props);
            shaderPackOldLighting.loadFrom(props);
            shaderPackShadowTranslucent.loadFrom(props);
            shaderPackUnderwaterOverlay.loadFrom(props);
            shaderPackSun.loadFrom(props);
            shaderPackVignette.loadFrom(props);
            shaderPackMoon.loadFrom(props);
            shaderPackBackFaceSolid.loadFrom(props);
            shaderPackBackFaceCutout.loadFrom(props);
            shaderPackBackFaceCutoutMipped.loadFrom(props);
            shaderPackBackFaceTranslucent.loadFrom(props);
            shaderPackRainDepth.loadFrom(props);
            shaderPackBeaconBeamDepth.loadFrom(props);
            shaderPackSeparateAo.loadFrom(props);
            shaderPackFrustumCulling.loadFrom(props);
            shaderPackOptionSliders = ShaderPackParser.parseOptionSliders(props, shaderPackOptions);
            shaderPackProfiles = ShaderPackParser.parseProfiles(props, shaderPackOptions);
            shaderPackGuiScreens = ShaderPackParser.parseGuiScreens(props, shaderPackProfiles, shaderPackOptions);
            shaderPackProgramConditions = ShaderPackParser.parseProgramConditions(props, shaderPackOptions);
            customTexturesGbuffers = loadCustomTextures(props, 0);
            customTexturesComposite = loadCustomTextures(props, 1);
            customTexturesDeferred = loadCustomTextures(props, 2);
            noiseTexturePath = props.getProperty("texture.noise");
            if (noiseTexturePath != null) {
               noiseTextureEnabled = true;
            }

            customUniforms = ShaderPackParser.parseCustomUniforms(props);
            ShaderPackParser.parseAlphaStates(props);
            ShaderPackParser.parseBlendStates(props);
            ShaderPackParser.parseRenderScales(props);
            ShaderPackParser.parseBuffersFlip(props);
         } catch (IOException var3) {
            Photon.LOGGER.warn("[Shaders] Error reading: " + path);
         }
      }
   }

   private static ICustomTexture[] loadCustomTextures(Properties props, int stage) {
      String PREFIX_TEXTURE = "texture." + STAGE_NAMES[stage] + ".";
      String[] keys = props.keySet().toArray(new String[0]);
      List<ICustomTexture> list = new ArrayList<>();

      for (String key : keys) {
         if (key.startsWith(PREFIX_TEXTURE)) {
            String name = StrUtils.removePrefix(key, PREFIX_TEXTURE);
            name = StrUtils.removeSuffix(name, new String[]{".0", ".1", ".2", ".3", ".4", ".5", ".6", ".7", ".8", ".9"});
            String path = props.getProperty(key).trim();
            int index = getTextureIndex(stage, name);
            if (index < 0) {
               SMCLog.warning("Invalid texture name: " + key);
            } else {
               ICustomTexture ct = loadCustomTexture(index, path);
               if (ct != null) {
                  SMCLog.info("Custom texture: " + key + " = " + path);
                  list.add(ct);
               }
            }
         }
      }

      return list.size() <= 0 ? null : list.toArray(new ICustomTexture[list.size()]);
   }

   private static ICustomTexture loadCustomTexture(int textureUnit, String path) {
      if (path == null) {
         return null;
      } else {
         path = path.trim();
         if (path.indexOf(58) >= 0) {
            return loadCustomTextureLocation(textureUnit, path);
         } else {
            return path.indexOf(32) >= 0 ? loadCustomTextureRaw(textureUnit, path) : loadCustomTextureShaders(textureUnit, path);
         }
      }
   }

   private static ICustomTexture loadCustomTextureLocation(int textureUnit, String path) {
      String pathFull = path.trim();
      int variant = 0;
      if (pathFull.startsWith("minecraft:textures/")) {
         pathFull = StrUtils.addSuffixCheck(pathFull, ".png");
         if (pathFull.endsWith("_n.png")) {
            pathFull = StrUtils.replaceSuffix(pathFull, "_n.png", ".png");
            variant = 1;
         } else if (pathFull.endsWith("_s.png")) {
            pathFull = StrUtils.replaceSuffix(pathFull, "_s.png", ".png");
            variant = 2;
         }
      }

      Identifier loc = new Identifier(pathFull);
      return new CustomTextureLocation(textureUnit, loc, variant);
   }

   private static ICustomTexture loadCustomTextureRaw(int textureUnit, String line) {
      ConnectedParser cp = new ConnectedParser("Shaders");
      String[] parts = Config.tokenize(line, " ");
      Deque<String> params = new ArrayDeque<>(Arrays.asList(parts));
      String path = params.poll();
      TextureType type = (TextureType)cp.parseEnum(params.poll(), TextureType.values(), "texture type");
      if (type == null) {
         SMCLog.warning("Invalid raw texture type: " + line);
         return null;
      } else {
         InternalFormat internalFormat = (InternalFormat)cp.parseEnum(params.poll(), InternalFormat.values(), "internal format");
         if (internalFormat == null) {
            SMCLog.warning("Invalid raw texture internal format: " + line);
            return null;
         } else {
            int width = 0;
            int height = 0;
            int depth = 0;
            switch (type) {
               case TEXTURE_1D:
                  width = cp.parseInt(params.poll(), -1);
                  break;
               case TEXTURE_2D:
                  width = cp.parseInt(params.poll(), -1);
                  height = cp.parseInt(params.poll(), -1);
                  break;
               case TEXTURE_3D:
                  width = cp.parseInt(params.poll(), -1);
                  height = cp.parseInt(params.poll(), -1);
                  depth = cp.parseInt(params.poll(), -1);
                  break;
               case TEXTURE_RECTANGLE:
                  width = cp.parseInt(params.poll(), -1);
                  height = cp.parseInt(params.poll(), -1);
                  break;
               default:
                  SMCLog.warning("Invalid raw texture type: " + type);
                  return null;
            }

            if (width >= 0 && height >= 0 && depth >= 0) {
               PixelFormat pixelFormat = (PixelFormat)cp.parseEnum(params.poll(), PixelFormat.values(), "pixel format");
               if (pixelFormat == null) {
                  SMCLog.warning("Invalid raw texture pixel format: " + line);
                  return null;
               } else {
                  PixelType pixelType = (PixelType)cp.parseEnum(params.poll(), PixelType.values(), "pixel type");
                  if (pixelType == null) {
                     SMCLog.warning("Invalid raw texture pixel type: " + line);
                     return null;
                  } else if (!params.isEmpty()) {
                     SMCLog.warning("Invalid raw texture, too many parameters: " + line);
                     return null;
                  } else {
                     return loadCustomTextureRaw(textureUnit, line, path, type, internalFormat, width, height, depth, pixelFormat, pixelType);
                  }
               }
            } else {
               SMCLog.warning("Invalid raw texture size: " + line);
               return null;
            }
         }
      }
   }

   private static ICustomTexture loadCustomTextureRaw(
           int textureUnit,
           String line,
           String path,
           TextureType type,
           InternalFormat internalFormat,
           int width,
           int height,
           int depth,
           PixelFormat pixelFormat,
           PixelType pixelType
   ) {
      try {
         String pathFull = "shaders/" + StrUtils.removePrefix(path, "/");
         InputStream in = shaderPack.getResourceAsStream(pathFull);
         if (in == null) {
            SMCLog.warning("Raw texture not found: " + path);
            return null;
         } else {
            byte[] bytes = Config.readAll(in);
            IOUtils.closeQuietly(in);
            ByteBuffer bb = GlAllocationUtils.allocateByteBuffer(bytes.length);
            bb.put(bytes);
            ((Buffer)bb).flip();
            TextureResourceMetadata tms = SimpleShaderTexture.loadTextureMetadataSection(pathFull, new TextureResourceMetadata(true, true, new ArrayList()));
            return new CustomTextureRaw(
                    type, internalFormat, width, height, depth, pixelFormat, pixelType, bb, textureUnit, tms.method_5980(), tms.method_5981()
            );
         }
      } catch (IOException var16) {
         SMCLog.warning("Error loading raw texture: " + path);
         SMCLog.warning("" + var16.getClass().getName() + ": " + var16.getMessage());
         return null;
      }
   }

   private static ICustomTexture loadCustomTextureShaders(int textureUnit, String path) {
      path = path.trim();
      if (path.indexOf(46) < 0) {
         path = path + ".png";
      }

      try {
         String pathFull = "shaders/" + StrUtils.removePrefix(path, "/");
         InputStream in = shaderPack.getResourceAsStream(pathFull);
         if (in == null) {
            SMCLog.warning("Texture not found: " + path);
            return null;
         } else {
            IOUtils.closeQuietly(in);
            SimpleShaderTexture tex = new SimpleShaderTexture(pathFull);
            tex.load(mc.getResourceManager());
            return new CustomTexture(textureUnit, pathFull, tex);
         }
      } catch (IOException var6) {
         SMCLog.warning("Error loading texture: " + path);
         SMCLog.warning("" + var6.getClass().getName() + ": " + var6.getMessage());
         return null;
      }
   }

   private static int getTextureIndex(int stage, String name) {
      if (stage == 0) {
         if (name.equals("texture")) {
            return 0;
         }

         if (name.equals("lightmap")) {
            return 1;
         }

         if (name.equals("normals")) {
            return 2;
         }

         if (name.equals("specular")) {
            return 3;
         }

         if (name.equals("shadowtex0") || name.equals("watershadow")) {
            return 4;
         }

         if (name.equals("shadow")) {
            return waterShadowEnabled ? 5 : 4;
         }

         if (name.equals("shadowtex1")) {
            return 5;
         }

         if (name.equals("depthtex0")) {
            return 6;
         }

         if (name.equals("gaux1")) {
            return 7;
         }

         if (name.equals("gaux2")) {
            return 8;
         }

         if (name.equals("gaux3")) {
            return 9;
         }

         if (name.equals("gaux4")) {
            return 10;
         }

         if (name.equals("depthtex1")) {
            return 12;
         }

         if (name.equals("shadowcolor0") || name.equals("shadowcolor")) {
            return 13;
         }

         if (name.equals("shadowcolor1")) {
            return 14;
         }

         if (name.equals("noisetex")) {
            return 15;
         }
      }

      if (stage == 1 || stage == 2) {
         if (name.equals("colortex0") || name.equals("colortex0")) {
            return 0;
         }

         if (name.equals("colortex1") || name.equals("gdepth")) {
            return 1;
         }

         if (name.equals("colortex2") || name.equals("gnormal")) {
            return 2;
         }

         if (name.equals("colortex3") || name.equals("composite")) {
            return 3;
         }

         if (name.equals("shadowtex0") || name.equals("watershadow")) {
            return 4;
         }

         if (name.equals("shadow")) {
            return waterShadowEnabled ? 5 : 4;
         }

         if (name.equals("shadowtex1")) {
            return 5;
         }

         if (name.equals("depthtex0") || name.equals("gdepthtex")) {
            return 6;
         }

         if (name.equals("colortex4") || name.equals("gaux1")) {
            return 7;
         }

         if (name.equals("colortex5") || name.equals("gaux2")) {
            return 8;
         }

         if (name.equals("colortex6") || name.equals("gaux3")) {
            return 9;
         }

         if (name.equals("colortex7") || name.equals("gaux4")) {
            return 10;
         }

         if (name.equals("depthtex1")) {
            return 11;
         }

         if (name.equals("depthtex2")) {
            return 12;
         }

         if (name.equals("shadowcolor0") || name.equals("shadowcolor")) {
            return 13;
         }

         if (name.equals("shadowcolor1")) {
            return 14;
         }

         if (name.equals("noisetex")) {
            return 15;
         }
      }

      return -1;
   }

   private static void bindCustomTextures(ICustomTexture[] cts) {
      if (cts != null) {
         for (int i = 0; i < cts.length; i++) {
            ICustomTexture ct = cts[i];
            GlStateManager.activeTexture(33984 + ct.getTextureUnit());
            int texId = ct.getTextureId();
            int target = ct.getTarget();
            if (target == 3553) {
               GlStateManager.bindTexture(texId);
            } else {
               GL11.glBindTexture(target, texId);
            }
         }
      }
   }

   private static void resetCustomTextures() {
      deleteCustomTextures(customTexturesGbuffers);
      deleteCustomTextures(customTexturesComposite);
      deleteCustomTextures(customTexturesDeferred);
      customTexturesGbuffers = null;
      customTexturesComposite = null;
      customTexturesDeferred = null;
   }

   private static void deleteCustomTextures(ICustomTexture[] cts) {
      if (cts != null) {
         for (int i = 0; i < cts.length; i++) {
            ICustomTexture ct = cts[i];
            ct.deleteTexture();
         }
      }
   }

   public static ShaderOption[] getShaderPackOptions(String screenName) {
      ShaderOption[] ops = (ShaderOption[])shaderPackOptions.clone();
      if (shaderPackGuiScreens == null) {
         if (shaderPackProfiles != null) {
            ShaderOptionProfile optionProfile = new ShaderOptionProfile(shaderPackProfiles, ops);
            ops = (ShaderOption[])Config.addObjectToArray(ops, optionProfile, 0);
         }

         return getVisibleOptions(ops);
      } else {
         String key = screenName != null ? "screen." + screenName : "screen";
         ScreenShaderOptions sso = shaderPackGuiScreens.get(key);
         if (sso == null) {
            return new ShaderOption[0];
         } else {
            ShaderOption[] sos = sso.getShaderOptions();
            List<ShaderOption> list = new ArrayList<>();

            for (int i = 0; i < sos.length; i++) {
               ShaderOption so = sos[i];
               if (so == null) {
                  list.add(null);
               } else if (so instanceof ShaderOptionRest) {
                  ShaderOption[] restOps = getShaderOptionsRest(shaderPackGuiScreens, ops);
                  list.addAll(Arrays.asList(restOps));
               } else {
                  list.add(so);
               }
            }

            return list.toArray(new ShaderOption[list.size()]);
         }
      }
   }

   public static int getShaderPackColumns(String screenName, int def) {
      String key = screenName != null ? "screen." + screenName : "screen";
      if (shaderPackGuiScreens == null) {
         return def;
      } else {
         ScreenShaderOptions sso = shaderPackGuiScreens.get(key);
         return sso == null ? def : sso.getColumns();
      }
   }

   private static ShaderOption[] getShaderOptionsRest(Map<String, ScreenShaderOptions> mapScreens, ShaderOption[] ops) {
      Set<String> setNames = new HashSet<>();

      for (String key : mapScreens.keySet()) {
         ScreenShaderOptions sso = mapScreens.get(key);
         ShaderOption[] sos = sso.getShaderOptions();

         for (int v = 0; v < sos.length; v++) {
            ShaderOption so = sos[v];
            if (so != null) {
               setNames.add(so.getName());
            }
         }
      }

      List<ShaderOption> list = new ArrayList<>();

      for (int i = 0; i < ops.length; i++) {
         ShaderOption so = ops[i];
         if (so.isVisible()) {
            String name = so.getName();
            if (!setNames.contains(name)) {
               list.add(so);
            }
         }
      }

      return list.toArray(new ShaderOption[list.size()]);
   }

   public static ShaderOption getShaderOption(String name) {
      return ShaderUtils.getShaderOption(name, shaderPackOptions);
   }

   public static ShaderOption[] getShaderPackOptions() {
      return shaderPackOptions;
   }

   public static boolean isShaderPackOptionSlider(String name) {
      return shaderPackOptionSliders == null ? false : shaderPackOptionSliders.contains(name);
   }

   private static ShaderOption[] getVisibleOptions(ShaderOption[] ops) {
      List<ShaderOption> list = new ArrayList<>();

      for (int i = 0; i < ops.length; i++) {
         ShaderOption so = ops[i];
         if (so.isVisible()) {
            list.add(so);
         }
      }

      return list.toArray(new ShaderOption[list.size()]);
   }

   public static void saveShaderPackOptions() {
      saveShaderPackOptions(shaderPackOptions, shaderPack);
   }

   private static void saveShaderPackOptions(ShaderOption[] sos, IShaderPack sp) {
      Properties props = new PropertiesOrdered();
      if (shaderPackOptions != null) {
         for (int i = 0; i < sos.length; i++) {
            ShaderOption so = sos[i];
            if (so.isChanged() && so.isEnabled()) {
               props.setProperty(so.getName(), so.getValue());
            }
         }
      }

      try {
         saveOptionProperties(sp, props);
      } catch (IOException var5) {
         Photon.LOGGER.warn("[Shaders] Error saving configuration for " + shaderPack.getName());
         var5.printStackTrace();
      }
   }

   private static void saveOptionProperties(IShaderPack sp, Properties props) throws IOException {
      String path = "shaderpacks/" + sp.getName() + ".txt";
      File propFile = new File(MinecraftClient.getInstance().runDirectory, path);
      if (props.isEmpty()) {
         propFile.delete();
      } else {
         FileOutputStream fos = new FileOutputStream(propFile);
         props.store(fos, null);
         fos.flush();
         fos.close();
      }
   }

   private static ShaderOption[] loadShaderPackOptions() {
      try {
         String[] programNames = programs.getProgramNames();
         ShaderOption[] sos = ShaderPackParser.parseShaderPackOptions(shaderPack, programNames, shaderPackDimensions);
         Properties props = loadOptionProperties(shaderPack);

         for (int i = 0; i < sos.length; i++) {
            ShaderOption so = sos[i];
            String val = props.getProperty(so.getName());
            if (val != null) {
               so.resetValue();
               if (!so.setValue(val)) {
                  Photon.LOGGER.warn("[Shaders] Invalid value, option: " + so.getName() + ", value: " + val);
               }
            }
         }

         return sos;
      } catch (IOException var6) {
         Photon.LOGGER.warn("[Shaders] Error reading configuration for " + shaderPack.getName());
         var6.printStackTrace();
         return null;
      }
   }

   private static Properties loadOptionProperties(IShaderPack sp) throws IOException {
      Properties props = new PropertiesOrdered();
      String path = "shaderpacks/" + sp.getName() + ".txt";
      File propFile = new File(MinecraftClient.getInstance().runDirectory, path);
      if (propFile.exists() && propFile.isFile() && propFile.canRead()) {
         FileInputStream fis = new FileInputStream(propFile);
         props.load(fis);
         fis.close();
         return props;
      } else {
         return props;
      }
   }

   public static ShaderOption[] getChangedOptions(ShaderOption[] ops) {
      List<ShaderOption> list = new ArrayList<>();

      for (int i = 0; i < ops.length; i++) {
         ShaderOption op = ops[i];
         if (op.isEnabled() && op.isChanged()) {
            list.add(op);
         }
      }

      return list.toArray(new ShaderOption[list.size()]);
   }

   private static String applyOptions(String line, ShaderOption[] ops) {
      if (ops != null && ops.length > 0) {
         for (int i = 0; i < ops.length; i++) {
            ShaderOption op = ops[i];
            if (op.matchesLine(line)) {
               line = op.getSourceLine();
               break;
            }
         }

         return line;
      } else {
         return line;
      }
   }

   public static ArrayList listOfShaders() {
      ArrayList<String> list = new ArrayList<>();
      list.add("OFF");
      list.add("(internal)");
      int countFixed = list.size();

      try {
         if (!shaderPacksDir.exists()) {
            shaderPacksDir.mkdir();
         }

         File[] listOfFiles = shaderPacksDir.listFiles();

         for (int i = 0; i < listOfFiles.length; i++) {
            File file = listOfFiles[i];
            String name = file.getName();
            if (file.isDirectory()) {
               if (!name.equals("debug")) {
                  File subDir = new File(file, "shaders");
                  if (subDir.exists() && subDir.isDirectory()) {
                     list.add(name);
                  }
               }
            } else if (file.isFile() && name.toLowerCase().endsWith(".zip")) {
               list.add(name);
            }
         }
      } catch (Exception var7) {
      }

      List<String> sortList = list.subList(countFixed, list.size());
      Collections.sort(sortList, String.CASE_INSENSITIVE_ORDER);
      return list;
   }

   public static int checkFramebufferStatus(String location) {
      int status = EXTFramebufferObject.glCheckFramebufferStatusEXT(36160);
      if (status != 36053) {
         System.err.format("FramebufferStatus 0x%04X at %s\n", status, location);
      }

      return status;
   }

   public static int checkGLError(String location) {
      int errorCode = GL11.glGetError();
      if (errorCode != 0) {
         String errorText = Config.getGlErrorString(errorCode);
         String shadersInfo = getErrorInfo(errorCode, location);
         String messageLog = String.format("OpenGL error: %s (%s)%s, at: %s", errorCode, errorText, shadersInfo, location);
         SMCLog.severe(messageLog);
            String messageChat = I18n.translate("of.message.openglError", new Object[]{errorCode, errorText});
            printChat(messageChat);

      }

      return errorCode;
   }

   private static String getErrorInfo(int errorCode, String location) {
      StringBuilder sb = new StringBuilder();
      if (errorCode == 1286) {
         int statusCode = EXTFramebufferObject.glCheckFramebufferStatusEXT(36160);
         String statusText = getFramebufferStatusText(statusCode);
         String info = ", fbStatus: " + statusCode + " (" + statusText + ")";
         sb.append(info);
      }

      String programName = activeProgram.getName();
      if (programName.isEmpty()) {
         programName = "none";
      }

      sb.append(", program: " + programName);
      Program activeProgramReal = getProgramById(activeProgramID);
      if (activeProgramReal != activeProgram) {
         String programRealName = activeProgramReal.getName();
         if (programRealName.isEmpty()) {
            programRealName = "none";
         }

         sb.append(" (" + programRealName + ")");
      }

      if (location.equals("setDrawBuffers")) {
         sb.append(", drawBuffers: " + activeProgram.getDrawBufSettings());
      }

      return sb.toString();
   }

   private static Program getProgramById(int programID) {
      for (int i = 0; i < ProgramsAll.length; i++) {
         Program pi = ProgramsAll[i];
         if (pi.getId() == programID) {
            return pi;
         }
      }

      return ProgramNone;
   }

   private static String getFramebufferStatusText(int fbStatusCode) {
      switch (fbStatusCode) {
         case 33305:
            return "Undefined";
         case 36053:
            return "Complete";
         case 36054:
            return "Incomplete attachment";
         case 36055:
            return "Incomplete missing attachment";
         case 36059:
            return "Incomplete draw buffer";
         case 36060:
            return "Incomplete read buffer";
         case 36061:
            return "Unsupported";
         case 36182:
            return "Incomplete multisample";
         case 36264:
            return "Incomplete layer targets";
         default:
            return "Unknown";
      }
   }

   private static void printChat(String str) {
      mc.inGameHud.getChatHud().addMessage(new LiteralText(str));
   }

   private static void printChatAndLogError(String str) {
      SMCLog.severe(str);
      mc.inGameHud.getChatHud().addMessage(new LiteralText(str));
   }

   public static void printIntBuffer(String title, IntBuffer buf) {
      StringBuilder sb = new StringBuilder(128);
      sb.append(title).append(" [pos ").append(buf.position()).append(" lim ").append(buf.limit()).append(" cap ").append(buf.capacity()).append(" :");
      int lim = buf.limit();

      for (int i = 0; i < lim; i++) {
         sb.append(" ").append(buf.get(i));
      }

      sb.append("]");
      SMCLog.info(sb.toString());
   }

   public static void startup(MinecraftClient mc) {
      checkShadersModInstalled();
      Shaders.mc = mc;
      mc = MinecraftClient.getInstance();
      capabilities = GLContext.getCapabilities();
      glVersionString = GL11.glGetString(7938);
      glVendorString = GL11.glGetString(7936);
      glRendererString = GL11.glGetString(7937);
      SMCLog.info("OpenGL Version: " + glVersionString);
      SMCLog.info("Vendor:  " + glVendorString);
      SMCLog.info("Renderer: " + glRendererString);
      SMCLog.info(
              "Capabilities: "
                      + (capabilities.OpenGL20 ? " 2.0 " : " - ")
                      + (capabilities.OpenGL21 ? " 2.1 " : " - ")
                      + (capabilities.OpenGL30 ? " 3.0 " : " - ")
                      + (capabilities.OpenGL32 ? " 3.2 " : " - ")
                      + (capabilities.OpenGL40 ? " 4.0 " : " - ")
      );
      SMCLog.info("GL_MAX_DRAW_BUFFERS: " + GL11.glGetInteger(34852));
      SMCLog.info("GL_MAX_COLOR_ATTACHMENTS_EXT: " + GL11.glGetInteger(36063));
      SMCLog.info("GL_MAX_TEXTURE_IMAGE_UNITS: " + GL11.glGetInteger(34930));
      hasGlGenMipmap = capabilities.OpenGL30;
      loadConfig();
   }

   public static void updateBlockLightLevel() {
      if (isOldLighting()) {
         blockLightLevel05 = 0.5F;
         blockLightLevel06 = 0.6F;
         blockLightLevel08 = 0.8F;
      } else {
         blockLightLevel05 = 1.0F;
         blockLightLevel06 = 1.0F;
         blockLightLevel08 = 1.0F;
      }
   }

   public static boolean isOldHandLight() {
      if (!configOldHandLight.isDefault()) {
         return configOldHandLight.isTrue();
      } else {
         return !shaderPackOldHandLight.isDefault() ? shaderPackOldHandLight.isTrue() : true;
      }
   }

   public static boolean isDynamicHandLight() {
      return !shaderPackDynamicHandLight.isDefault() ? shaderPackDynamicHandLight.isTrue() : true;
   }

   public static boolean isOldLighting() {
      if (!configOldLighting.isDefault()) {
         return configOldLighting.isTrue();
      } else {
         return !shaderPackOldLighting.isDefault() ? shaderPackOldLighting.isTrue() : true;
      }
   }

   public static boolean isRenderShadowTranslucent() {
      return !shaderPackShadowTranslucent.isFalse();
   }

   public static boolean isUnderwaterOverlay() {
      return !shaderPackUnderwaterOverlay.isFalse();
   }

   public static boolean isSun() {
      return !shaderPackSun.isFalse();
   }

   public static boolean isMoon() {
      return !shaderPackMoon.isFalse();
   }

   public static boolean isVignette() {
      return !shaderPackVignette.isFalse();
   }

   public static boolean isRenderBackFace(RenderLayer blockLayerIn) {
      switch (blockLayerIn) {
         case SOLID:
            return shaderPackBackFaceSolid.isTrue();
         case CUTOUT:
            return shaderPackBackFaceCutout.isTrue();
         case CUTOUT_MIPPED:
            return shaderPackBackFaceCutoutMipped.isTrue();
         case TRANSLUCENT:
            return shaderPackBackFaceTranslucent.isTrue();
         default:
            return false;
      }
   }

   public static boolean isRainDepth() {
      return shaderPackRainDepth.isTrue();
   }

   public static boolean isBeaconBeamDepth() {
      return shaderPackBeaconBeamDepth.isTrue();
   }

   public static boolean isSeparateAo() {
      return shaderPackSeparateAo.isTrue();
   }

   public static boolean isFrustumCulling() {
      return !shaderPackFrustumCulling.isFalse();
   }

   public static void init() {
      boolean firstInit;
      if (!isInitializedOnce) {
         isInitializedOnce = true;
         firstInit = true;
      } else {
         firstInit = false;
      }

      if (!isShaderPackInitialized) {
         checkGLError("Shaders.init pre");
         if (getShaderPackName() != null) {
         }

         if (!capabilities.OpenGL20) {
            printChatAndLogError("No OpenGL 2.0");
         }

         if (!capabilities.GL_EXT_framebuffer_object) {
            printChatAndLogError("No EXT_framebuffer_object");
         }

         ((Buffer)dfbDrawBuffers).position(0).limit(8);
         ((Buffer)dfbColorTextures).position(0).limit(16);
         ((Buffer)dfbDepthTextures).position(0).limit(3);
         ((Buffer)sfbDrawBuffers).position(0).limit(8);
         ((Buffer)sfbDepthTextures).position(0).limit(2);
         ((Buffer)sfbColorTextures).position(0).limit(8);
         usedColorBuffers = 4;
         usedDepthBuffers = 1;
         usedShadowColorBuffers = 0;
         usedShadowDepthBuffers = 0;
         usedColorAttachs = 1;
         usedDrawBuffers = 1;
         Arrays.fill(gbuffersFormat, 6408);
         Arrays.fill(gbuffersClear, true);
         Arrays.fill(gbuffersClearColor, null);
         Arrays.fill(shadowHardwareFilteringEnabled, false);
         Arrays.fill(shadowMipmapEnabled, false);
         Arrays.fill(shadowFilterNearest, false);
         Arrays.fill(shadowColorMipmapEnabled, false);
         Arrays.fill(shadowColorFilterNearest, false);
         centerDepthSmoothEnabled = false;
         noiseTextureEnabled = false;
         sunPathRotation = 0.0F;
         shadowIntervalSize = 2.0F;
         shadowMapWidth = 1024;
         shadowMapHeight = 1024;
         spShadowMapWidth = 1024;
         spShadowMapHeight = 1024;
         shadowMapFOV = 90.0F;
         shadowMapHalfPlane = 160.0F;
         shadowMapIsOrtho = true;
         shadowDistanceRenderMul = -1.0F;
         aoLevel = -1.0F;
         useEntityAttrib = false;
         useMidTexCoordAttrib = false;
         useTangentAttrib = false;
         waterShadowEnabled = false;
         hasGeometryShaders = false;
         updateBlockLightLevel();
         Smoother.resetValues();
         shaderUniforms.reset();
         if (customUniforms != null) {
            customUniforms.reset();
         }

         ShaderProfile activeProfile = ShaderUtils.detectProfile(shaderPackProfiles, shaderPackOptions, false);
         String worldPrefix = "";
         if (currentWorld != null) {
            int dimId = currentWorld.dimension.getType();
            if (shaderPackDimensions.contains(dimId)) {
               worldPrefix = "world" + dimId + "/";
            }
         }

         for (int i = 0; i < ProgramsAll.length; i++) {
            Program p = ProgramsAll[i];
            p.resetId();
            p.resetConfiguration();
            if (p.getProgramStage() != ProgramStage.NONE) {
               String programName = p.getName();
               String programPath = worldPrefix + programName;
               boolean enabled = true;
               if (shaderPackProgramConditions.containsKey(programPath)) {
                  enabled = enabled && shaderPackProgramConditions.get(programPath).eval();
               }

               if (activeProfile != null) {
                  enabled = enabled && !activeProfile.isProgramDisabled(programPath);
               }

               if (!enabled) {
                  SMCLog.info("Program disabled: " + programPath);
                  programName = "<disabled>";
                  programPath = worldPrefix + programName;
               }

               String programFullPath = "/shaders/" + programPath;
               String programFullPathVertex = programFullPath + ".vsh";
               String programFullPathGeometry = programFullPath + ".gsh";
               String programFullPathFragment = programFullPath + ".fsh";
               setupProgram(p, programFullPathVertex, programFullPathGeometry, programFullPathFragment);
               int pr = p.getId();
               if (pr > 0) {
                  SMCLog.info("Program loaded: " + programPath);
               }

               initDrawBuffers(p);
               updateToggleBuffers(p);
            }
         }

         hasDeferredPrograms = false;

         for (int cp = 0; cp < ProgramsDeferred.length; cp++) {
            if (ProgramsDeferred[cp].getId() != 0) {
               hasDeferredPrograms = true;
               break;
            }
         }

         usedColorAttachs = usedColorBuffers;
         shadowPassInterval = usedShadowDepthBuffers > 0 ? 1 : 0;
         shouldSkipDefaultShadow = usedShadowDepthBuffers > 0;
         SMCLog.info("usedColorBuffers: " + usedColorBuffers);
         SMCLog.info("usedDepthBuffers: " + usedDepthBuffers);
         SMCLog.info("usedShadowColorBuffers: " + usedShadowColorBuffers);
         SMCLog.info("usedShadowDepthBuffers: " + usedShadowDepthBuffers);
         SMCLog.info("usedColorAttachs: " + usedColorAttachs);
         SMCLog.info("usedDrawBuffers: " + usedDrawBuffers);
         ((Buffer)dfbDrawBuffers).position(0).limit(usedDrawBuffers);
         ((Buffer)dfbColorTextures).position(0).limit(usedColorBuffers * 2);
         dfbColorTexturesFlip.reset();

         for (int ix = 0; ix < usedDrawBuffers; ix++) {
            dfbDrawBuffers.put(ix, 36064 + ix);
         }

         int maxDrawBuffers = GL11.glGetInteger(34852);
         if (usedDrawBuffers > maxDrawBuffers) {
            printChatAndLogError("[Shaders] Error: Not enough draw buffers, needed: " + usedDrawBuffers + ", available: " + maxDrawBuffers);
         }

         ((Buffer)sfbDrawBuffers).position(0).limit(usedShadowColorBuffers);

         for (int ix = 0; ix < usedShadowColorBuffers; ix++) {
            sfbDrawBuffers.put(ix, 36064 + ix);
         }

         for (int ix = 0; ix < ProgramsAll.length; ix++) {
            Program pi = ProgramsAll[ix];
            Program pn = pi;

            while (pn.getId() == 0 && pn.getProgramBackup() != pn) {
               pn = pn.getProgramBackup();
            }

            if (pn != pi && pi != ProgramShadow) {
               pi.copyFrom(pn);
            }
         }

         resize();
         resizeShadow();
         if (noiseTextureEnabled) {
            setupNoiseTexture();
         }

         if (defaultTexture == null) {
            defaultTexture = ShadersTex.createDefaultTexture();
         }

         GlStateManager.pushMatrix();
         GlStateManager.rotate(-90.0F, 0.0F, 1.0F, 0.0F);
         preCelestialRotate();
         postCelestialRotate();
         GlStateManager.popMatrix();
         isShaderPackInitialized = true;
         loadEntityDataMap();
         resetDisplayLists();
         if (!firstInit) {
         }

         checkGLError("Shaders.init");
      }
   }

   private static void initDrawBuffers(Program p) {
      int maxDrawBuffers = GL11.glGetInteger(34852);
      Arrays.fill(p.getToggleColorTextures(), false);
      if (p == ProgramFinal) {
         p.setDrawBuffers(null);
      } else if (p.getId() == 0) {
         if (p == ProgramShadow) {
            p.setDrawBuffers(drawBuffersNone);
         } else {
            p.setDrawBuffers(drawBuffersColorAtt0);
         }
      } else {
         String str = p.getDrawBufSettings();
         if (str == null) {
            if (p != ProgramShadow && p != ProgramShadowSolid && p != ProgramShadowCutout) {
               p.setDrawBuffers(dfbDrawBuffers);
               usedDrawBuffers = usedColorBuffers;
               Arrays.fill(p.getToggleColorTextures(), 0, usedColorBuffers, true);
            } else {
               p.setDrawBuffers(sfbDrawBuffers);
            }
         } else {
            IntBuffer intbuf = p.getDrawBuffersBuffer();
            int numDB = str.length();
            usedDrawBuffers = Math.max(usedDrawBuffers, numDB);
            numDB = Math.min(numDB, maxDrawBuffers);
            p.setDrawBuffers(intbuf);
            ((Buffer)intbuf).limit(numDB);

            for (int i = 0; i < numDB; i++) {
               int drawBuffer = getDrawBuffer(p, str, i);
               intbuf.put(i, drawBuffer);
            }
         }
      }
   }

   private static int getDrawBuffer(Program p, String str, int i) {
      int drawBuffer = 0;
      if (i >= str.length()) {
         return drawBuffer;
      } else {
         int ca = str.charAt(i) - '0';
         if (p == ProgramShadow) {
            if (ca >= 0 && ca <= 1) {
               drawBuffer = ca + 36064;
               usedShadowColorBuffers = Math.max(usedShadowColorBuffers, ca);
            }

            return drawBuffer;
         } else {
            if (ca >= 0 && ca <= 7) {
               p.getToggleColorTextures()[ca] = true;
               drawBuffer = ca + 36064;
               usedColorAttachs = Math.max(usedColorAttachs, ca);
               usedColorBuffers = Math.max(usedColorBuffers, ca);
            }

            return drawBuffer;
         }
      }
   }

   private static void updateToggleBuffers(Program p) {
      boolean[] toggleBuffers = p.getToggleColorTextures();
      Boolean[] flipBuffers = p.getBuffersFlip();

      for (int i = 0; i < flipBuffers.length; i++) {
         Boolean flip = flipBuffers[i];
         if (flip != null) {
            toggleBuffers[i] = flip;
         }
      }
   }

   public static void resetDisplayLists() {
      SMCLog.info("Reset model renderers");
      countResetDisplayLists++;
      SMCLog.info("Reset world renderers");
      mc.worldRenderer.reload();
   }

   private static void setupProgram(Program program, String vShaderPath, String gShaderPath, String fShaderPath) {
      checkGLError("pre setupProgram");
      int programid = ARBShaderObjects.glCreateProgramObjectARB();
      checkGLError("create");
      if (programid != 0) {
         progUseEntityAttrib = false;
         progUseMidTexCoordAttrib = false;
         progUseTangentAttrib = false;
         int vShader = createVertShader(program, vShaderPath);
         int gShader = createGeomShader(program, gShaderPath);
         int fShader = createFragShader(program, fShaderPath);
         checkGLError("create");
         if (vShader == 0 && gShader == 0 && fShader == 0) {
            ARBShaderObjects.glDeleteObjectARB(programid);
            program.resetId();
         } else {
            if (vShader != 0) {
               ARBShaderObjects.glAttachObjectARB(programid, vShader);
               checkGLError("attach");
            }

            if (gShader != 0) {
               ARBShaderObjects.glAttachObjectARB(programid, gShader);
               checkGLError("attach");
               if (progArbGeometryShader4) {
                  ARBGeometryShader4.glProgramParameteriARB(programid, 36315, 4);
                  ARBGeometryShader4.glProgramParameteriARB(programid, 36316, 5);
                  ARBGeometryShader4.glProgramParameteriARB(programid, 36314, progMaxVerticesOut);
                  checkGLError("arbGeometryShader4");
               }

               hasGeometryShaders = true;
            }

            if (fShader != 0) {
               ARBShaderObjects.glAttachObjectARB(programid, fShader);
               checkGLError("attach");
            }

            if (progUseEntityAttrib) {
               ARBVertexShader.glBindAttribLocationARB(programid, entityAttrib, "mc_Entity");
               checkGLError("mc_Entity");
            }

            if (progUseMidTexCoordAttrib) {
               ARBVertexShader.glBindAttribLocationARB(programid, midTexCoordAttrib, "mc_midTexCoord");
               checkGLError("mc_midTexCoord");
            }

            if (progUseTangentAttrib) {
               ARBVertexShader.glBindAttribLocationARB(programid, tangentAttrib, "at_tangent");
               checkGLError("at_tangent");
            }

            ARBShaderObjects.glLinkProgramARB(programid);
            if (GL20.glGetProgrami(programid, 35714) != 1) {
               SMCLog.severe("Error linking program: " + programid + " (" + program.getName() + ")");
            }

            printLogInfo(programid, program.getName());
            if (vShader != 0) {
               ARBShaderObjects.glDetachObjectARB(programid, vShader);
               ARBShaderObjects.glDeleteObjectARB(vShader);
            }

            if (gShader != 0) {
               ARBShaderObjects.glDetachObjectARB(programid, gShader);
               ARBShaderObjects.glDeleteObjectARB(gShader);
            }

            if (fShader != 0) {
               ARBShaderObjects.glDetachObjectARB(programid, fShader);
               ARBShaderObjects.glDeleteObjectARB(fShader);
            }

            program.setId(programid);
            program.setRef(programid);
            useProgram(program);
            ARBShaderObjects.glValidateProgramARB(programid);
            useProgram(ProgramNone);
            printLogInfo(programid, program.getName());
            int valid = GL20.glGetProgrami(programid, 35715);
            if (valid != 1) {
               String Q = "\"";
               printChatAndLogError("[Shaders] Error: Invalid program " + Q + program.getName() + Q);
               ARBShaderObjects.glDeleteObjectARB(programid);
               program.resetId();
            }
         }
      }
   }

   private static int createVertShader(Program program, String filename) {
      int vertShader = ARBShaderObjects.glCreateShaderObjectARB(35633);
      if (vertShader == 0) {
         return 0;
      } else {
         StringBuilder vertexCode = new StringBuilder(131072);
         BufferedReader reader = null;

         try {
            reader = new BufferedReader(getShaderReader(filename));
         } catch (Exception var10) {
            ARBShaderObjects.glDeleteObjectARB(vertShader);
            return 0;
         }

         ShaderOption[] activeOptions = getChangedOptions(shaderPackOptions);
         List<String> listFiles = new ArrayList<>();
         if (reader != null) {
            try {
               reader = ShaderPackParser.resolveIncludes(reader, filename, shaderPack, 0, listFiles, 0);
               MacroState macroState = new MacroState();

               while (true) {
                  String line = reader.readLine();
                  if (line == null) {
                     reader.close();
                     break;
                  }

                  line = applyOptions(line, activeOptions);
                  vertexCode.append(line).append('\n');
                  if (macroState.processLine(line)) {
                     ShaderLine sl = ShaderParser.parseLine(line);
                     if (sl != null) {
                        if (sl.isAttribute("mc_Entity")) {
                           useEntityAttrib = true;
                           progUseEntityAttrib = true;
                        } else if (sl.isAttribute("mc_midTexCoord")) {
                           useMidTexCoordAttrib = true;
                           progUseMidTexCoordAttrib = true;
                        } else if (sl.isAttribute("at_tangent")) {
                           useTangentAttrib = true;
                           progUseTangentAttrib = true;
                        }

                        if (sl.isConstInt("countInstances")) {
                           program.setCountInstances(sl.getValueInt());
                           SMCLog.info("countInstances: " + program.getCountInstances());
                        }
                     }
                  }
               }
            } catch (Exception var11) {
               SMCLog.severe("Couldn't read " + filename + "!");
               var11.printStackTrace();
               ARBShaderObjects.glDeleteObjectARB(vertShader);
               return 0;
            }
         }

         if (saveFinalShaders) {
            saveShader(filename, vertexCode.toString());
         }

         ARBShaderObjects.glShaderSourceARB(vertShader, vertexCode);
         ARBShaderObjects.glCompileShaderARB(vertShader);
         if (GL20.glGetShaderi(vertShader, 35713) != 1) {
            SMCLog.severe("Error compiling vertex shader: " + filename);
         }

         printShaderLogInfo(vertShader, filename, listFiles);
         return vertShader;
      }
   }

   private static int createGeomShader(Program program, String filename) {
      int geomShader = ARBShaderObjects.glCreateShaderObjectARB(36313);
      if (geomShader == 0) {
         return 0;
      } else {
         StringBuilder geomCode = new StringBuilder(131072);
         BufferedReader reader = null;

         try {
            reader = new BufferedReader(getShaderReader(filename));
         } catch (Exception var11) {
            ARBShaderObjects.glDeleteObjectARB(geomShader);
            return 0;
         }

         ShaderOption[] activeOptions = getChangedOptions(shaderPackOptions);
         List<String> listFiles = new ArrayList<>();
         progArbGeometryShader4 = false;
         progMaxVerticesOut = 3;
         if (reader != null) {
            try {
               reader = ShaderPackParser.resolveIncludes(reader, filename, shaderPack, 0, listFiles, 0);
               MacroState macroState = new MacroState();

               while (true) {
                  String line = reader.readLine();
                  if (line == null) {
                     reader.close();
                     break;
                  }

                  line = applyOptions(line, activeOptions);
                  geomCode.append(line).append('\n');
                  if (macroState.processLine(line)) {
                     ShaderLine sl = ShaderParser.parseLine(line);
                     if (sl != null) {
                        if (sl.isExtension("GL_ARB_geometry_shader4")) {
                           String val = Config.normalize(sl.getValue());
                           if (val.equals("enable") || val.equals("require") || val.equals("warn")) {
                              progArbGeometryShader4 = true;
                           }
                        }

                        if (sl.isConstInt("maxVerticesOut")) {
                           progMaxVerticesOut = sl.getValueInt();
                        }
                     }
                  }
               }
            } catch (Exception var12) {
               SMCLog.severe("Couldn't read " + filename + "!");
               var12.printStackTrace();
               ARBShaderObjects.glDeleteObjectARB(geomShader);
               return 0;
            }
         }

         if (saveFinalShaders) {
            saveShader(filename, geomCode.toString());
         }

         ARBShaderObjects.glShaderSourceARB(geomShader, geomCode);
         ARBShaderObjects.glCompileShaderARB(geomShader);
         if (GL20.glGetShaderi(geomShader, 35713) != 1) {
            SMCLog.severe("Error compiling geometry shader: " + filename);
         }

         printShaderLogInfo(geomShader, filename, listFiles);
         return geomShader;
      }
   }

   private static int createFragShader(Program program, String filename) {
      int fragShader = ARBShaderObjects.glCreateShaderObjectARB(35632);
      if (fragShader == 0) {
         return 0;
      } else {
         StringBuilder fragCode = new StringBuilder(131072);
         BufferedReader reader = null;

         try {
            reader = new BufferedReader(getShaderReader(filename));
         } catch (Exception var14) {
            ARBShaderObjects.glDeleteObjectARB(fragShader);
            return 0;
         }

         ShaderOption[] activeOptions = getChangedOptions(shaderPackOptions);
         List<String> listFiles = new ArrayList<>();
         if (reader != null) {
            try {
               reader = ShaderPackParser.resolveIncludes(reader, filename, shaderPack, 0, listFiles, 0);
               MacroState macroState = new MacroState();

               while (true) {
                  String line = reader.readLine();
                  if (line == null) {
                     reader.close();
                     break;
                  }

                  line = applyOptions(line, activeOptions);
                  fragCode.append(line).append('\n');
                  if (macroState.processLine(line)) {
                     ShaderLine sl = ShaderParser.parseLine(line);
                     if (sl != null) {
                        if (sl.isUniform()) {
                           String uniform = sl.getName();
                           int index;
                           if ((index = ShaderParser.getShadowDepthIndex(uniform)) >= 0) {
                              usedShadowDepthBuffers = Math.max(usedShadowDepthBuffers, index + 1);
                           } else if ((index = ShaderParser.getShadowColorIndex(uniform)) >= 0) {
                              usedShadowColorBuffers = Math.max(usedShadowColorBuffers, index + 1);
                           } else if ((index = ShaderParser.getDepthIndex(uniform)) >= 0) {
                              usedDepthBuffers = Math.max(usedDepthBuffers, index + 1);
                           } else if (uniform.equals("gdepth") && gbuffersFormat[1] == 6408) {
                              gbuffersFormat[1] = 34836;
                           } else if ((index = ShaderParser.getColorIndex(uniform)) >= 0) {
                              usedColorBuffers = Math.max(usedColorBuffers, index + 1);
                           } else if (uniform.equals("centerDepthSmooth")) {
                              centerDepthSmoothEnabled = true;
                           }
                        } else if (sl.isConstInt("shadowMapResolution") || sl.isProperty("SHADOWRES")) {
                           spShadowMapWidth = spShadowMapHeight = sl.getValueInt();
                           shadowMapWidth = shadowMapHeight = Math.round((float)spShadowMapWidth * configShadowResMul);
                           SMCLog.info("Shadow map resolution: " + spShadowMapWidth);
                        } else if (sl.isConstFloat("shadowMapFov") || sl.isProperty("SHADOWFOV")) {
                           shadowMapFOV = sl.getValueFloat();
                           shadowMapIsOrtho = false;
                           SMCLog.info("Shadow map field of view: " + shadowMapFOV);
                        } else if (sl.isConstFloat("shadowDistance") || sl.isProperty("SHADOWHPL")) {
                           shadowMapHalfPlane = sl.getValueFloat();
                           shadowMapIsOrtho = true;
                           SMCLog.info("Shadow map distance: " + shadowMapHalfPlane);
                        } else if (sl.isConstFloat("shadowDistanceRenderMul")) {
                           shadowDistanceRenderMul = sl.getValueFloat();
                           SMCLog.info("Shadow distance render mul: " + shadowDistanceRenderMul);
                        } else if (sl.isConstFloat("shadowIntervalSize")) {
                           shadowIntervalSize = sl.getValueFloat();
                           SMCLog.info("Shadow map interval size: " + shadowIntervalSize);
                        } else if (sl.isConstBool("generateShadowMipmap", true)) {
                           Arrays.fill(shadowMipmapEnabled, true);
                           SMCLog.info("Generate shadow mipmap");
                        } else if (sl.isConstBool("generateShadowColorMipmap", true)) {
                           Arrays.fill(shadowColorMipmapEnabled, true);
                           SMCLog.info("Generate shadow color mipmap");
                        } else if (sl.isConstBool("shadowHardwareFiltering", true)) {
                           Arrays.fill(shadowHardwareFilteringEnabled, true);
                           SMCLog.info("Hardware shadow filtering enabled.");
                        } else if (sl.isConstBool("shadowHardwareFiltering0", true)) {
                           shadowHardwareFilteringEnabled[0] = true;
                           SMCLog.info("shadowHardwareFiltering0");
                        } else if (sl.isConstBool("shadowHardwareFiltering1", true)) {
                           shadowHardwareFilteringEnabled[1] = true;
                           SMCLog.info("shadowHardwareFiltering1");
                        } else if (sl.isConstBool("shadowtex0Mipmap", "shadowtexMipmap", true)) {
                           shadowMipmapEnabled[0] = true;
                           SMCLog.info("shadowtex0Mipmap");
                        } else if (sl.isConstBool("shadowtex1Mipmap", true)) {
                           shadowMipmapEnabled[1] = true;
                           SMCLog.info("shadowtex1Mipmap");
                        } else if (sl.isConstBool("shadowcolor0Mipmap", "shadowColor0Mipmap", true)) {
                           shadowColorMipmapEnabled[0] = true;
                           SMCLog.info("shadowcolor0Mipmap");
                        } else if (sl.isConstBool("shadowcolor1Mipmap", "shadowColor1Mipmap", true)) {
                           shadowColorMipmapEnabled[1] = true;
                           SMCLog.info("shadowcolor1Mipmap");
                        } else if (sl.isConstBool("shadowtex0Nearest", "shadowtexNearest", "shadow0MinMagNearest", true)) {
                           shadowFilterNearest[0] = true;
                           SMCLog.info("shadowtex0Nearest");
                        } else if (sl.isConstBool("shadowtex1Nearest", "shadow1MinMagNearest", true)) {
                           shadowFilterNearest[1] = true;
                           SMCLog.info("shadowtex1Nearest");
                        } else if (sl.isConstBool("shadowcolor0Nearest", "shadowColor0Nearest", "shadowColor0MinMagNearest", true)) {
                           shadowColorFilterNearest[0] = true;
                           SMCLog.info("shadowcolor0Nearest");
                        } else if (sl.isConstBool("shadowcolor1Nearest", "shadowColor1Nearest", "shadowColor1MinMagNearest", true)) {
                           shadowColorFilterNearest[1] = true;
                           SMCLog.info("shadowcolor1Nearest");
                        } else if (sl.isConstFloat("wetnessHalflife") || sl.isProperty("WETNESSHL")) {
                           wetnessHalfLife = sl.getValueFloat();
                           SMCLog.info("Wetness halflife: " + wetnessHalfLife);
                        } else if (sl.isConstFloat("drynessHalflife") || sl.isProperty("DRYNESSHL")) {
                           drynessHalfLife = sl.getValueFloat();
                           SMCLog.info("Dryness halflife: " + drynessHalfLife);
                        } else if (sl.isConstFloat("eyeBrightnessHalflife")) {
                           eyeBrightnessHalflife = sl.getValueFloat();
                           SMCLog.info("Eye brightness halflife: " + eyeBrightnessHalflife);
                        } else if (sl.isConstFloat("centerDepthHalflife")) {
                           centerDepthSmoothHalflife = sl.getValueFloat();
                           SMCLog.info("Center depth halflife: " + centerDepthSmoothHalflife);
                        } else if (sl.isConstFloat("sunPathRotation")) {
                           sunPathRotation = sl.getValueFloat();
                           SMCLog.info("Sun path rotation: " + sunPathRotation);
                        } else if (sl.isConstFloat("ambientOcclusionLevel")) {
                           aoLevel = NumUtils.limit(sl.getValueFloat(), 0.0F, 1.0F);
                           SMCLog.info("AO Level: " + aoLevel);
                        } else if (sl.isConstInt("superSamplingLevel")) {
                           int ssaa = sl.getValueInt();
                           if (ssaa > 1) {
                              SMCLog.info("Super sampling level: " + ssaa + "x");
                              superSamplingLevel = ssaa;
                           } else {
                              superSamplingLevel = 1;
                           }
                        } else if (sl.isConstInt("noiseTextureResolution")) {
                           noiseTextureResolution = sl.getValueInt();
                           noiseTextureEnabled = true;
                           SMCLog.info("Noise texture enabled");
                           SMCLog.info("Noise texture resolution: " + noiseTextureResolution);
                        } else if (sl.isConstIntSuffix("Format")) {
                           String name = StrUtils.removeSuffix(sl.getName(), "Format");
                           String value = sl.getValue();
                           int bufferindex = getBufferIndexFromString(name);
                           int format = getTextureFormatFromString(value);
                           if (bufferindex >= 0 && format != 0) {
                              gbuffersFormat[bufferindex] = format;
                              SMCLog.info("%s format: %s", name, value);
                           }
                        } else if (sl.isConstBoolSuffix("Clear", false)) {
                           if (ShaderParser.isComposite(filename) || ShaderParser.isDeferred(filename)) {
                              String name = StrUtils.removeSuffix(sl.getName(), "Clear");
                              int bufferindex = getBufferIndexFromString(name);
                              if (bufferindex >= 0) {
                                 gbuffersClear[bufferindex] = false;
                                 SMCLog.info("%s clear disabled", name);
                              }
                           }
                        } else if (sl.isConstVec4Suffix("ClearColor")) {
                           if (ShaderParser.isComposite(filename) || ShaderParser.isDeferred(filename)) {
                              String name = StrUtils.removeSuffix(sl.getName(), "ClearColor");
                              int bufferindex = getBufferIndexFromString(name);
                              if (bufferindex >= 0) {
                                 Vector4f col = sl.getValueVec4();
                                 if (col != null) {
                                    gbuffersClearColor[bufferindex] = col;
                                    SMCLog.info("%s clear color: %s %s %s %s", name, col.getX(), col.getY(), col.getZ(), col.getW());
                                 } else {
                                    SMCLog.warning("Invalid color value: " + sl.getValue());
                                 }
                              }
                           }
                        } else if (sl.isProperty("GAUX4FORMAT", "RGBA32F")) {
                           gbuffersFormat[7] = 34836;
                           SMCLog.info("gaux4 format : RGB32AF");
                        } else if (sl.isProperty("GAUX4FORMAT", "RGB32F")) {
                           gbuffersFormat[7] = 34837;
                           SMCLog.info("gaux4 format : RGB32F");
                        } else if (sl.isProperty("GAUX4FORMAT", "RGB16")) {
                           gbuffersFormat[7] = 32852;
                           SMCLog.info("gaux4 format : RGB16");
                        } else if (sl.isConstBoolSuffix("MipmapEnabled", true)) {
                           if (ShaderParser.isComposite(filename) || ShaderParser.isDeferred(filename) || ShaderParser.isFinal(filename)) {
                              String name = StrUtils.removeSuffix(sl.getName(), "MipmapEnabled");
                              int bufferindex = getBufferIndexFromString(name);
                              if (bufferindex >= 0) {
                                 int compositeMipmapSetting = program.getCompositeMipmapSetting();
                                 compositeMipmapSetting |= 1 << bufferindex;
                                 program.setCompositeMipmapSetting(compositeMipmapSetting);
                                 SMCLog.info("%s mipmap enabled", name);
                              }
                           }
                        } else if (sl.isProperty("DRAWBUFFERS")) {
                           String val = sl.getValue();
                           if (ShaderParser.isValidDrawBuffers(val)) {
                              program.setDrawBufSettings(val);
                           } else {
                              SMCLog.warning("Invalid draw buffers: " + val);
                           }
                        }
                     }
                  }
               }
            } catch (Exception var15) {
               SMCLog.severe("Couldn't read " + filename + "!");
               var15.printStackTrace();
               ARBShaderObjects.glDeleteObjectARB(fragShader);
               return 0;
            }
         }

         if (saveFinalShaders) {
            saveShader(filename, fragCode.toString());
         }

         ARBShaderObjects.glShaderSourceARB(fragShader, fragCode);
         ARBShaderObjects.glCompileShaderARB(fragShader);
         if (GL20.glGetShaderi(fragShader, 35713) != 1) {
            SMCLog.severe("Error compiling fragment shader: " + filename);
         }

         printShaderLogInfo(fragShader, filename, listFiles);
         return fragShader;
      }
   }

   private static Reader getShaderReader(String filename) {
      return new InputStreamReader(shaderPack.getResourceAsStream(filename));
   }

   public static void saveShader(String filename, String code) {
      try {
         File file = new File(shaderPacksDir, "debug/" + filename);
         file.getParentFile().mkdirs();
         Config.writeFile(file, code);
      } catch (IOException var3) {
         Photon.LOGGER.warn("Error saving: " + filename);
         var3.printStackTrace();
      }
   }

   private static void clearDirectory(File dir) {
      if (dir.exists()) {
         if (dir.isDirectory()) {
            File[] files = dir.listFiles();
            if (files != null) {
               for (int i = 0; i < files.length; i++) {
                  File file = files[i];
                  if (file.isDirectory()) {
                     clearDirectory(file);
                  }

                  file.delete();
               }
            }
         }
      }
   }

   private static boolean printLogInfo(int obj, String name) {
      IntBuffer iVal = BufferUtils.createIntBuffer(1);
      ARBShaderObjects.glGetObjectParameterARB(obj, 35716, iVal);
      int length = iVal.get();
      if (length > 1) {
         ByteBuffer infoLog = BufferUtils.createByteBuffer(length);
         ((Buffer)iVal).flip();
         ARBShaderObjects.glGetInfoLogARB(obj, iVal, infoLog);
         byte[] infoBytes = new byte[length];
         infoLog.get(infoBytes);
         if (infoBytes[length - 1] == 0) {
            infoBytes[length - 1] = 10;
         }

         String out = new String(infoBytes, Charsets.US_ASCII);
         out = StrUtils.trim(out, " \n\r\t");
         SMCLog.info("Info log: " + name + "\n" + out);
         return false;
      } else {
         return true;
      }
   }

   private static boolean printShaderLogInfo(int shader, String name, List<String> listFiles) {
      IntBuffer iVal = BufferUtils.createIntBuffer(1);
      int length = GL20.glGetShaderi(shader, 35716);
      if (length <= 1) {
         return true;
      } else {
         for (int i = 0; i < listFiles.size(); i++) {
            String path = listFiles.get(i);
            SMCLog.info("File: " + (i + 1) + " = " + path);
         }

         String log = GL20.glGetShaderInfoLog(shader, length);
         log = StrUtils.trim(log, " \n\r\t");
         SMCLog.info("Shader info log: " + name + "\n" + log);
         return false;
      }
   }

   public static void setDrawBuffers(IntBuffer drawBuffers) {
      if (drawBuffers == null) {
         drawBuffers = drawBuffersNone;
      }

      if (activeDrawBuffers != drawBuffers) {
         activeDrawBuffers = drawBuffers;
         GL20.glDrawBuffers(drawBuffers);
         checkGLError("setDrawBuffers");
      }
   }

   public static void useProgram(Program program) {
      checkGLError("pre-useProgram");
      if (isShadowPass) {
         program = ProgramShadow;
      } else if (isEntitiesGlowing) {
         program = ProgramEntitiesGlowing;
      }

      if (activeProgram != program) {
         updateAlphaBlend(activeProgram, program);
         activeProgram = program;
         int programID = program.getId();
         activeProgramID = programID;
         ARBShaderObjects.glUseProgramObjectARB(programID);
         if (checkGLError("useProgram") != 0) {
            program.setId(0);
            programID = program.getId();
            activeProgramID = programID;
            ARBShaderObjects.glUseProgramObjectARB(programID);
         }

         shaderUniforms.setProgram(programID);
         if (customUniforms != null) {
            customUniforms.setProgram(programID);
         }

         if (programID != 0) {
            IntBuffer drawBuffers = program.getDrawBuffers();
            if (isRenderingDfb) {
               setDrawBuffers(drawBuffers);
            }

            activeCompositeMipmapSetting = program.getCompositeMipmapSetting();
            switch (program.getProgramStage()) {
               case GBUFFERS:
                  setProgramUniform1i(uniform_texture, 0);
                  setProgramUniform1i(uniform_lightmap, 1);
                  setProgramUniform1i(uniform_normals, 2);
                  setProgramUniform1i(uniform_specular, 3);
                  setProgramUniform1i(uniform_shadow, waterShadowEnabled ? 5 : 4);
                  setProgramUniform1i(uniform_watershadow, 4);
                  setProgramUniform1i(uniform_shadowtex0, 4);
                  setProgramUniform1i(uniform_shadowtex1, 5);
                  setProgramUniform1i(uniform_depthtex0, 6);
                  if (customTexturesGbuffers != null || hasDeferredPrograms) {
                     setProgramUniform1i(uniform_gaux1, 7);
                     setProgramUniform1i(uniform_gaux2, 8);
                     setProgramUniform1i(uniform_gaux3, 9);
                     setProgramUniform1i(uniform_gaux4, 10);
                  }

                  setProgramUniform1i(uniform_depthtex1, 11);
                  setProgramUniform1i(uniform_shadowcolor, 13);
                  setProgramUniform1i(uniform_shadowcolor0, 13);
                  setProgramUniform1i(uniform_shadowcolor1, 14);
                  setProgramUniform1i(uniform_noisetex, 15);
                  break;
               case DEFERRED:
               case COMPOSITE:
                  setProgramUniform1i(uniform_gcolor, 0);
                  setProgramUniform1i(uniform_gdepth, 1);
                  setProgramUniform1i(uniform_gnormal, 2);
                  setProgramUniform1i(uniform_composite, 3);
                  setProgramUniform1i(uniform_gaux1, 7);
                  setProgramUniform1i(uniform_gaux2, 8);
                  setProgramUniform1i(uniform_gaux3, 9);
                  setProgramUniform1i(uniform_gaux4, 10);
                  setProgramUniform1i(uniform_colortex0, 0);
                  setProgramUniform1i(uniform_colortex1, 1);
                  setProgramUniform1i(uniform_colortex2, 2);
                  setProgramUniform1i(uniform_colortex3, 3);
                  setProgramUniform1i(uniform_colortex4, 7);
                  setProgramUniform1i(uniform_colortex5, 8);
                  setProgramUniform1i(uniform_colortex6, 9);
                  setProgramUniform1i(uniform_colortex7, 10);
                  setProgramUniform1i(uniform_shadow, waterShadowEnabled ? 5 : 4);
                  setProgramUniform1i(uniform_watershadow, 4);
                  setProgramUniform1i(uniform_shadowtex0, 4);
                  setProgramUniform1i(uniform_shadowtex1, 5);
                  setProgramUniform1i(uniform_gdepthtex, 6);
                  setProgramUniform1i(uniform_depthtex0, 6);
                  setProgramUniform1i(uniform_depthtex1, 11);
                  setProgramUniform1i(uniform_depthtex2, 12);
                  setProgramUniform1i(uniform_shadowcolor, 13);
                  setProgramUniform1i(uniform_shadowcolor0, 13);
                  setProgramUniform1i(uniform_shadowcolor1, 14);
                  setProgramUniform1i(uniform_noisetex, 15);
                  break;
               case SHADOW:
                  setProgramUniform1i(uniform_tex, 0);
                  setProgramUniform1i(uniform_texture, 0);
                  setProgramUniform1i(uniform_lightmap, 1);
                  setProgramUniform1i(uniform_normals, 2);
                  setProgramUniform1i(uniform_specular, 3);
                  setProgramUniform1i(uniform_shadow, waterShadowEnabled ? 5 : 4);
                  setProgramUniform1i(uniform_watershadow, 4);
                  setProgramUniform1i(uniform_shadowtex0, 4);
                  setProgramUniform1i(uniform_shadowtex1, 5);
                  if (customTexturesGbuffers != null) {
                     setProgramUniform1i(uniform_gaux1, 7);
                     setProgramUniform1i(uniform_gaux2, 8);
                     setProgramUniform1i(uniform_gaux3, 9);
                     setProgramUniform1i(uniform_gaux4, 10);
                  }

                  setProgramUniform1i(uniform_shadowcolor, 13);
                  setProgramUniform1i(uniform_shadowcolor0, 13);
                  setProgramUniform1i(uniform_shadowcolor1, 14);
                  setProgramUniform1i(uniform_noisetex, 15);
            }

            ItemStack stack = mc.player != null ? mc.player.getStackInHand() : null;
            Item item = stack != null ? stack.getItem() : null;
            int itemID = -1;
            Block block = null;
            if (item != null) {
               itemID = Item.REGISTRY.getRawId(item);
               block = (Block)Block.REGISTRY.getByRawId(itemID);
               itemID = ItemAliases.getItemAliasId(itemID);
            }

            int blockLight = block != null ? block.getLightLevel() : 0;
            setProgramUniform1i(uniform_heldItemId, itemID);
            setProgramUniform1i(uniform_heldBlockLightValue, blockLight);
            setProgramUniform1i(uniform_fogMode, fogEnabled ? fogMode : 0);
            setProgramUniform1f(uniform_fogDensity, fogEnabled ? fogDensity : 0.0F);
            setProgramUniform3f(uniform_fogColor, fogColorR, fogColorG, fogColorB);
            setProgramUniform3f(uniform_skyColor, skyColorR, skyColorG, skyColorB);
            setProgramUniform1i(uniform_worldTime, (int)(worldTime % 24000L));
            setProgramUniform1i(uniform_worldDay, (int)(worldTime / 24000L));
            setProgramUniform1i(uniform_moonPhase, moonPhase);
            setProgramUniform1i(uniform_frameCounter, frameCounter);
            setProgramUniform1f(uniform_frameTime, frameTime);
            setProgramUniform1f(uniform_frameTimeCounter, frameTimeCounter);
            setProgramUniform1f(uniform_sunAngle, sunAngle);
            setProgramUniform1f(uniform_shadowAngle, shadowAngle);
            setProgramUniform1f(uniform_rainStrength, rainStrength);
            setProgramUniform1f(uniform_aspectRatio, (float)renderWidth / (float)renderHeight);
            setProgramUniform1f(uniform_viewWidth, (float)renderWidth);
            setProgramUniform1f(uniform_viewHeight, (float)renderHeight);
            setProgramUniform1f(uniform_near, 0.05F);
            setProgramUniform1f(uniform_far, (float)(mc.options.viewDistance * 16));
            setProgramUniform3f(uniform_sunPosition, sunPosition[0], sunPosition[1], sunPosition[2]);
            setProgramUniform3f(uniform_moonPosition, moonPosition[0], moonPosition[1], moonPosition[2]);
            setProgramUniform3f(uniform_shadowLightPosition, shadowLightPosition[0], shadowLightPosition[1], shadowLightPosition[2]);
            setProgramUniform3f(uniform_upPosition, upPosition[0], upPosition[1], upPosition[2]);
            setProgramUniform3f(uniform_previousCameraPosition, (float)previousCameraPositionX, (float)previousCameraPositionY, (float)previousCameraPositionZ);
            setProgramUniform3f(uniform_cameraPosition, (float)cameraPositionX, (float)cameraPositionY, (float)cameraPositionZ);
            setProgramUniformMatrix4ARB(uniform_gbufferModelView, false, modelView);
            setProgramUniformMatrix4ARB(uniform_gbufferModelViewInverse, false, modelViewInverse);
            setProgramUniformMatrix4ARB(uniform_gbufferPreviousProjection, false, previousProjection);
            setProgramUniformMatrix4ARB(uniform_gbufferProjection, false, projection);
            setProgramUniformMatrix4ARB(uniform_gbufferProjectionInverse, false, projectionInverse);
            setProgramUniformMatrix4ARB(uniform_gbufferPreviousModelView, false, previousModelView);
            if (usedShadowDepthBuffers > 0) {
               setProgramUniformMatrix4ARB(uniform_shadowProjection, false, shadowProjection);
               setProgramUniformMatrix4ARB(uniform_shadowProjectionInverse, false, shadowProjectionInverse);
               setProgramUniformMatrix4ARB(uniform_shadowModelView, false, shadowModelView);
               setProgramUniformMatrix4ARB(uniform_shadowModelViewInverse, false, shadowModelViewInverse);
            }

            setProgramUniform1f(uniform_wetness, wetness);
            setProgramUniform1f(uniform_eyeAltitude, eyePosY);
            setProgramUniform2i(uniform_eyeBrightness, eyeBrightness & 65535, eyeBrightness >> 16);
            setProgramUniform2i(uniform_eyeBrightnessSmooth, Math.round(eyeBrightnessFadeX), Math.round(eyeBrightnessFadeY));
            setProgramUniform2i(uniform_terrainTextureSize, terrainTextureSize[0], terrainTextureSize[1]);
            setProgramUniform1i(uniform_terrainIconSize, terrainIconSize);
            setProgramUniform1i(uniform_isEyeInWater, isEyeInWater);
            setProgramUniform1f(uniform_nightVision, nightVision);
            setProgramUniform1f(uniform_blindness, blindness);
            setProgramUniform1f(uniform_screenBrightness, mc.options.gamma);
            setProgramUniform1i(uniform_hideGUI, mc.options.hudHidden ? 1 : 0);
            setProgramUniform1f(uniform_centerDepthSmooth, centerDepthSmooth);
            setProgramUniform2i(uniform_atlasSize, atlasSizeX, atlasSizeY);
            if (customUniforms != null) {
               customUniforms.update();
            }

            checkGLError("end useProgram");
         }
      }
   }

   private static void updateAlphaBlend(Program programOld, Program programNew) {
  
   }

   private static void setProgramUniform1i(ShaderUniform1i su, int value) {
      su.setValue(value);
   }

   private static void setProgramUniform2i(ShaderUniform2i su, int i0, int i1) {
      su.setValue(i0, i1);
   }

   private static void setProgramUniform1f(ShaderUniform1f su, float value) {
      su.setValue(value);
   }

   private static void setProgramUniform3f(ShaderUniform3f su, float f0, float f1, float f2) {
      su.setValue(f0, f1, f2);
   }

   private static void setProgramUniformMatrix4ARB(ShaderUniformM4 su, boolean transpose, FloatBuffer matrix) {
      su.setValue(transpose, matrix);
   }

   public static int getBufferIndexFromString(String name) {
      if (name.equals("colortex0") || name.equals("gcolor")) {
         return 0;
      } else if (name.equals("colortex1") || name.equals("gdepth")) {
         return 1;
      } else if (name.equals("colortex2") || name.equals("gnormal")) {
         return 2;
      } else if (name.equals("colortex3") || name.equals("composite")) {
         return 3;
      } else if (name.equals("colortex4") || name.equals("gaux1")) {
         return 4;
      } else if (name.equals("colortex5") || name.equals("gaux2")) {
         return 5;
      } else if (name.equals("colortex6") || name.equals("gaux3")) {
         return 6;
      } else {
         return !name.equals("colortex7") && !name.equals("gaux4") ? -1 : 7;
      }
   }

   private static int getTextureFormatFromString(String par) {
      par = par.trim();

      for (int i = 0; i < formatNames.length; i++) {
         String name = formatNames[i];
         if (par.equals(name)) {
            return formatIds[i];
         }
      }

      return 0;
   }

   private static void setupNoiseTexture() {
      if (noiseTexture == null && noiseTexturePath != null) {
         noiseTexture = loadCustomTexture(15, noiseTexturePath);
      }

      if (noiseTexture == null) {
         noiseTexture = new HFNoiseTexture(noiseTextureResolution, noiseTextureResolution);
      }
   }

   private static void loadEntityDataMap() {
      mapBlockToEntityData = new IdentityHashMap<>(300);
      if (mapBlockToEntityData.isEmpty()) {
         for (Identifier key : Block.REGISTRY.keySet()) {
            Block block = (Block)Block.REGISTRY.get(key);
            int id = Block.REGISTRY.getRawId(block);
            mapBlockToEntityData.put(block, id);
         }
      }

      BufferedReader reader = null;

      try {
         reader = new BufferedReader(new InputStreamReader(shaderPack.getResourceAsStream("/mc_Entity_x.txt")));
      } catch (Exception var8) {
      }

      if (reader != null) {
         String line;
         try {
            while ((line = reader.readLine()) != null) {
               Matcher m = patternLoadEntityDataMap.matcher(line);
               if (m.matches()) {
                  String name = m.group(1);
                  String value = m.group(2);
                  int id = Integer.parseInt(value);
                  Block block = Block.get(name);
                  if (block != null) {
                     mapBlockToEntityData.put(block, id);
                  } else {
                     SMCLog.warning("Unknown block name %s", name);
                  }
               } else {
                  SMCLog.warning("unmatched %s\n", line);
               }
            }
         } catch (Exception var9) {
            SMCLog.warning("Error parsing mc_Entity_x.txt");
         }
      }

      if (reader != null) {
         try {
            reader.close();
         } catch (Exception var7) {
         }
      }
   }

   private static IntBuffer fillIntBufferZero(IntBuffer buf) {
      int limit = buf.limit();

      for (int i = buf.position(); i < limit; i++) {
         buf.put(i, 0);
      }

      return buf;
   }

   public static void uninit() {
      if (isShaderPackInitialized) {
         checkGLError("Shaders.uninit pre");

         for (int i = 0; i < ProgramsAll.length; i++) {
            Program pi = ProgramsAll[i];
            if (pi.getRef() != 0) {
               ARBShaderObjects.glDeleteObjectARB(pi.getRef());
               checkGLError("del programRef");
            }

            pi.setRef(0);
            pi.setId(0);
            pi.setDrawBufSettings(null);
            pi.setDrawBuffers(null);
            pi.setCompositeMipmapSetting(0);
         }

         hasDeferredPrograms = false;
         if (dfb != 0) {
            EXTFramebufferObject.glDeleteFramebuffersEXT(dfb);
            dfb = 0;
            checkGLError("del dfb");
         }

         if (sfb != 0) {
            EXTFramebufferObject.glDeleteFramebuffersEXT(sfb);
            sfb = 0;
            checkGLError("del sfb");
         }

         if (dfbDepthTextures != null) {
            PhotonSM.deleteTextures(dfbDepthTextures);
            fillIntBufferZero(dfbDepthTextures);
            checkGLError("del dfbDepthTextures");
         }

         if (dfbColorTextures != null) {
            PhotonSM.deleteTextures(dfbColorTextures);
            fillIntBufferZero(dfbColorTextures);
            checkGLError("del dfbTextures");
         }

         if (sfbDepthTextures != null) {
            PhotonSM.deleteTextures(sfbDepthTextures);
            fillIntBufferZero(sfbDepthTextures);
            checkGLError("del shadow depth");
         }

         if (sfbColorTextures != null) {
            PhotonSM.deleteTextures(sfbColorTextures);
            fillIntBufferZero(sfbColorTextures);
            checkGLError("del shadow color");
         }

         if (dfbDrawBuffers != null) {
            fillIntBufferZero(dfbDrawBuffers);
         }

         if (noiseTexture != null) {
            noiseTexture.deleteTexture();
            noiseTexture = null;
         }

         SMCLog.info("Uninit");
         shadowPassInterval = 0;
         shouldSkipDefaultShadow = false;
         isShaderPackInitialized = false;
         checkGLError("Shaders.uninit");
      }
   }

   public static void scheduleResize() {
      renderDisplayHeight = 0;
   }

   public static void scheduleResizeShadow() {
      needResizeShadow = true;
   }

   private static void resize() {
      renderDisplayWidth = mc.width;
      renderDisplayHeight = mc.height;
      renderWidth = Math.round((float)renderDisplayWidth * configRenderResMul);
      renderHeight = Math.round((float)renderDisplayHeight * configRenderResMul);
      setupFrameBuffer();
   }

   private static void resizeShadow() {
      needResizeShadow = false;
      shadowMapWidth = Math.round((float)spShadowMapWidth * configShadowResMul);
      shadowMapHeight = Math.round((float)spShadowMapHeight * configShadowResMul);
      setupShadowFrameBuffer();
   }

   private static void setupFrameBuffer() {
      if (dfb != 0) {
         EXTFramebufferObject.glDeleteFramebuffersEXT(dfb);
         PhotonSM.deleteTextures(dfbDepthTextures);
         PhotonSM.deleteTextures(dfbColorTextures);
      }

      dfb = EXTFramebufferObject.glGenFramebuffersEXT();
      GL11.glGenTextures((IntBuffer)((Buffer)dfbDepthTextures).clear().limit(usedDepthBuffers));
      GL11.glGenTextures((IntBuffer)((Buffer)dfbColorTextures).clear().limit(16));
      ((Buffer)dfbDepthTextures).position(0);
      ((Buffer)dfbColorTextures).position(0);
      EXTFramebufferObject.glBindFramebufferEXT(36160, dfb);
      GL20.glDrawBuffers(0);
      GL11.glReadBuffer(0);

      for (int i = 0; i < usedDepthBuffers; i++) {
         GlStateManager.bindTexture(dfbDepthTextures.get(i));
         GL11.glTexParameteri(3553, 10242, 33071);
         GL11.glTexParameteri(3553, 10243, 33071);
         GL11.glTexParameteri(3553, 10241, 9728);
         GL11.glTexParameteri(3553, 10240, 9728);
         GL11.glTexParameteri(3553, 34891, 6409);
         GL11.glTexImage2D(3553, 0, 6402, renderWidth, renderHeight, 0, 6402, 5126, (FloatBuffer)null);
      }

      EXTFramebufferObject.glFramebufferTexture2DEXT(36160, 36096, 3553, dfbDepthTextures.get(0), 0);
      GL20.glDrawBuffers(dfbDrawBuffers);
      GL11.glReadBuffer(0);
      checkGLError("FT d");

      for (int i = 0; i < usedColorBuffers; i++) {
         GlStateManager.bindTexture(dfbColorTexturesFlip.getA(i));
         GL11.glTexParameteri(3553, 10242, 33071);
         GL11.glTexParameteri(3553, 10243, 33071);
         GL11.glTexParameteri(3553, 10241, 9729);
         GL11.glTexParameteri(3553, 10240, 9729);
         GL11.glTexImage2D(3553, 0, gbuffersFormat[i], renderWidth, renderHeight, 0, getPixelFormat(gbuffersFormat[i]), 33639, (ByteBuffer)null);
         EXTFramebufferObject.glFramebufferTexture2DEXT(36160, 36064 + i, 3553, dfbColorTexturesFlip.getA(i), 0);
         checkGLError("FT c");
      }

      for (int i = 0; i < usedColorBuffers; i++) {
         GlStateManager.bindTexture(dfbColorTexturesFlip.getB(i));
         GL11.glTexParameteri(3553, 10242, 33071);
         GL11.glTexParameteri(3553, 10243, 33071);
         GL11.glTexParameteri(3553, 10241, 9729);
         GL11.glTexParameteri(3553, 10240, 9729);
         GL11.glTexImage2D(3553, 0, gbuffersFormat[i], renderWidth, renderHeight, 0, getPixelFormat(gbuffersFormat[i]), 33639, (ByteBuffer)null);
         checkGLError("FT ca");
      }

      int status = EXTFramebufferObject.glCheckFramebufferStatusEXT(36160);
      if (status == 36058) {
         printChatAndLogError("[Shaders] Error: Failed framebuffer incomplete formats");

         for (int i = 0; i < usedColorBuffers; i++) {
            GlStateManager.bindTexture(dfbColorTexturesFlip.getA(i));
            GL11.glTexImage2D(3553, 0, 6408, renderWidth, renderHeight, 0, 32993, 33639, (ByteBuffer)null);
            EXTFramebufferObject.glFramebufferTexture2DEXT(36160, 36064 + i, 3553, dfbColorTexturesFlip.getA(i), 0);
            checkGLError("FT c");
         }

         status = EXTFramebufferObject.glCheckFramebufferStatusEXT(36160);
         if (status == 36053) {
            SMCLog.info("complete");
         }
      }

      GlStateManager.bindTexture(0);
      if (status != 36053) {
         printChatAndLogError("[Shaders] Error: Failed creating framebuffer! (Status " + status + ")");
      } else {
         SMCLog.info("Framebuffer created.");
      }
   }

   private static int getPixelFormat(int internalFormat) {
      switch (internalFormat) {
         case 33333:
         case 33334:
         case 33339:
         case 33340:
         case 36208:
         case 36209:
         case 36226:
         case 36227:
            return 36251;
         default:
            return 32993;
      }
   }

   private static void setupShadowFrameBuffer() {
      if (usedShadowDepthBuffers != 0) {
         if (sfb != 0) {
            EXTFramebufferObject.glDeleteFramebuffersEXT(sfb);
            PhotonSM.deleteTextures(sfbDepthTextures);
            PhotonSM.deleteTextures(sfbColorTextures);
         }

         sfb = EXTFramebufferObject.glGenFramebuffersEXT();
         EXTFramebufferObject.glBindFramebufferEXT(36160, sfb);
         GL11.glDrawBuffer(0);
         GL11.glReadBuffer(0);
         GL11.glGenTextures((IntBuffer)((Buffer)sfbDepthTextures).clear().limit(usedShadowDepthBuffers));
         GL11.glGenTextures((IntBuffer)((Buffer)sfbColorTextures).clear().limit(usedShadowColorBuffers));
         ((Buffer)sfbDepthTextures).position(0);
         ((Buffer)sfbColorTextures).position(0);

         for (int i = 0; i < usedShadowDepthBuffers; i++) {
            GlStateManager.bindTexture(sfbDepthTextures.get(i));
            GL11.glTexParameterf(3553, 10242, 33071.0F);
            GL11.glTexParameterf(3553, 10243, 33071.0F);
            int filter = shadowFilterNearest[i] ? 9728 : 9729;
            GL11.glTexParameteri(3553, 10241, filter);
            GL11.glTexParameteri(3553, 10240, filter);
            if (shadowHardwareFilteringEnabled[i]) {
               GL11.glTexParameteri(3553, 34892, 34894);
            }

            GL11.glTexImage2D(3553, 0, 6402, shadowMapWidth, shadowMapHeight, 0, 6402, 5126, (FloatBuffer)null);
         }

         EXTFramebufferObject.glFramebufferTexture2DEXT(36160, 36096, 3553, sfbDepthTextures.get(0), 0);
         checkGLError("FT sd");

         for (int i = 0; i < usedShadowColorBuffers; i++) {
            GlStateManager.bindTexture(sfbColorTextures.get(i));
            GL11.glTexParameterf(3553, 10242, 33071.0F);
            GL11.glTexParameterf(3553, 10243, 33071.0F);
            int filter = shadowColorFilterNearest[i] ? 9728 : 9729;
            GL11.glTexParameteri(3553, 10241, filter);
            GL11.glTexParameteri(3553, 10240, filter);
            GL11.glTexImage2D(3553, 0, 6408, shadowMapWidth, shadowMapHeight, 0, 32993, 33639, (ByteBuffer)null);
            EXTFramebufferObject.glFramebufferTexture2DEXT(36160, 36064 + i, 3553, sfbColorTextures.get(i), 0);
            checkGLError("FT sc");
         }

         GlStateManager.bindTexture(0);
         if (usedShadowColorBuffers > 0) {
            GL20.glDrawBuffers(sfbDrawBuffers);
         }

         int status = EXTFramebufferObject.glCheckFramebufferStatusEXT(36160);
         if (status != 36053) {
            printChatAndLogError("[Shaders] Error: Failed creating shadow framebuffer! (Status " + status + ")");
         } else {
            SMCLog.info("Shadow framebuffer created.");
         }
      }
   }

   public static void beginRender(MinecraftClient minecraft, float partialTicks, long finishTimeNano) {
      checkGLError("pre beginRender");
      checkWorldChanged(mc.world);
      mc = minecraft;
      mc.profiler.push("init");
      entityRenderer = mc.gameRenderer;
      if (!isShaderPackInitialized) {
         try {
            init();
         } catch (IllegalStateException var8) {
            if (Config.normalize(var8.getMessage()).equals("Function is not supported")) {
               printChatAndLogError("[Shaders] Error: " + var8.getMessage());
               var8.printStackTrace();
               setShaderPack("OFF");
               return;
            }
         }
      }

      if (mc.width != renderDisplayWidth || mc.height != renderDisplayHeight) {
         resize();
      }

      if (needResizeShadow) {
         resizeShadow();
      }

      worldTime = mc.world.getTimeOfDay();
      diffWorldTime = (worldTime - lastWorldTime) % 24000L;
      if (diffWorldTime < 0L) {
         diffWorldTime += 24000L;
      }

      lastWorldTime = worldTime;
      moonPhase = mc.world.getMoonPhase();
      frameCounter++;
      if (frameCounter >= 720720) {
         frameCounter = 0;
      }

      systemTime = System.currentTimeMillis();
      if (lastSystemTime == 0L) {
         lastSystemTime = systemTime;
      }

      diffSystemTime = systemTime - lastSystemTime;
      lastSystemTime = systemTime;
      frameTime = (float)diffSystemTime / 1000.0F;
      frameTimeCounter = frameTimeCounter + frameTime;
      frameTimeCounter %= 3600.0F;
      rainStrength = minecraft.world.getRainGradient(partialTicks);
      float fadeScalar = (float)diffSystemTime * 0.01F;
      float temp1 = (float)Math.exp(Math.log(0.5) * (double)fadeScalar / (double)(wetness < rainStrength ? drynessHalfLife : wetnessHalfLife));
      wetness = wetness * temp1 + rainStrength * (1.0F - temp1);
      Entity renderViewEntity = mc.getCameraEntity();
      if (renderViewEntity != null) {
         isSleeping = renderViewEntity instanceof LivingEntity && ((LivingEntity)renderViewEntity).isSleeping();
         eyePosY = (float)renderViewEntity.y * partialTicks + (float)renderViewEntity.prevTickY * (1.0F - partialTicks);
         eyeBrightness = renderViewEntity.getLightmapCoordinates(partialTicks);
         temp1 = (float)diffSystemTime * 0.01F;
         float temp2 = (float)Math.exp(Math.log(0.5) * (double)temp1 / (double)eyeBrightnessHalflife);
         eyeBrightnessFadeX = eyeBrightnessFadeX * temp2 + (float)(eyeBrightness & 65535) * (1.0F - temp2);
         eyeBrightnessFadeY = eyeBrightnessFadeY * temp2 + (float)(eyeBrightness >> 16) * (1.0F - temp2);
         Block cameraBlock = Camera.getSubmergedBlock(mc.world, renderViewEntity, partialTicks);
         Material cameraPosMaterial = cameraBlock.getMaterial();
         if (cameraPosMaterial == Material.WATER) {
            isEyeInWater = 1;
         } else if (cameraPosMaterial == Material.LAVA) {
            isEyeInWater = 2;
         } else {
            isEyeInWater = 0;
         }

         if (mc.player != null) {
            nightVision = 0.0F;
            if (mc.player.hasStatusEffect(StatusEffect.NIGHTVISION)) {
               nightVision = Config.getMinecraft().gameRenderer.getNightVisionStrength(mc.player, partialTicks);
            }

            blindness = 0.0F;
            if (mc.player.hasStatusEffect(StatusEffect.BLINDNESS)) {
               int blindnessTicks = mc.player.getEffectInstance(StatusEffect.BLINDNESS).getDuration();
               blindness = NumUtils.limit((float)blindnessTicks / 20.0F, 0.0F, 1.0F);
            }
         }

         Vec3d skyColorV = mc.world.method_3631(renderViewEntity, partialTicks);
         skyColorR = (float)skyColorV.x;
         skyColorG = (float)skyColorV.y;
         skyColorB = (float)skyColorV.z;
      }

      isRenderingWorld = true;
      isCompositeRendered = false;
      isShadowPass = false;
      isHandRenderedMain = false;
      isHandRenderedOff = false;
      skipRenderHandMain = false;
      skipRenderHandOff = false;
      bindGbuffersTextures();
      previousCameraPositionX = cameraPositionX;
      previousCameraPositionY = cameraPositionY;
      previousCameraPositionZ = cameraPositionZ;
      ((Buffer)previousProjection).position(0);
      ((Buffer)projection).position(0);
      previousProjection.put(projection);
      ((Buffer)previousProjection).position(0);
      ((Buffer)projection).position(0);
      ((Buffer)previousModelView).position(0);
      ((Buffer)modelView).position(0);
      previousModelView.put(modelView);
      ((Buffer)previousModelView).position(0);
      ((Buffer)modelView).position(0);
      checkGLError("beginRender");
      ShadersRender.renderShadowMap(entityRenderer, 0, partialTicks, finishTimeNano);
      mc.profiler.pop();
      EXTFramebufferObject.glBindFramebufferEXT(36160, dfb);

      for (int i = 0; i < usedColorBuffers; i++) {
         EXTFramebufferObject.glFramebufferTexture2DEXT(36160, 36064 + i, 3553, dfbColorTexturesFlip.getA(i), 0);
      }

      checkGLError("end beginRender");
   }

   private static void bindGbuffersTextures() {
      if (usedShadowDepthBuffers >= 1) {
         GlStateManager.activeTexture(33988);
         GlStateManager.bindTexture(sfbDepthTextures.get(0));
         if (usedShadowDepthBuffers >= 2) {
            GlStateManager.activeTexture(33989);
            GlStateManager.bindTexture(sfbDepthTextures.get(1));
         }
      }

      GlStateManager.activeTexture(33984);

      for (int i = 0; i < usedColorBuffers; i++) {
         GlStateManager.bindTexture(dfbColorTexturesFlip.getA(i));
         GL11.glTexParameteri(3553, 10240, 9729);
         GL11.glTexParameteri(3553, 10241, 9729);
         GlStateManager.bindTexture(dfbColorTexturesFlip.getB(i));
         GL11.glTexParameteri(3553, 10240, 9729);
         GL11.glTexParameteri(3553, 10241, 9729);
      }

      GlStateManager.bindTexture(0);

      for (int i = 0; i < 4 && 4 + i < usedColorBuffers; i++) {
         GlStateManager.activeTexture(33991 + i);
         GlStateManager.bindTexture(dfbColorTexturesFlip.getA(4 + i));
      }

      GlStateManager.activeTexture(33990);
      GlStateManager.bindTexture(dfbDepthTextures.get(0));
      if (usedDepthBuffers >= 2) {
         GlStateManager.activeTexture(33995);
         GlStateManager.bindTexture(dfbDepthTextures.get(1));
         if (usedDepthBuffers >= 3) {
            GlStateManager.activeTexture(33996);
            GlStateManager.bindTexture(dfbDepthTextures.get(2));
         }
      }

      for (int i = 0; i < usedShadowColorBuffers; i++) {
         GlStateManager.activeTexture(33997 + i);
         GlStateManager.bindTexture(sfbColorTextures.get(i));
      }

      if (noiseTextureEnabled) {
         GlStateManager.activeTexture(33984 + noiseTexture.getTextureUnit());
         GlStateManager.bindTexture(noiseTexture.getTextureId());
      }

      bindCustomTextures(customTexturesGbuffers);
      GlStateManager.activeTexture(33984);
   }

   public static void checkWorldChanged(World world) {
      if (currentWorld != world) {
         World oldWorld = currentWorld;
         currentWorld = world;
         setCameraOffset(mc.getCameraEntity());
         int dimIdOld = getDimensionId(oldWorld);
         int dimIdNew = getDimensionId(world);
         if (dimIdNew != dimIdOld) {
            boolean dimShadersOld = shaderPackDimensions.contains(dimIdOld);
            boolean dimShadersNew = shaderPackDimensions.contains(dimIdNew);
            if (dimShadersOld || dimShadersNew) {
               uninit();
            }
         }

         Smoother.resetValues();
      }
   }

   private static int getDimensionId(World world) {
      return world == null ? Integer.MIN_VALUE : world.dimension.getType();
   }

   public static void beginRenderPass(int pass, float partialTicks, long finishTimeNano) {
      if (!isShadowPass) {
         EXTFramebufferObject.glBindFramebufferEXT(36160, dfb);
         GL11.glViewport(0, 0, renderWidth, renderHeight);
         activeDrawBuffers = null;
         ShadersTex.bindNSTextures(((ExtendedTexture)defaultTexture).getMultiTex());
         useProgram(ProgramTextured);
         checkGLError("end beginRenderPass");
      }
   }

   public static void setViewport(int vx, int vy, int vw, int vh) {
      GlStateManager.colorMask(true, true, true, true);
      if (isShadowPass) {
         GL11.glViewport(0, 0, shadowMapWidth, shadowMapHeight);
      } else {
         GL11.glViewport(0, 0, renderWidth, renderHeight);
         EXTFramebufferObject.glBindFramebufferEXT(36160, dfb);
         isRenderingDfb = true;
         GlStateManager.enableCull();
         GlStateManager.enableDepthTest();
         setDrawBuffers(drawBuffersNone);
         useProgram(ProgramTextured);
         checkGLError("beginRenderPass");
      }
   }

   public static void setFogMode(int value) {
      fogMode = value;
      if (fogEnabled) {
         setProgramUniform1i(uniform_fogMode, value);
      }
   }

   public static void setFogColor(float r, float g, float b) {
      fogColorR = r;
      fogColorG = g;
      fogColorB = b;
      setProgramUniform3f(uniform_fogColor, fogColorR, fogColorG, fogColorB);
   }

   public static void setClearColor(float red, float green, float blue, float alpha) {
      GlStateManager.clearColor(red, green, blue, alpha);
      clearColorR = red;
      clearColorG = green;
      clearColorB = blue;
   }

   public static void clearRenderBuffer() {
      if (isShadowPass) {
         checkGLError("shadow clear pre");
         EXTFramebufferObject.glFramebufferTexture2DEXT(36160, 36096, 3553, sfbDepthTextures.get(0), 0);
         GL11.glClearColor(1.0F, 1.0F, 1.0F, 1.0F);
         GL20.glDrawBuffers(ProgramShadow.getDrawBuffers());
         checkFramebufferStatus("shadow clear");
         GL11.glClear(16640);
         checkGLError("shadow clear");
      } else {
         checkGLError("clear pre");
         if (gbuffersClear[0]) {
            Vector4f col = gbuffersClearColor[0];
            if (col != null) {
               GL11.glClearColor(col.getX(), col.getY(), col.getZ(), col.getW());
            }

            if (dfbColorTexturesFlip.isChanged(0)) {
               EXTFramebufferObject.glFramebufferTexture2DEXT(36160, 36064, 3553, dfbColorTexturesFlip.getB(0), 0);
               GL20.glDrawBuffers(36064);
               GL11.glClear(16384);
               EXTFramebufferObject.glFramebufferTexture2DEXT(36160, 36064, 3553, dfbColorTexturesFlip.getA(0), 0);
            }

            GL20.glDrawBuffers(36064);
            GL11.glClear(16384);
         }

         if (gbuffersClear[1]) {
            GL11.glClearColor(1.0F, 1.0F, 1.0F, 1.0F);
            Vector4f colx = gbuffersClearColor[1];
            if (colx != null) {
               GL11.glClearColor(colx.getX(), colx.getY(), colx.getZ(), colx.getW());
            }

            if (dfbColorTexturesFlip.isChanged(1)) {
               EXTFramebufferObject.glFramebufferTexture2DEXT(36160, 36065, 3553, dfbColorTexturesFlip.getB(1), 0);
               GL20.glDrawBuffers(36065);
               GL11.glClear(16384);
               EXTFramebufferObject.glFramebufferTexture2DEXT(36160, 36065, 3553, dfbColorTexturesFlip.getA(1), 0);
            }

            GL20.glDrawBuffers(36065);
            GL11.glClear(16384);
         }

         for (int i = 2; i < usedColorBuffers; i++) {
            if (gbuffersClear[i]) {
               GL11.glClearColor(0.0F, 0.0F, 0.0F, 0.0F);
               Vector4f colxx = gbuffersClearColor[i];
               if (colxx != null) {
                  GL11.glClearColor(colxx.getX(), colxx.getY(), colxx.getZ(), colxx.getW());
               }

               if (dfbColorTexturesFlip.isChanged(i)) {
                  EXTFramebufferObject.glFramebufferTexture2DEXT(36160, 36064 + i, 3553, dfbColorTexturesFlip.getB(i), 0);
                  GL20.glDrawBuffers(36064 + i);
                  GL11.glClear(16384);
                  EXTFramebufferObject.glFramebufferTexture2DEXT(36160, 36064 + i, 3553, dfbColorTexturesFlip.getA(i), 0);
               }

               GL20.glDrawBuffers(36064 + i);
               GL11.glClear(16384);
            }
         }

         setDrawBuffers(dfbDrawBuffers);
         checkFramebufferStatus("clear");
         checkGLError("clear");
      }
   }

   public static void setCamera(float partialTicks) {
      Entity viewEntity = mc.getCameraEntity();
      double x = viewEntity.prevTickX + (viewEntity.x - viewEntity.prevTickX) * (double)partialTicks;
      double y = viewEntity.prevTickY + (viewEntity.y - viewEntity.prevTickY) * (double)partialTicks;
      double z = viewEntity.prevTickZ + (viewEntity.z - viewEntity.prevTickZ) * (double)partialTicks;
      updateCameraOffset(viewEntity);
      cameraPositionX = x - (double)cameraOffsetX;
      cameraPositionY = y;
      cameraPositionZ = z - (double)cameraOffsetZ;
      GL11.glGetFloat(2983, (FloatBuffer)((Buffer)projection).position(0));
      SMath.invertMat4FBFA(
              (FloatBuffer)((Buffer)projectionInverse).position(0), (FloatBuffer)((Buffer)projection).position(0), faProjectionInverse, faProjection
      );
      ((Buffer)projection).position(0);
      ((Buffer)projectionInverse).position(0);
      GL11.glGetFloat(2982, (FloatBuffer)((Buffer)modelView).position(0));
      SMath.invertMat4FBFA((FloatBuffer)((Buffer)modelViewInverse).position(0), (FloatBuffer)((Buffer)modelView).position(0), faModelViewInverse, faModelView);
      ((Buffer)modelView).position(0);
      ((Buffer)modelViewInverse).position(0);
      checkGLError("setCamera");
   }

   private static void updateCameraOffset(Entity viewEntity) {
      double adx = Math.abs(cameraPositionX - previousCameraPositionX);
      double adz = Math.abs(cameraPositionZ - previousCameraPositionZ);
      double apx = Math.abs(cameraPositionX);
      double apz = Math.abs(cameraPositionZ);
      if (adx > 1000.0 || adz > 1000.0 || apx > 1000000.0 || apz > 1000000.0) {
         setCameraOffset(viewEntity);
      }
   }

   private static void setCameraOffset(Entity viewEntity) {
      if (viewEntity == null) {
         cameraOffsetX = 0;
         cameraOffsetZ = 0;
      } else {
         cameraOffsetX = (int)viewEntity.x / 1000 * 1000;
         cameraOffsetZ = (int)viewEntity.z / 1000 * 1000;
      }
   }

   public static void setCameraShadow(float partialTicks) {
      Entity viewEntity = mc.getCameraEntity();
      double x = viewEntity.prevTickX + (viewEntity.x - viewEntity.prevTickX) * (double)partialTicks;
      double y = viewEntity.prevTickY + (viewEntity.y - viewEntity.prevTickY) * (double)partialTicks;
      double z = viewEntity.prevTickZ + (viewEntity.z - viewEntity.prevTickZ) * (double)partialTicks;
      updateCameraOffset(viewEntity);
      cameraPositionX = x - (double)cameraOffsetX;
      cameraPositionY = y;
      cameraPositionZ = z - (double)cameraOffsetZ;
      GL11.glGetFloat(2983, (FloatBuffer)((Buffer)projection).position(0));
      SMath.invertMat4FBFA(
              (FloatBuffer)((Buffer)projectionInverse).position(0), (FloatBuffer)((Buffer)projection).position(0), faProjectionInverse, faProjection
      );
      ((Buffer)projection).position(0);
      ((Buffer)projectionInverse).position(0);
      GL11.glGetFloat(2982, (FloatBuffer)((Buffer)modelView).position(0));
      SMath.invertMat4FBFA((FloatBuffer)((Buffer)modelViewInverse).position(0), (FloatBuffer)((Buffer)modelView).position(0), faModelViewInverse, faModelView);
      ((Buffer)modelView).position(0);
      ((Buffer)modelViewInverse).position(0);
      GL11.glViewport(0, 0, shadowMapWidth, shadowMapHeight);
      GL11.glMatrixMode(5889);
      GL11.glLoadIdentity();
      if (shadowMapIsOrtho) {
         GL11.glOrtho((double)(-shadowMapHalfPlane), (double)shadowMapHalfPlane, (double)(-shadowMapHalfPlane), (double)shadowMapHalfPlane, 0.05F, 256.0);
      } else {
         GLU.gluPerspective(shadowMapFOV, (float)shadowMapWidth / (float)shadowMapHeight, 0.05F, 256.0F);
      }

      GL11.glMatrixMode(5888);
      GL11.glLoadIdentity();
      GL11.glTranslatef(0.0F, 0.0F, -100.0F);
      GL11.glRotatef(90.0F, 1.0F, 0.0F, 0.0F);
      celestialAngle = mc.world.getSkyAngle(partialTicks);
      sunAngle = celestialAngle < 0.75F ? celestialAngle + 0.25F : celestialAngle - 0.75F;
      float angle = celestialAngle * -360.0F;
      float angleInterval = shadowAngleInterval > 0.0F ? angle % shadowAngleInterval - shadowAngleInterval * 0.5F : 0.0F;
      if ((double)sunAngle <= 0.5) {
         GL11.glRotatef(angle - angleInterval, 0.0F, 0.0F, 1.0F);
         GL11.glRotatef(sunPathRotation, 1.0F, 0.0F, 0.0F);
         shadowAngle = sunAngle;
      } else {
         GL11.glRotatef(angle + 180.0F - angleInterval, 0.0F, 0.0F, 1.0F);
         GL11.glRotatef(sunPathRotation, 1.0F, 0.0F, 0.0F);
         shadowAngle = sunAngle - 0.5F;
      }

      if (shadowMapIsOrtho) {
         float trans = shadowIntervalSize;
         float trans2 = trans / 2.0F;
         GL11.glTranslatef((float)x % trans - trans2, (float)y % trans - trans2, (float)z % trans - trans2);
      }

      float raSun = sunAngle * (float) (Math.PI * 2);
      float x1 = (float)Math.cos((double)raSun);
      float y1 = (float)Math.sin((double)raSun);
      float raTilt = sunPathRotation * (float) (Math.PI * 2);
      float x2 = x1;
      float y2 = y1 * (float)Math.cos((double)raTilt);
      float z2 = y1 * (float)Math.sin((double)raTilt);
      if ((double)sunAngle > 0.5) {
         x2 = -x1;
         y2 = -y2;
         z2 = -z2;
      }

      shadowLightPositionVector[0] = x2;
      shadowLightPositionVector[1] = y2;
      shadowLightPositionVector[2] = z2;
      shadowLightPositionVector[3] = 0.0F;
      GL11.glGetFloat(2983, (FloatBuffer)((Buffer)shadowProjection).position(0));
      SMath.invertMat4FBFA(
              (FloatBuffer)((Buffer)shadowProjectionInverse).position(0),
              (FloatBuffer)((Buffer)shadowProjection).position(0),
              faShadowProjectionInverse,
              faShadowProjection
      );
      ((Buffer)shadowProjection).position(0);
      ((Buffer)shadowProjectionInverse).position(0);
      GL11.glGetFloat(2982, (FloatBuffer)((Buffer)shadowModelView).position(0));
      SMath.invertMat4FBFA(
              (FloatBuffer)((Buffer)shadowModelViewInverse).position(0),
              (FloatBuffer)((Buffer)shadowModelView).position(0),
              faShadowModelViewInverse,
              faShadowModelView
      );
      ((Buffer)shadowModelView).position(0);
      ((Buffer)shadowModelViewInverse).position(0);
      setProgramUniformMatrix4ARB(uniform_gbufferProjection, false, projection);
      setProgramUniformMatrix4ARB(uniform_gbufferProjectionInverse, false, projectionInverse);
      setProgramUniformMatrix4ARB(uniform_gbufferPreviousProjection, false, previousProjection);
      setProgramUniformMatrix4ARB(uniform_gbufferModelView, false, modelView);
      setProgramUniformMatrix4ARB(uniform_gbufferModelViewInverse, false, modelViewInverse);
      setProgramUniformMatrix4ARB(uniform_gbufferPreviousModelView, false, previousModelView);
      setProgramUniformMatrix4ARB(uniform_shadowProjection, false, shadowProjection);
      setProgramUniformMatrix4ARB(uniform_shadowProjectionInverse, false, shadowProjectionInverse);
      setProgramUniformMatrix4ARB(uniform_shadowModelView, false, shadowModelView);
      setProgramUniformMatrix4ARB(uniform_shadowModelViewInverse, false, shadowModelViewInverse);
      mc.options.perspective = 1;
      checkGLError("setCamera");
   }

   public static void preCelestialRotate() {
      GL11.glRotatef(sunPathRotation * 1.0F, 0.0F, 0.0F, 1.0F);
      checkGLError("preCelestialRotate");
   }

   public static void postCelestialRotate() {
      FloatBuffer modelView = tempMatrixDirectBuffer;
      ((Buffer)modelView).clear();
      GL11.glGetFloat(2982, modelView);
      modelView.get(tempMat, 0, 16);
      SMath.multiplyMat4xVec4(sunPosition, tempMat, sunPosModelView);
      SMath.multiplyMat4xVec4(moonPosition, tempMat, moonPosModelView);
      System.arraycopy(shadowAngle == sunAngle ? sunPosition : moonPosition, 0, shadowLightPosition, 0, 3);
      setProgramUniform3f(uniform_sunPosition, sunPosition[0], sunPosition[1], sunPosition[2]);
      setProgramUniform3f(uniform_moonPosition, moonPosition[0], moonPosition[1], moonPosition[2]);
      setProgramUniform3f(uniform_shadowLightPosition, shadowLightPosition[0], shadowLightPosition[1], shadowLightPosition[2]);
      if (customUniforms != null) {
         customUniforms.update();
      }

      checkGLError("postCelestialRotate");
   }

   public static void setUpPosition() {
      FloatBuffer modelView = tempMatrixDirectBuffer;
      ((Buffer)modelView).clear();
      GL11.glGetFloat(2982, modelView);
      modelView.get(tempMat, 0, 16);
      SMath.multiplyMat4xVec4(upPosition, tempMat, upPosModelView);
      setProgramUniform3f(uniform_upPosition, upPosition[0], upPosition[1], upPosition[2]);
      if (customUniforms != null) {
         customUniforms.update();
      }
   }

   public static void genCompositeMipmap() {
      if (hasGlGenMipmap) {
         for (int i = 0; i < usedColorBuffers; i++) {
            if ((activeCompositeMipmapSetting & 1 << i) != 0) {
               GlStateManager.activeTexture(33984 + colorTextureImageUnit[i]);
               GL11.glTexParameteri(3553, 10241, 9987);
               GL30.glGenerateMipmap(3553);
            }
         }

         GlStateManager.activeTexture(33984);
      }
   }

   public static void drawComposite() {
      GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
      drawCompositeQuad();
      int countInstances = activeProgram.getCountInstances();
      if (countInstances > 1) {
         for (int i = 1; i < countInstances; i++) {
            uniform_instanceId.setValue(i);
            drawCompositeQuad();
         }

         uniform_instanceId.setValue(0);
      }
   }

   private static void drawCompositeQuad() {
      if (!canRenderQuads()) {
         GL11.glBegin(5);
         GL11.glTexCoord2f(0.0F, 0.0F);
         GL11.glVertex3f(0.0F, 0.0F, 0.0F);
         GL11.glTexCoord2f(1.0F, 0.0F);
         GL11.glVertex3f(1.0F, 0.0F, 0.0F);
         GL11.glTexCoord2f(0.0F, 1.0F);
         GL11.glVertex3f(0.0F, 1.0F, 0.0F);
         GL11.glTexCoord2f(1.0F, 1.0F);
         GL11.glVertex3f(1.0F, 1.0F, 0.0F);
         GL11.glEnd();
      } else {
         GL11.glBegin(7);
         GL11.glTexCoord2f(0.0F, 0.0F);
         GL11.glVertex3f(0.0F, 0.0F, 0.0F);
         GL11.glTexCoord2f(1.0F, 0.0F);
         GL11.glVertex3f(1.0F, 0.0F, 0.0F);
         GL11.glTexCoord2f(1.0F, 1.0F);
         GL11.glVertex3f(1.0F, 1.0F, 0.0F);
         GL11.glTexCoord2f(0.0F, 1.0F);
         GL11.glVertex3f(0.0F, 1.0F, 0.0F);
         GL11.glEnd();
      }
   }

   public static void renderDeferred() {
      if (!isShadowPass) {
         boolean buffersChanged = checkBufferFlip(ProgramDeferredPre);
         if (hasDeferredPrograms) {
            checkGLError("pre-render Deferred");
            renderComposites(ProgramsDeferred, false);
            buffersChanged = true;
         }

         if (buffersChanged) {
            bindGbuffersTextures();

            for (int i = 0; i < usedColorBuffers; i++) {
               EXTFramebufferObject.glFramebufferTexture2DEXT(36160, 36064 + i, 3553, dfbColorTexturesFlip.getA(i), 0);
            }

            if (ProgramWater.getDrawBuffers() != null) {
               setDrawBuffers(ProgramWater.getDrawBuffers());
            } else {
               setDrawBuffers(dfbDrawBuffers);
            }

            GlStateManager.activeTexture(33984);
            mc.getTextureManager().bindTexture(SpriteAtlasTexture.BLOCK_ATLAS_TEX);
         }
      }
   }

   public static void renderCompositeFinal() {
      if (!isShadowPass) {
         checkBufferFlip(ProgramCompositePre);
         checkGLError("pre-render CompositeFinal");
         renderComposites(ProgramsComposite, true);
      }
   }

   private static boolean checkBufferFlip(Program program) {
      boolean flipped = false;
      Boolean[] buffersFlip = program.getBuffersFlip();

      for (int i = 0; i < usedColorBuffers; i++) {
         if (Config.isTrue(buffersFlip[i])) {
            dfbColorTexturesFlip.flip(i);
            flipped = true;
         }
      }

      return flipped;
   }

   private static void renderComposites(Program[] ps, boolean renderFinal) {
      if (!isShadowPass) {
         GL11.glPushMatrix();
         GL11.glLoadIdentity();
         GL11.glMatrixMode(5889);
         GL11.glPushMatrix();
         GL11.glLoadIdentity();
         GL11.glOrtho(0.0, 1.0, 0.0, 1.0, 0.0, 1.0);
         GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
         GlStateManager.enableTexture();
         GlStateManager.disableAlphaTest();
         GlStateManager.disableBlend();
         GlStateManager.enableDepthTest();
         GlStateManager.depthFunc(519);
         GlStateManager.depthMask(false);
         GlStateManager.disableLighting();
         if (usedShadowDepthBuffers >= 1) {
            GlStateManager.activeTexture(33988);
            GlStateManager.bindTexture(sfbDepthTextures.get(0));
            if (usedShadowDepthBuffers >= 2) {
               GlStateManager.activeTexture(33989);
               GlStateManager.bindTexture(sfbDepthTextures.get(1));
            }
         }

         for (int i = 0; i < usedColorBuffers; i++) {
            GlStateManager.activeTexture(33984 + colorTextureImageUnit[i]);
            GlStateManager.bindTexture(dfbColorTexturesFlip.getA(i));
         }

         GlStateManager.activeTexture(33990);
         GlStateManager.bindTexture(dfbDepthTextures.get(0));
         if (usedDepthBuffers >= 2) {
            GlStateManager.activeTexture(33995);
            GlStateManager.bindTexture(dfbDepthTextures.get(1));
            if (usedDepthBuffers >= 3) {
               GlStateManager.activeTexture(33996);
               GlStateManager.bindTexture(dfbDepthTextures.get(2));
            }
         }

         for (int i = 0; i < usedShadowColorBuffers; i++) {
            GlStateManager.activeTexture(33997 + i);
            GlStateManager.bindTexture(sfbColorTextures.get(i));
         }

         if (noiseTextureEnabled) {
            GlStateManager.activeTexture(33984 + noiseTexture.getTextureUnit());
            GlStateManager.bindTexture(noiseTexture.getTextureId());
         }

         if (renderFinal) {
            bindCustomTextures(customTexturesComposite);
         } else {
            bindCustomTextures(customTexturesDeferred);
         }

         GlStateManager.activeTexture(33984);

         for (int i = 0; i < usedColorBuffers; i++) {
            EXTFramebufferObject.glFramebufferTexture2DEXT(36160, 36064 + i, 3553, dfbColorTexturesFlip.getB(i), 0);
         }

         EXTFramebufferObject.glFramebufferTexture2DEXT(36160, 36096, 3553, dfbDepthTextures.get(0), 0);
         GL20.glDrawBuffers(dfbDrawBuffers);
         checkGLError("pre-composite");

         for (int cp = 0; cp < ps.length; cp++) {
            Program program = ps[cp];
            if (program.getId() != 0) {
               useProgram(program);
               checkGLError(program.getName());
               if (activeCompositeMipmapSetting != 0) {
                  genCompositeMipmap();
               }

               preDrawComposite();
               drawComposite();
               postDrawComposite();

               for (int i = 0; i < usedColorBuffers; i++) {
                  if (program.getToggleColorTextures()[i]) {
                     dfbColorTexturesFlip.flip(i);
                     GlStateManager.activeTexture(33984 + colorTextureImageUnit[i]);
                     GlStateManager.bindTexture(dfbColorTexturesFlip.getA(i));
                     EXTFramebufferObject.glFramebufferTexture2DEXT(36160, 36064 + i, 3553, dfbColorTexturesFlip.getB(i), 0);
                  }
               }

               GlStateManager.activeTexture(33984);
            }
         }

         checkGLError("composite");
         if (renderFinal) {
            renderFinal();
            isCompositeRendered = true;
         }

         GlStateManager.enableLighting();
         GlStateManager.enableTexture();
         GlStateManager.enableAlphaTest();
         GlStateManager.enableBlend();
         GlStateManager.depthFunc(515);
         GlStateManager.depthMask(true);
         GL11.glPopMatrix();
         GL11.glMatrixMode(5888);
         GL11.glPopMatrix();
         useProgram(ProgramNone);
      }
   }

   private static void preDrawComposite() {
      RenderScale rs = activeProgram.getRenderScale();
      if (rs != null) {
         int x = (int)((float)renderWidth * rs.getOffsetX());
         int y = (int)((float)renderHeight * rs.getOffsetY());
         int w = (int)((float)renderWidth * rs.getScale());
         int h = (int)((float)renderHeight * rs.getScale());
         GL11.glViewport(x, y, w, h);
      }
   }

   private static void postDrawComposite() {
      RenderScale rs = activeProgram.getRenderScale();
      if (rs != null) {
         GL11.glViewport(0, 0, renderWidth, renderHeight);
      }
   }

   private static void renderFinal() {
      isRenderingDfb = false;
      mc.getFramebuffer().bind(true);
      GLX.advancedFrameBufferTexture2D(GLX.framebuffer, GLX.colorAttachment, 3553, mc.getFramebuffer().colorAttachment, 0);
      GL11.glViewport(0, 0, mc.width, mc.height);
      if (GameRenderer.anaglyphEnabled) {
         boolean maskR = GameRenderer.anaglyphFilter != 0;
         GlStateManager.colorMask(maskR, !maskR, !maskR, true);
      }

      GlStateManager.depthMask(true);
      GL11.glClearColor(clearColorR, clearColorG, clearColorB, 1.0F);
      GL11.glClear(16640);
      GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
      GlStateManager.enableTexture();
      GlStateManager.disableAlphaTest();
      GlStateManager.disableBlend();
      GlStateManager.enableDepthTest();
      GlStateManager.depthFunc(519);
      GlStateManager.depthMask(false);
      checkGLError("pre-final");
      useProgram(ProgramFinal);
      checkGLError("final");
      if (activeCompositeMipmapSetting != 0) {
         genCompositeMipmap();
      }

      drawComposite();
      checkGLError("renderCompositeFinal");
   }

   public static void endRender() {
      if (isShadowPass) {
         checkGLError("shadow endRender");
      } else {
         if (!isCompositeRendered) {
            renderCompositeFinal();
         }

         isRenderingWorld = false;
         GlStateManager.colorMask(true, true, true, true);
         useProgram(ProgramNone);
         DiffuseLighting.disable();
         checkGLError("endRender end");
      }
   }

   public static void beginSky() {
      isRenderingSky = true;
      fogEnabled = true;
      setDrawBuffers(dfbDrawBuffers);
      useProgram(ProgramSkyTextured);
      pushEntity(-2, 0);
   }

   public static void setSkyColor(Vec3d v3color) {
      skyColorR = (float)v3color.x;
      skyColorG = (float)v3color.y;
      skyColorB = (float)v3color.z;
      setProgramUniform3f(uniform_skyColor, skyColorR, skyColorG, skyColorB);
   }

   public static void drawHorizon() {
      BufferBuilder tess = Tessellator.getInstance().getBuffer();
      float farDistance = (float)(mc.options.viewDistance * 16);
      double xzq = (double)farDistance * 0.9238;
      double xzp = (double)farDistance * 0.3826;
      double xzn = -xzp;
      double xzm = -xzq;
      double top = 16.0;
      double bot = -cameraPositionY;
      tess.begin(7, VertexFormats.POSITION);
      tess.vertex(xzn, bot, xzm).next();
      tess.vertex(xzn, top, xzm).next();
      tess.vertex(xzm, top, xzn).next();
      tess.vertex(xzm, bot, xzn).next();
      tess.vertex(xzm, bot, xzn).next();
      tess.vertex(xzm, top, xzn).next();
      tess.vertex(xzm, top, xzp).next();
      tess.vertex(xzm, bot, xzp).next();
      tess.vertex(xzm, bot, xzp).next();
      tess.vertex(xzm, top, xzp).next();
      tess.vertex(xzn, top, xzq).next();
      tess.vertex(xzn, bot, xzq).next();
      tess.vertex(xzn, bot, xzq).next();
      tess.vertex(xzn, top, xzq).next();
      tess.vertex(xzp, top, xzq).next();
      tess.vertex(xzp, bot, xzq).next();
      tess.vertex(xzp, bot, xzq).next();
      tess.vertex(xzp, top, xzq).next();
      tess.vertex(xzq, top, xzp).next();
      tess.vertex(xzq, bot, xzp).next();
      tess.vertex(xzq, bot, xzp).next();
      tess.vertex(xzq, top, xzp).next();
      tess.vertex(xzq, top, xzn).next();
      tess.vertex(xzq, bot, xzn).next();
      tess.vertex(xzq, bot, xzn).next();
      tess.vertex(xzq, top, xzn).next();
      tess.vertex(xzp, top, xzm).next();
      tess.vertex(xzp, bot, xzm).next();
      tess.vertex(xzp, bot, xzm).next();
      tess.vertex(xzp, top, xzm).next();
      tess.vertex(xzn, top, xzm).next();
      tess.vertex(xzn, bot, xzm).next();
      tess.vertex(xzm, bot, xzm).next();
      tess.vertex(xzm, bot, xzq).next();
      tess.vertex(xzq, bot, xzq).next();
      tess.vertex(xzq, bot, xzm).next();
      Tessellator.getInstance().draw();
   }

   public static void preSkyList() {
      setUpPosition();
      GL11.glColor3f(fogColorR, fogColorG, fogColorB);
      drawHorizon();
      GL11.glColor3f(skyColorR, skyColorG, skyColorB);
   }

   public static void endSky() {
      isRenderingSky = false;
      setDrawBuffers(dfbDrawBuffers);
      useProgram(lightmapEnabled ? ProgramTexturedLit : ProgramTextured);
      popEntity();
   }

   public static void beginUpdateChunks() {
      checkGLError("beginUpdateChunks1");
      checkFramebufferStatus("beginUpdateChunks1");
      if (!isShadowPass) {
         useProgram(ProgramTerrain);
      }

      checkGLError("beginUpdateChunks2");
      checkFramebufferStatus("beginUpdateChunks2");
   }

   public static void endUpdateChunks() {
      checkGLError("endUpdateChunks1");
      checkFramebufferStatus("endUpdateChunks1");
      if (!isShadowPass) {
         useProgram(ProgramTerrain);
      }

      checkGLError("endUpdateChunks2");
      checkFramebufferStatus("endUpdateChunks2");
   }

   public static boolean shouldRenderClouds(GameOptions gs) {
      if (!shaderPackLoaded) {
         return true;
      } else {
         checkGLError("shouldRenderClouds");
         return isShadowPass ? configCloudShadow : gs.cloudMode > 0;
      }
   }

   public static void beginClouds() {
      fogEnabled = true;
      pushEntity(-3, 0);
      useProgram(ProgramClouds);
   }

   public static void endClouds() {
      disableFog();
      popEntity();
      useProgram(lightmapEnabled ? ProgramTexturedLit : ProgramTextured);
   }

   public static void beginEntities() {
      if (isRenderingWorld) {
         useProgram(ProgramEntities);
      }
   }

   public static void nextEntity(Entity entity) {
      if (isRenderingWorld) {
         useProgram(ProgramEntities);
         setEntityId(entity);
      }
   }

   public static void setEntityId(Entity entity) {
      if (uniform_entityId.isDefined()) {
         int id = EntityUtils.getEntityIdByClass(entity);
         int idAlias = EntityAliases.getEntityAliasId(id);
         if (idAlias >= 0) {
            id = idAlias;
         }

         uniform_entityId.setValue(id);
      }
   }

   public static void beginSpiderEyes() {
      if (isRenderingWorld && ProgramSpiderEyes.getId() != ProgramNone.getId()) {
         useProgram(ProgramSpiderEyes);
         GlStateManager.enableAlphaTest();
         GlStateManager.alphaFunc(516, 0.0F);
         GlStateManager.blendFunc(770, 771);
      }
   }

   public static void endSpiderEyes() {
      if (isRenderingWorld && ProgramSpiderEyes.getId() != ProgramNone.getId()) {
         useProgram(ProgramEntities);
         GlStateManager.disableAlphaTest();
      }
   }

   public static void endEntities() {
      if (isRenderingWorld) {
         setEntityId(null);
         useProgram(lightmapEnabled ? ProgramTexturedLit : ProgramTextured);
      }
   }

   public static void beginEntitiesGlowing() {
      if (isRenderingWorld) {
         isEntitiesGlowing = true;
      }
   }

   public static void endEntitiesGlowing() {
      if (isRenderingWorld) {
         isEntitiesGlowing = false;
      }
   }

   public static void setEntityColor(float r, float g, float b, float a) {
      if (isRenderingWorld && !isShadowPass) {
         uniform_entityColor.setValue(r, g, b, a);
      }
   }

   public static void beginLivingDamage() {
      if (isRenderingWorld) {
         ShadersTex.bindTexture(defaultTexture);
         if (!isShadowPass) {
            setDrawBuffers(drawBuffersColorAtt0);
         }
      }
   }

   public static void endLivingDamage() {
      if (isRenderingWorld && !isShadowPass) {
         setDrawBuffers(ProgramEntities.getDrawBuffers());
      }
   }

   public static void beginBlockEntities() {
      if (isRenderingWorld) {
         checkGLError("beginBlockEntities");
         useProgram(ProgramBlock);
      }
   }

   public static void nextBlockEntity(BlockEntity tileEntity) {
      if (isRenderingWorld) {
         checkGLError("nextBlockEntity");
         useProgram(ProgramBlock);
         setBlockEntityId(tileEntity);
      }
   }

   public static void setBlockEntityId(BlockEntity tileEntity) {
      if (uniform_blockEntityId.isDefined()) {
         int blockId = getBlockEntityId(tileEntity);
         uniform_blockEntityId.setValue(blockId);
      }
   }

   private static int getBlockEntityId(BlockEntity tileEntity) {
      if (tileEntity == null) {
         return -1;
      } else {
         Block block = tileEntity.getBlock();
         if (block == null) {
            return 0;
         } else {
            int blockId = Block.getIdByBlock(block);
            int metadata = tileEntity.getDataValue();
            int blockAliasId = BlockAliases.getBlockAliasId(blockId, metadata);
            if (blockAliasId >= 0) {
               blockId = blockAliasId;
            }

            return blockId;
         }
      }
   }

   public static void endBlockEntities() {
      if (isRenderingWorld) {
         checkGLError("endBlockEntities");
         setBlockEntityId(null);
         useProgram(lightmapEnabled ? ProgramTexturedLit : ProgramTextured);
         ShadersTex.bindNSTextures(((ExtendedTexture)defaultTexture).getMultiTex());
      }
   }

   public static void beginLitParticles() {
      useProgram(ProgramTexturedLit);
   }

   public static void beginParticles() {
      useProgram(ProgramTextured);
   }

   public static void endParticles() {
      useProgram(ProgramTexturedLit);
   }

   public static void readCenterDepth() {
      if (!isShadowPass && centerDepthSmoothEnabled) {
         ((Buffer)tempDirectFloatBuffer).clear();
         GL11.glReadPixels(renderWidth / 2, renderHeight / 2, 1, 1, 6402, 5126, tempDirectFloatBuffer);
         centerDepth = tempDirectFloatBuffer.get(0);
         float fadeScalar = (float)diffSystemTime * 0.01F;
         float fadeFactor = (float)Math.exp(Math.log(0.5) * (double)fadeScalar / (double)centerDepthSmoothHalflife);
         centerDepthSmooth = centerDepthSmooth * fadeFactor + centerDepth * (1.0F - fadeFactor);
      }
   }

   public static void beginWeather() {
      if (!isShadowPass) {
         if (usedDepthBuffers >= 3) {
            GlStateManager.activeTexture(33996);
            GL11.glCopyTexSubImage2D(3553, 0, 0, 0, 0, 0, renderWidth, renderHeight);
            GlStateManager.activeTexture(33984);
         }

         GlStateManager.enableDepthTest();
         GlStateManager.enableBlend();
         GlStateManager.blendFunc(770, 771);
         GlStateManager.enableAlphaTest();
         useProgram(ProgramWeather);
      }
   }

   public static void endWeather() {
      GlStateManager.disableBlend();
      useProgram(ProgramTexturedLit);
   }

   public static void preWater() {
      if (usedDepthBuffers >= 2) {
         GlStateManager.activeTexture(33995);
         checkGLError("pre copy depth");
         GL11.glCopyTexSubImage2D(3553, 0, 0, 0, 0, 0, renderWidth, renderHeight);
         checkGLError("copy depth");
         GlStateManager.activeTexture(33984);
      }

      ShadersTex.bindNSTextures(((ExtendedTexture)defaultTexture).getMultiTex());
   }

   public static void beginWater() {
      if (isRenderingWorld) {
         if (!isShadowPass) {
            renderDeferred();
            useProgram(ProgramWater);
            GlStateManager.enableBlend();
            GlStateManager.depthMask(true);
         } else {
            GlStateManager.depthMask(true);
         }
      }
   }

   public static void endWater() {
      if (isRenderingWorld) {
         if (isShadowPass) {
         }

         useProgram(lightmapEnabled ? ProgramTexturedLit : ProgramTextured);
      }
   }

   public static void applyHandDepth() {
      if ((double)configHandDepthMul != 1.0) {
         GL11.glScaled(1.0, 1.0, (double)configHandDepthMul);
      }
   }

   public static void beginHand(boolean translucent) {
      GL11.glMatrixMode(5888);
      GL11.glPushMatrix();
      GL11.glMatrixMode(5889);
      GL11.glPushMatrix();
      GL11.glMatrixMode(5888);
      if (translucent) {
         useProgram(ProgramHandWater);
      } else {
         useProgram(ProgramHand);
      }

      checkGLError("beginHand");
      checkFramebufferStatus("beginHand");
   }

   public static void endHand() {
      checkGLError("pre endHand");
      checkFramebufferStatus("pre endHand");
      GL11.glMatrixMode(5889);
      GL11.glPopMatrix();
      GL11.glMatrixMode(5888);
      GL11.glPopMatrix();
      GlStateManager.blendFunc(770, 771);
      checkGLError("endHand");
   }

   public static void beginFPOverlay() {
      GlStateManager.disableLighting();
      GlStateManager.disableBlend();
   }

   public static void endFPOverlay() {
   }

   public static void glEnableWrapper(int cap) {
      GL11.glEnable(cap);
      if (cap == 3553) {
         enableTexture2D();
      } else if (cap == 2912) {
         enableFog();
      }
   }

   public static void glDisableWrapper(int cap) {
      GL11.glDisable(cap);
      if (cap == 3553) {
         disableTexture2D();
      } else if (cap == 2912) {
         disableFog();
      }
   }

   public static void sglEnableT2D(int cap) {
      GL11.glEnable(cap);
      enableTexture2D();
   }

   public static void sglDisableT2D(int cap) {
      GL11.glDisable(cap);
      disableTexture2D();
   }

   public static void sglEnableFog(int cap) {
      GL11.glEnable(cap);
      enableFog();
   }

   public static void sglDisableFog(int cap) {
      GL11.glDisable(cap);
      disableFog();
   }

   public static void enableTexture2D() {
      if (isRenderingSky) {
         useProgram(ProgramSkyTextured);
      } else if (activeProgram == ProgramBasic) {
         useProgram(lightmapEnabled ? ProgramTexturedLit : ProgramTextured);
      }
   }

   public static void disableTexture2D() {
      if (isRenderingSky) {
         useProgram(ProgramSkyBasic);
      } else if (activeProgram == ProgramTextured || activeProgram == ProgramTexturedLit) {
         useProgram(ProgramBasic);
      }
   }

   public static void pushProgram() {
      programStack.push(activeProgram);
   }

   public static void popProgram() {
      Program program = programStack.pop();
      useProgram(program);
   }

   public static void beginLeash() {
      pushProgram();
      useProgram(ProgramBasic);
   }

   public static void endLeash() {
      popProgram();
   }

   public static void enableFog() {
      fogEnabled = true;
      setProgramUniform1i(uniform_fogMode, fogMode);
      setProgramUniform1f(uniform_fogDensity, fogDensity);
   }

   public static void disableFog() {
      fogEnabled = false;
      setProgramUniform1i(uniform_fogMode, 0);
   }

   public static void setFogDensity(float value) {
      fogDensity = value;
      if (fogEnabled) {
         setProgramUniform1f(uniform_fogDensity, value);
      }
   }

   public static void sglFogi(int pname, int param) {
      GL11.glFogi(pname, param);
      if (pname == 2917) {
         fogMode = param;
         if (fogEnabled) {
            setProgramUniform1i(uniform_fogMode, fogMode);
         }
      }
   }

   public static void enableLightmap() {
      lightmapEnabled = true;
      if (activeProgram == ProgramTextured) {
         useProgram(ProgramTexturedLit);
      }
   }

   public static void disableLightmap() {
      lightmapEnabled = false;
      if (activeProgram == ProgramTexturedLit) {
         useProgram(ProgramTextured);
      }
   }

   public static int getEntityData() {
      return entityData[entityDataIndex * 2];
   }

   public static int getEntityData2() {
      return entityData[entityDataIndex * 2 + 1];
   }

   public static int setEntityData1(int data1) {
      entityData[entityDataIndex * 2] = entityData[entityDataIndex * 2] & 65535 | data1 << 16;
      return data1;
   }

   public static int setEntityData2(int data2) {
      entityData[entityDataIndex * 2 + 1] = entityData[entityDataIndex * 2 + 1] & -65536 | data2 & 65535;
      return data2;
   }

   public static void pushEntity(int data0, int data1) {
      entityDataIndex++;
      entityData[entityDataIndex * 2] = data0 & 65535 | data1 << 16;
      entityData[entityDataIndex * 2 + 1] = 0;
   }

   public static void pushEntity(int data0) {
      entityDataIndex++;
      entityData[entityDataIndex * 2] = data0 & 65535;
      entityData[entityDataIndex * 2 + 1] = 0;
   }

   public static void pushEntity(Block block) {
      entityDataIndex++;
      int blockRenderType = block.getBlockType();
      entityData[entityDataIndex * 2] = Block.REGISTRY.getRawId(block) & 65535 | blockRenderType << 16;
      entityData[entityDataIndex * 2 + 1] = 0;
   }

   public static void popEntity() {
      entityData[entityDataIndex * 2] = 0;
      entityData[entityDataIndex * 2 + 1] = 0;
      entityDataIndex--;
   }

   public static void mcProfilerEndSection() {
      mc.profiler.pop();
   }

   public static String getShaderPackName() {
      if (shaderPack == null) {
         return null;
      } else {
         return shaderPack instanceof ShaderPackNone ? null : shaderPack.getName();
      }
   }

   public static InputStream getShaderPackResourceStream(String path) {
      return shaderPack == null ? null : shaderPack.getResourceAsStream(path);
   }

   public static void nextAntialiasingLevel(boolean forward) {
      if (forward) {
         configAntialiasingLevel += 2;
         if (configAntialiasingLevel > 4) {
            configAntialiasingLevel = 0;
         }
      } else {
         configAntialiasingLevel -= 2;
         if (configAntialiasingLevel < 0) {
            configAntialiasingLevel = 4;
         }
      }

      configAntialiasingLevel = configAntialiasingLevel / 2 * 2;
      configAntialiasingLevel = NumUtils.limit(configAntialiasingLevel, 0, 4);
   }

   public static void checkShadersModInstalled() {
      try {
         Class cls = Class.forName("shadersmod.transform.SMCClassTransformer");
      } catch (Throwable var1) {
         return;
      }

      throw new RuntimeException("Shaders Mod detected. Please remove it, OptiFine has built-in support for shaders.");
   }

   public static void resourcesReloaded() {
      loadShaderPackResources();
      if (shaderPackLoaded) {
         BlockAliases.resourcesReloaded();
         ItemAliases.resourcesReloaded();
         EntityAliases.resourcesReloaded();
      }
   }

   private static void loadShaderPackResources() {
      shaderPackResources = new HashMap<>();
      if (shaderPackLoaded) {
         List<String> listFiles = new ArrayList<>();
         String PREFIX = "/shaders/lang/";
         String EN_US = "en_US";
         String SUFFIX = ".lang";
         listFiles.add(PREFIX + EN_US + SUFFIX);
         if (!Config.getGameSettings().language.equals(EN_US)) {
            listFiles.add(PREFIX + Config.getGameSettings().language + SUFFIX);
         }

         try {
            for (String file : listFiles) {
               InputStream in = shaderPack.getResourceAsStream(file);
               if (in != null) {
                  Properties props = new PropertiesOrdered();
                  Lang.loadLocaleData(in, props);
                  in.close();

                  for (String key : props.keySet().toArray(new String[0])) {
                     String value = props.getProperty(key);
                     shaderPackResources.put(key, value);
                  }
               }
            }
         } catch (IOException var12) {
            var12.printStackTrace();
         }
      }
   }

   public static String translate(String key, String def) {
      String str = shaderPackResources.get(key);
      return str == null ? def : str;
   }

   public static boolean isProgramPath(String path) {
      if (path == null) {
         return false;
      } else if (path.length() <= 0) {
         return false;
      } else {
         int pos = path.lastIndexOf("/");
         if (pos >= 0) {
            path = path.substring(pos + 1);
         }

         Program p = getProgram(path);
         return p != null;
      }
   }

   public static Program getProgram(String name) {
      return programs.getProgram(name);
   }

   public static void setItemToRenderMain(ItemStack itemToRenderMain) {
      itemToRenderMainTranslucent = isTranslucentBlock(itemToRenderMain);
   }

   public static void setItemToRenderOff(ItemStack itemToRenderOff) {
      itemToRenderOffTranslucent = isTranslucentBlock(itemToRenderOff);
   }

   public static boolean isItemToRenderMainTranslucent() {
      return itemToRenderMainTranslucent;
   }

   public static boolean isItemToRenderOffTranslucent() {
      return itemToRenderOffTranslucent;
   }

   public static boolean isBothHandsRendered() {
      return isHandRenderedMain && isHandRenderedOff;
   }

   private static boolean isTranslucentBlock(ItemStack stack) {
      if (stack == null) {
         return false;
      } else {
         Item item = stack.getItem();
         if (item == null) {
            return false;
         } else if (!(item instanceof BlockItem)) {
            return false;
         } else {
            BlockItem itemBlock = (BlockItem)item;
            Block block = itemBlock.getBlock();
            if (block == null) {
               return false;
            } else {
               RenderLayer blockRenderLayer = block.getRenderLayerType();
               return blockRenderLayer == RenderLayer.TRANSLUCENT;
            }
         }
      }
   }

   public static boolean isSkipRenderHand() {
      return skipRenderHandMain;
   }

   public static boolean isRenderBothHands() {
      return !skipRenderHandMain && !skipRenderHandOff;
   }

   public static void setSkipRenderHands(boolean skipMain, boolean skipOff) {
      skipRenderHandMain = skipMain;
      skipRenderHandOff = skipOff;
   }

   public static void setHandsRendered(boolean handMain, boolean handOff) {
      isHandRenderedMain = handMain;
      isHandRenderedOff = handOff;
   }

   public static boolean isHandRenderedMain() {
      return isHandRenderedMain;
   }

   public static boolean isHandRenderedOff() {
      return isHandRenderedOff;
   }

   public static float getShadowRenderDistance() {
      return shadowDistanceRenderMul < 0.0F ? -1.0F : shadowMapHalfPlane * shadowDistanceRenderMul;
   }

   public static void setRenderingFirstPersonHand(boolean flag) {
      isRenderingFirstPersonHand = flag;
   }

   public static boolean isRenderingFirstPersonHand() {
      return isRenderingFirstPersonHand;
   }

   public static void beginBeacon() {
      if (isRenderingWorld) {
         useProgram(ProgramBeaconBeam);
      }
   }

   public static void endBeacon() {
      if (isRenderingWorld) {
         useProgram(ProgramBlock);
      }
   }

   public static World getCurrentWorld() {
      return currentWorld;
   }

   public static BlockPos getCameraPosition() {
      return new BlockPos(cameraPositionX, cameraPositionY, cameraPositionZ);
   }

   public static boolean isCustomUniforms() {
      return customUniforms != null;
   }

   public static boolean canRenderQuads() {
      return hasGeometryShaders ? capabilities.GL_NV_geometry_shader4 : true;
   }
}
