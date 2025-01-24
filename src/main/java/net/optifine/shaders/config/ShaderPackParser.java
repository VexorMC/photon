package net.optifine.shaders.config;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.CharArrayReader;
import java.io.CharArrayWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import dev.vexor.photon.Config;
import dev.vexor.photon.Photon;
import net.optifine.expr.ExpressionFloatArrayCached;
import net.optifine.expr.ExpressionFloatCached;
import net.optifine.expr.ExpressionParser;
import net.optifine.expr.ExpressionType;
import net.optifine.expr.IExpression;
import net.optifine.expr.IExpressionBool;
import net.optifine.expr.IExpressionFloat;
import net.optifine.expr.IExpressionFloatArray;
import net.optifine.expr.ParseException;
import net.optifine.render.GlAlphaState;
import net.optifine.render.GlBlendState;
import net.optifine.shaders.IShaderPack;
import net.optifine.shaders.Program;
import net.optifine.shaders.SMCLog;
import net.optifine.shaders.ShaderUtils;
import net.optifine.shaders.Shaders;
import net.optifine.shaders.uniform.CustomUniform;
import net.optifine.shaders.uniform.CustomUniforms;
import net.optifine.shaders.uniform.ShaderExpressionResolver;
import net.optifine.shaders.uniform.UniformType;
import net.optifine.util.StrUtils;

public class ShaderPackParser {
   private static final Pattern PATTERN_VERSION = Pattern.compile("^\\s*#version\\s+.*$");
   private static final Pattern PATTERN_INCLUDE = Pattern.compile("^\\s*#include\\s+\"([A-Za-z0-9_/\\.]+)\".*$");
   private static final Set<String> setConstNames = makeSetConstNames();
   private static final Map<String, Integer> mapAlphaFuncs = makeMapAlphaFuncs();
   private static final Map<String, Integer> mapBlendFactors = makeMapBlendFactors();

   public static ShaderOption[] parseShaderPackOptions(IShaderPack shaderPack, String[] programNames, List<Integer> listDimensions) {
      if (shaderPack == null) {
         return new ShaderOption[0];
      } else {
         Map<String, ShaderOption> mapOptions = new HashMap<>();
         collectShaderOptions(shaderPack, "/shaders", programNames, mapOptions);

         for (int dimId : listDimensions) {
            String dirWorld = "/shaders/world" + dimId;
            collectShaderOptions(shaderPack, dirWorld, programNames, mapOptions);
         }

         Collection<ShaderOption> options = mapOptions.values();
         ShaderOption[] sos = options.toArray(new ShaderOption[options.size()]);
         Comparator<ShaderOption> comp = new Comparator<ShaderOption>() {
            public int compare(ShaderOption o1, ShaderOption o2) {
               return o1.getName().compareToIgnoreCase(o2.getName());
            }
         };
         Arrays.sort(sos, comp);
         return sos;
      }
   }

   private static void collectShaderOptions(IShaderPack shaderPack, String dir, String[] programNames, Map<String, ShaderOption> mapOptions) {
      for (int i = 0; i < programNames.length; i++) {
         String programName = programNames[i];
         if (!programName.equals("")) {
            String vsh = dir + "/" + programName + ".vsh";
            String fsh = dir + "/" + programName + ".fsh";
            collectShaderOptions(shaderPack, vsh, mapOptions);
            collectShaderOptions(shaderPack, fsh, mapOptions);
         }
      }
   }

   private static void collectShaderOptions(IShaderPack sp, String path, Map<String, ShaderOption> mapOptions) {
      String[] lines = getLines(sp, path);

      for (int i = 0; i < lines.length; i++) {
         String line = lines[i];
         ShaderOption so = getShaderOption(line, path);
         if (so != null && !so.getName().startsWith(ShaderMacros.getPrefixMacro()) && (!so.checkUsed() || isOptionUsed(so, lines))) {
            String key = so.getName();
            ShaderOption so2 = mapOptions.get(key);
            if (so2 != null) {
               if (!Config.equals(so2.getValueDefault(), so.getValueDefault())) {
                  Photon.LOGGER.warn("Ambiguous shader option: " + so.getName());
                  Photon.LOGGER.warn(" - in " + Config.arrayToString((Object[])so2.getPaths()) + ": " + so2.getValueDefault());
                  Photon.LOGGER.warn(" - in " + Config.arrayToString((Object[])so.getPaths()) + ": " + so.getValueDefault());
                  so2.setEnabled(false);
               }

               if (so2.getDescription() == null || so2.getDescription().length() <= 0) {
                  so2.setDescription(so.getDescription());
               }

               so2.addPaths(so.getPaths());
            } else {
               mapOptions.put(key, so);
            }
         }
      }
   }

   private static boolean isOptionUsed(ShaderOption so, String[] lines) {
      for (int i = 0; i < lines.length; i++) {
         String line = lines[i];
         if (so.isUsedInLine(line)) {
            return true;
         }
      }

      return false;
   }

   private static String[] getLines(IShaderPack sp, String path) {
      try {
         List<String> listFiles = new ArrayList<>();
         String str = loadFile(path, sp, 0, listFiles, 0);
         if (str == null) {
            return new String[0];
         } else {
            ByteArrayInputStream is = new ByteArrayInputStream(str.getBytes());
            return Config.readLines(is);
         }
      } catch (IOException var6) {
         Photon.LOGGER.debug(var6.getClass().getName() + ": " + var6.getMessage());
         return new String[0];
      }
   }

   private static ShaderOption getShaderOption(String line, String path) {
      ShaderOption so = null;
      if (so == null) {
         so = ShaderOptionSwitch.parseOption(line, path);
      }

      if (so == null) {
         so = ShaderOptionVariable.parseOption(line, path);
      }

      if (so != null) {
         return so;
      } else {
         if (so == null) {
            so = ShaderOptionSwitchConst.parseOption(line, path);
         }

         if (so == null) {
            so = ShaderOptionVariableConst.parseOption(line, path);
         }

         return so != null && setConstNames.contains(so.getName()) ? so : null;
      }
   }

   private static Set<String> makeSetConstNames() {
      Set<String> set = new HashSet<>();
      set.add("shadowMapResolution");
      set.add("shadowMapFov");
      set.add("shadowDistance");
      set.add("shadowDistanceRenderMul");
      set.add("shadowIntervalSize");
      set.add("generateShadowMipmap");
      set.add("generateShadowColorMipmap");
      set.add("shadowHardwareFiltering");
      set.add("shadowHardwareFiltering0");
      set.add("shadowHardwareFiltering1");
      set.add("shadowtex0Mipmap");
      set.add("shadowtexMipmap");
      set.add("shadowtex1Mipmap");
      set.add("shadowcolor0Mipmap");
      set.add("shadowColor0Mipmap");
      set.add("shadowcolor1Mipmap");
      set.add("shadowColor1Mipmap");
      set.add("shadowtex0Nearest");
      set.add("shadowtexNearest");
      set.add("shadow0MinMagNearest");
      set.add("shadowtex1Nearest");
      set.add("shadow1MinMagNearest");
      set.add("shadowcolor0Nearest");
      set.add("shadowColor0Nearest");
      set.add("shadowColor0MinMagNearest");
      set.add("shadowcolor1Nearest");
      set.add("shadowColor1Nearest");
      set.add("shadowColor1MinMagNearest");
      set.add("wetnessHalflife");
      set.add("drynessHalflife");
      set.add("eyeBrightnessHalflife");
      set.add("centerDepthHalflife");
      set.add("sunPathRotation");
      set.add("ambientOcclusionLevel");
      set.add("superSamplingLevel");
      set.add("noiseTextureResolution");
      return set;
   }

   public static ShaderProfile[] parseProfiles(Properties props, ShaderOption[] shaderOptions) {
      String PREFIX_PROFILE = "profile.";
      List<ShaderProfile> list = new ArrayList<>();

      for (String key : props.keySet().toArray(new String[0])) {
         if (key.startsWith(PREFIX_PROFILE)) {
            String name = key.substring(PREFIX_PROFILE.length());
            String val = props.getProperty(key);
            Set<String> parsedProfiles = new HashSet<>();
            ShaderProfile p = parseProfile(name, props, parsedProfiles, shaderOptions);
            if (p != null) {
               list.add(p);
            }
         }
      }

      return list.size() <= 0 ? null : list.toArray(new ShaderProfile[list.size()]);
   }

   public static Map<String, IExpressionBool> parseProgramConditions(Properties props, ShaderOption[] shaderOptions) {
      String PREFIX_PROGRAM = "program.";
      Pattern pattern = Pattern.compile("program\\.([^.]+)\\.enabled");
      Map<String, IExpressionBool> map = new HashMap<>();

      for (String key : props.keySet().toArray(new String[0])) {
         Matcher matcher = pattern.matcher(key);
         if (matcher.matches()) {
            String name = matcher.group(1);
            String val = props.getProperty(key).trim();
            IExpressionBool expr = parseOptionExpression(val, shaderOptions);
            if (expr == null) {
               SMCLog.severe("Error parsing program condition: " + key);
            } else {
               map.put(name, expr);
            }
         }
      }

      return map;
   }

   private static IExpressionBool parseOptionExpression(String val, ShaderOption[] shaderOptions) {
      try {
         ShaderOptionResolver sor = new ShaderOptionResolver(shaderOptions);
         ExpressionParser parser = new ExpressionParser(sor);
         return parser.parseBool(val);
      } catch (ParseException var5) {
         SMCLog.warning(var5.getClass().getName() + ": " + var5.getMessage());
         return null;
      }
   }

   public static Set<String> parseOptionSliders(Properties props, ShaderOption[] shaderOptions) {
      Set<String> sliders = new HashSet<>();
      String value = props.getProperty("sliders");
      if (value == null) {
         return sliders;
      } else {
         String[] names = Config.tokenize(value, " ");

         for (int i = 0; i < names.length; i++) {
            String name = names[i];
            ShaderOption so = ShaderUtils.getShaderOption(name, shaderOptions);
            if (so == null) {
               Photon.LOGGER.warn("Invalid shader option: " + name);
            } else {
               sliders.add(name);
            }
         }

         return sliders;
      }
   }

   private static ShaderProfile parseProfile(String name, Properties props, Set<String> parsedProfiles, ShaderOption[] shaderOptions) {
      String PREFIX_PROFILE = "profile.";
      String key = PREFIX_PROFILE + name;
      if (parsedProfiles.contains(key)) {
         Photon.LOGGER.warn("[Shaders] Profile already parsed: " + name);
         return null;
      } else {
         parsedProfiles.add(name);
         ShaderProfile prof = new ShaderProfile(name);
         String val = props.getProperty(key);
         String[] parts = Config.tokenize(val, " ");

         for (int i = 0; i < parts.length; i++) {
            String part = parts[i];
            if (part.startsWith(PREFIX_PROFILE)) {
               String nameParent = part.substring(PREFIX_PROFILE.length());
               ShaderProfile profParent = parseProfile(nameParent, props, parsedProfiles, shaderOptions);
               if (prof != null) {
                  prof.addOptionValues(profParent);
                  prof.addDisabledPrograms(profParent.getDisabledPrograms());
               }
            } else {
               String[] tokens = Config.tokenize(part, ":=");
               if (tokens.length == 1) {
                  String option = tokens[0];
                  boolean on = true;
                  if (option.startsWith("!")) {
                     on = false;
                     option = option.substring(1);
                  }

                  String PREFIX_PROGRAM = "program.";
                  if (option.startsWith(PREFIX_PROGRAM)) {
                     String program = option.substring(PREFIX_PROGRAM.length());
                     if (!Shaders.isProgramPath(program)) {
                        Photon.LOGGER.warn("Invalid program: " + program + " in profile: " + prof.getName());
                     } else if (on) {
                        prof.removeDisabledProgram(program);
                     } else {
                        prof.addDisabledProgram(program);
                     }
                  } else {
                     ShaderOption so = ShaderUtils.getShaderOption(option, shaderOptions);
                     if (!(so instanceof ShaderOptionSwitch)) {
                        Photon.LOGGER.warn("[Shaders] Invalid option: " + option);
                     } else {
                        prof.addOptionValue(option, String.valueOf(on));
                        so.setVisible(true);
                     }
                  }
               } else if (tokens.length != 2) {
                  Photon.LOGGER.warn("[Shaders] Invalid option value: " + part);
               } else {
                  String optionx = tokens[0];
                  String value = tokens[1];
                  ShaderOption so = ShaderUtils.getShaderOption(optionx, shaderOptions);
                  if (so == null) {
                     Photon.LOGGER.warn("[Shaders] Invalid option: " + part);
                  } else if (!so.isValidValue(value)) {
                     Photon.LOGGER.warn("[Shaders] Invalid value: " + part);
                  } else {
                     so.setVisible(true);
                     prof.addOptionValue(optionx, value);
                  }
               }
            }
         }

         return prof;
      }
   }

   public static Map<String, ScreenShaderOptions> parseGuiScreens(Properties props, ShaderProfile[] shaderProfiles, ShaderOption[] shaderOptions) {
      Map<String, ScreenShaderOptions> map = new HashMap<>();
      parseGuiScreen("screen", props, map, shaderProfiles, shaderOptions);
      return map.isEmpty() ? null : map;
   }

   private static boolean parseGuiScreen(
      String key, Properties props, Map<String, ScreenShaderOptions> map, ShaderProfile[] shaderProfiles, ShaderOption[] shaderOptions
   ) {
      String val = props.getProperty(key);
      if (val == null) {
         return false;
      } else {
         List<ShaderOption> list = new ArrayList<>();
         Set<String> setNames = new HashSet<>();
         String[] opNames = Config.tokenize(val, " ");

         for (int i = 0; i < opNames.length; i++) {
            String opName = opNames[i];
            if (opName.equals("<empty>")) {
               list.add(null);
            } else if (setNames.contains(opName)) {
               Photon.LOGGER.warn("[Shaders] Duplicate option: " + opName + ", key: " + key);
            } else {
               setNames.add(opName);
               if (opName.equals("<profile>")) {
                  if (shaderProfiles == null) {
                     Photon.LOGGER.warn("[Shaders] Option profile can not be used, no profiles defined: " + opName + ", key: " + key);
                  } else {
                     ShaderOptionProfile optionProfile = new ShaderOptionProfile(shaderProfiles, shaderOptions);
                     list.add(optionProfile);
                  }
               } else if (opName.equals("*")) {
                  ShaderOption soRest = new ShaderOptionRest("<rest>");
                  list.add(soRest);
               } else if (opName.startsWith("[") && opName.endsWith("]")) {
                  String screen = StrUtils.removePrefixSuffix(opName, "[", "]");
                  if (!screen.matches("^[a-zA-Z0-9_]+$")) {
                     Photon.LOGGER.warn("[Shaders] Invalid screen: " + opName + ", key: " + key);
                  } else if (!parseGuiScreen("screen." + screen, props, map, shaderProfiles, shaderOptions)) {
                     Photon.LOGGER.warn("[Shaders] Invalid screen: " + opName + ", key: " + key);
                  } else {
                     ShaderOptionScreen optionScreen = new ShaderOptionScreen(screen);
                     list.add(optionScreen);
                  }
               } else {
                  ShaderOption so = ShaderUtils.getShaderOption(opName, shaderOptions);
                  if (so == null) {
                     Photon.LOGGER.warn("[Shaders] Invalid option: " + opName + ", key: " + key);
                     list.add(null);
                  } else {
                     so.setVisible(true);
                     list.add(so);
                  }
               }
            }
         }

         ShaderOption[] scrOps = list.toArray(new ShaderOption[list.size()]);
         String colStr = props.getProperty(key + ".columns");
         int columns = Config.parseInt(colStr, 2);
         ScreenShaderOptions sso = new ScreenShaderOptions(key, scrOps, columns);
         map.put(key, sso);
         return true;
      }
   }

   public static BufferedReader resolveIncludes(
      BufferedReader reader, String filePath, IShaderPack shaderPack, int fileIndex, List<String> listFiles, int includeLevel
   ) throws IOException {
      String fileDir = "/";
      int pos = filePath.lastIndexOf("/");
      if (pos >= 0) {
         fileDir = filePath.substring(0, pos);
      }

      CharArrayWriter caw = new CharArrayWriter();
      int macroInsertPosition = -1;
      Set<ShaderMacro> setCustomMacros = new LinkedHashSet<>();
      int lineNumber = 1;

      while (true) {
         String line = reader.readLine();
         if (line == null) {
            char[] chars = caw.toCharArray();
            if (macroInsertPosition >= 0 && setCustomMacros.size() > 0) {
               StringBuilder sb = new StringBuilder();

               for (ShaderMacro macro : setCustomMacros) {
                  sb.append("#define ");
                  sb.append(macro.getName());
                  sb.append(" ");
                  sb.append(macro.getValue());
                  sb.append("\n");
               }

               String strCustom = sb.toString();
               StringBuilder sbAll = new StringBuilder(new String(chars));
               sbAll.insert(macroInsertPosition, strCustom);
               String strAll = sbAll.toString();
               chars = strAll.toCharArray();
            }

            CharArrayReader car = new CharArrayReader(chars);
            return new BufferedReader(car);
         }

         if (macroInsertPosition < 0) {
            Matcher mv = PATTERN_VERSION.matcher(line);
            if (mv.matches()) {
               String strDef = ShaderMacros.getFixedMacroLines() + ShaderMacros.getOptionMacroLines();
               String lineA = line + "\n" + strDef;
               String lineB = "#line " + (lineNumber + 1) + " " + fileIndex;
               line = lineA + lineB;
               macroInsertPosition = caw.size() + lineA.length();
            }
         }

         Matcher mi = PATTERN_INCLUDE.matcher(line);
         if (mi.matches()) {
            String fileInc = mi.group(1);
            boolean absolute = fileInc.startsWith("/");
            String filePathInc = absolute ? "/shaders" + fileInc : fileDir + "/" + fileInc;
            if (!listFiles.contains(filePathInc)) {
               listFiles.add(filePathInc);
            }

            int includeFileIndex = listFiles.indexOf(filePathInc) + 1;
            line = loadFile(filePathInc, shaderPack, includeFileIndex, listFiles, includeLevel);
            if (line == null) {
               throw new IOException("Included file not found: " + filePath);
            }

            if (line.endsWith("\n")) {
               line = line.substring(0, line.length() - 1);
            }

            String lineIncludeStart = "#line 1 " + includeFileIndex + "\n";
            if (line.startsWith("#version ")) {
               lineIncludeStart = "";
            }

            line = lineIncludeStart + line + "\n" + "#line " + (lineNumber + 1) + " " + fileIndex;
         }

         if (macroInsertPosition >= 0 && line.contains(ShaderMacros.getPrefixMacro())) {
            ShaderMacro[] lineExts = findMacros(line, ShaderMacros.getExtensions());

            for (int i = 0; i < lineExts.length; i++) {
               ShaderMacro ext = lineExts[i];
               setCustomMacros.add(ext);
            }
         }

         caw.write(line);
         caw.write("\n");
         lineNumber++;
      }
   }

   private static ShaderMacro[] findMacros(String line, ShaderMacro[] macros) {
      List<ShaderMacro> list = new ArrayList<>();

      for (int i = 0; i < macros.length; i++) {
         ShaderMacro ext = macros[i];
         if (line.contains(ext.getName())) {
            list.add(ext);
         }
      }

      return list.toArray(new ShaderMacro[list.size()]);
   }

   private static String loadFile(String filePath, IShaderPack shaderPack, int fileIndex, List<String> listFiles, int includeLevel) throws IOException {
      if (includeLevel >= 10) {
         throw new IOException("#include depth exceeded: " + includeLevel + ", file: " + filePath);
      } else {
         includeLevel++;
         InputStream in = shaderPack.getResourceAsStream(filePath);
         if (in == null) {
            return null;
         } else {
            InputStreamReader isr = new InputStreamReader(in, "ASCII");
            BufferedReader br = new BufferedReader(isr);
            br = resolveIncludes(br, filePath, shaderPack, fileIndex, listFiles, includeLevel);
            CharArrayWriter caw = new CharArrayWriter();

            while (true) {
               String line = br.readLine();
               if (line == null) {
                  return caw.toString();
               }

               caw.write(line);
               caw.write("\n");
            }
         }
      }
   }

   public static CustomUniforms parseCustomUniforms(Properties props) {
      String UNIFORM = "uniform";
      String VARIABLE = "variable";
      String PREFIX_UNIFORM = UNIFORM + ".";
      String PREFIX_VARIABLE = VARIABLE + ".";
      Map<String, IExpression> mapExpressions = new HashMap<>();
      List<CustomUniform> listUniforms = new ArrayList<>();

      for (String key : props.keySet().toArray(new String[0])) {
         String[] keyParts = Config.tokenize(key, ".");
         if (keyParts.length == 3) {
            String kind = keyParts[0];
            String type = keyParts[1];
            String name = keyParts[2];
            String src = props.getProperty(key).trim();
            if (mapExpressions.containsKey(name)) {
               SMCLog.warning("Expression already defined: " + name);
            } else if (kind.equals(UNIFORM) || kind.equals(VARIABLE)) {
               SMCLog.info("Custom " + kind + ": " + name);
               CustomUniform cu = parseCustomUniform(kind, name, type, src, mapExpressions);
               if (cu != null) {
                  mapExpressions.put(name, cu.getExpression());
                  if (!kind.equals(VARIABLE)) {
                     listUniforms.add(cu);
                  }
               }
            }
         }
      }

      if (listUniforms.size() <= 0) {
         return null;
      } else {
         CustomUniform[] cusArr = listUniforms.toArray(new CustomUniform[listUniforms.size()]);
         return new CustomUniforms(cusArr, mapExpressions);
      }
   }

   private static CustomUniform parseCustomUniform(String kind, String name, String type, String src, Map<String, IExpression> mapExpressions) {
      try {
         UniformType uniformType = UniformType.parse(type);
         if (uniformType == null) {
            SMCLog.warning("Unknown " + kind + " type: " + uniformType);
            return null;
         } else {
            ShaderExpressionResolver resolver = new ShaderExpressionResolver(mapExpressions);
            ExpressionParser parser = new ExpressionParser(resolver);
            IExpression expr = parser.parse(src);
            ExpressionType expressionType = expr.getExpressionType();
            if (!uniformType.matchesExpressionType(expressionType)) {
               SMCLog.warning("Expression type does not match " + kind + " type, expression: " + expressionType + ", " + kind + ": " + uniformType + " " + name);
               return null;
            } else {
               expr = makeExpressionCached(expr);
               return new CustomUniform(name, uniformType, expr);
            }
         }
      } catch (ParseException var11) {
         SMCLog.warning(var11.getClass().getName() + ": " + var11.getMessage());
         return null;
      }
   }

   private static IExpression makeExpressionCached(IExpression expr) {
      if (expr instanceof IExpressionFloat) {
         return new ExpressionFloatCached((IExpressionFloat)expr);
      } else {
         return (IExpression)(expr instanceof IExpressionFloatArray ? new ExpressionFloatArrayCached((IExpressionFloatArray)expr) : expr);
      }
   }

   public static void parseAlphaStates(Properties props) {
      for (String key : props.keySet().toArray(new String[0])) {
         String[] keyParts = Config.tokenize(key, ".");
         if (keyParts.length == 2) {
            String type = keyParts[0];
            String programName = keyParts[1];
            if (type.equals("alphaTest")) {
               Program program = Shaders.getProgram(programName);
               if (program == null) {
                  SMCLog.severe("Invalid program name: " + programName);
               } else {
                  String val = props.getProperty(key).trim();
                  GlAlphaState state = parseAlphaState(val);
                  if (state != null) {
                     program.setAlphaState(state);
                  }
               }
            }
         }
      }
   }

   private static GlAlphaState parseAlphaState(String str) {
      String[] parts = Config.tokenize(str, " ");
      if (parts.length == 1) {
         String str0 = parts[0];
         if (str0.equals("off") || str0.equals("false")) {
            return new GlAlphaState(false);
         }
      } else if (parts.length == 2) {
         String str0 = parts[0];
         String str1 = parts[1];
         Integer func = mapAlphaFuncs.get(str0);
         float ref = Config.parseFloat(str1, -1.0F);
         if (func != null && ref >= 0.0F) {
            return new GlAlphaState(true, func, ref);
         }
      }

      SMCLog.severe("Invalid alpha test: " + str);
      return null;
   }

   public static void parseBlendStates(Properties props) {
      for (String key : props.keySet().toArray(new String[0])) {
         String[] keyParts = Config.tokenize(key, ".");
         if (keyParts.length == 2) {
            String type = keyParts[0];
            String programName = keyParts[1];
            if (type.equals("blend")) {
               Program program = Shaders.getProgram(programName);
               if (program == null) {
                  SMCLog.severe("Invalid program name: " + programName);
               } else {
                  String val = props.getProperty(key).trim();
                  GlBlendState state = parseBlendState(val);
                  if (state != null) {
                     program.setBlendState(state);
                  }
               }
            }
         }
      }
   }

   private static GlBlendState parseBlendState(String str) {
      String[] parts = Config.tokenize(str, " ");
      if (parts.length == 1) {
         String str0 = parts[0];
         if (str0.equals("off") || str0.equals("false")) {
            return new GlBlendState(false);
         }
      } else if (parts.length == 2 || parts.length == 4) {
         String str0 = parts[0];
         String str1 = parts[1];
         String str2 = str0;
         String str3 = str1;
         if (parts.length == 4) {
            str2 = parts[2];
            str3 = parts[3];
         }

         Integer src = mapBlendFactors.get(str0);
         Integer dst = mapBlendFactors.get(str1);
         Integer srcAlpha = mapBlendFactors.get(str2);
         Integer dstAlpha = mapBlendFactors.get(str3);
         if (src != null && dst != null && srcAlpha != null && dstAlpha != null) {
            return new GlBlendState(true, src, dst, srcAlpha, dstAlpha);
         }
      }

      SMCLog.severe("Invalid blend mode: " + str);
      return null;
   }

   public static void parseRenderScales(Properties props) {
      for (String key : props.keySet().toArray(new String[0])) {
         String[] keyParts = Config.tokenize(key, ".");
         if (keyParts.length == 2) {
            String type = keyParts[0];
            String programName = keyParts[1];
            if (type.equals("scale")) {
               Program program = Shaders.getProgram(programName);
               if (program == null) {
                  SMCLog.severe("Invalid program name: " + programName);
               } else {
                  String val = props.getProperty(key).trim();
                  RenderScale scale = parseRenderScale(val);
                  if (scale != null) {
                     program.setRenderScale(scale);
                  }
               }
            }
         }
      }
   }

   private static RenderScale parseRenderScale(String str) {
      String[] parts = Config.tokenize(str, " ");
      float scale = Config.parseFloat(parts[0], -1.0F);
      float offsetX = 0.0F;
      float offsetY = 0.0F;
      if (parts.length > 1) {
         if (parts.length != 3) {
            SMCLog.severe("Invalid render scale: " + str);
            return null;
         }

         offsetX = Config.parseFloat(parts[1], -1.0F);
         offsetY = Config.parseFloat(parts[2], -1.0F);
      }

      if (Config.between(scale, 0.0F, 1.0F) && Config.between(offsetX, 0.0F, 1.0F) && Config.between(offsetY, 0.0F, 1.0F)) {
         return new RenderScale(scale, offsetX, offsetY);
      } else {
         SMCLog.severe("Invalid render scale: " + str);
         return null;
      }
   }

   public static void parseBuffersFlip(Properties props) {
      for (String key : props.keySet().toArray(new String[0])) {
         String[] keyParts = Config.tokenize(key, ".");
         if (keyParts.length == 3) {
            String type = keyParts[0];
            String programName = keyParts[1];
            String bufferName = keyParts[2];
            if (type.equals("flip")) {
               Program program = Shaders.getProgram(programName);
               if (program == null) {
                  SMCLog.severe("Invalid program name: " + programName);
               } else {
                  Boolean[] buffersFlip = program.getBuffersFlip();
                  int buffer = Shaders.getBufferIndexFromString(bufferName);
                  if (buffer >= 0 && buffer < buffersFlip.length) {
                     String valStr = props.getProperty(key).trim();
                     Boolean val = Config.parseBoolean(valStr, null);
                     if (val == null) {
                        SMCLog.severe("Invalid boolean value: " + valStr);
                     } else {
                        buffersFlip[buffer] = val;
                     }
                  } else {
                     SMCLog.severe("Invalid buffer name: " + bufferName);
                  }
               }
            }
         }
      }
   }

   private static Map<String, Integer> makeMapAlphaFuncs() {
      Map<String, Integer> map = new HashMap<>();
      map.put("NEVER", (512));
      map.put("LESS", (513));
      map.put("EQUAL", (514));
      map.put("LEQUAL", (515));
      map.put("GREATER", (516));
      map.put("NOTEQUAL", (517));
      map.put("GEQUAL", (518));
      map.put("ALWAYS", (519));
      return Collections.unmodifiableMap(map);
   }

   private static Map<String, Integer> makeMapBlendFactors() {
      Map<String, Integer> map = new HashMap<>();
      map.put("ZERO", (0));
      map.put("ONE", (1));
      map.put("SRC_COLOR", (768));
      map.put("ONE_MINUS_SRC_COLOR", (769));
      map.put("DST_COLOR", (774));
      map.put("ONE_MINUS_DST_COLOR", (775));
      map.put("SRC_ALPHA", (770));
      map.put("ONE_MINUS_SRC_ALPHA", (771));
      map.put("DST_ALPHA", (772));
      map.put("ONE_MINUS_DST_ALPHA", (773));
      map.put("CONSTANT_COLOR", (32769));
      map.put("ONE_MINUS_CONSTANT_COLOR", (32770));
      map.put("CONSTANT_ALPHA", (32771));
      map.put("ONE_MINUS_CONSTANT_ALPHA", (32772));
      map.put("SRC_ALPHA_SATURATE", (776));
      return Collections.unmodifiableMap(map);
   }
}
