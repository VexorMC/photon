package net.optifine.expr;

import java.util.HashMap;
import java.util.Map;

import dev.vexor.photon.Photon;
import net.minecraft.client.MinecraftClient;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.World;
import net.optifine.shaders.uniform.Smoother;

public enum FunctionType {
   PLUS(10, ExpressionType.FLOAT, "+", ExpressionType.FLOAT, ExpressionType.FLOAT),
   MINUS(10, ExpressionType.FLOAT, "-", ExpressionType.FLOAT, ExpressionType.FLOAT),
   MUL(11, ExpressionType.FLOAT, "*", ExpressionType.FLOAT, ExpressionType.FLOAT),
   DIV(11, ExpressionType.FLOAT, "/", ExpressionType.FLOAT, ExpressionType.FLOAT),
   MOD(11, ExpressionType.FLOAT, "%", ExpressionType.FLOAT, ExpressionType.FLOAT),
   NEG(12, ExpressionType.FLOAT, "neg", ExpressionType.FLOAT),
   PI(ExpressionType.FLOAT, "pi"),
   SIN(ExpressionType.FLOAT, "sin", ExpressionType.FLOAT),
   COS(ExpressionType.FLOAT, "cos", ExpressionType.FLOAT),
   ASIN(ExpressionType.FLOAT, "asin", ExpressionType.FLOAT),
   ACOS(ExpressionType.FLOAT, "acos", ExpressionType.FLOAT),
   TAN(ExpressionType.FLOAT, "tan", ExpressionType.FLOAT),
   ATAN(ExpressionType.FLOAT, "atan", ExpressionType.FLOAT),
   ATAN2(ExpressionType.FLOAT, "atan2", ExpressionType.FLOAT, ExpressionType.FLOAT),
   TORAD(ExpressionType.FLOAT, "torad", ExpressionType.FLOAT),
   TODEG(ExpressionType.FLOAT, "todeg", ExpressionType.FLOAT),
   MIN(ExpressionType.FLOAT, "min", new ParametersVariable().first(ExpressionType.FLOAT).repeat(ExpressionType.FLOAT)),
   MAX(ExpressionType.FLOAT, "max", new ParametersVariable().first(ExpressionType.FLOAT).repeat(ExpressionType.FLOAT)),
   CLAMP(ExpressionType.FLOAT, "clamp", ExpressionType.FLOAT, ExpressionType.FLOAT, ExpressionType.FLOAT),
   ABS(ExpressionType.FLOAT, "abs", ExpressionType.FLOAT),
   FLOOR(ExpressionType.FLOAT, "floor", ExpressionType.FLOAT),
   CEIL(ExpressionType.FLOAT, "ceil", ExpressionType.FLOAT),
   EXP(ExpressionType.FLOAT, "exp", ExpressionType.FLOAT),
   FRAC(ExpressionType.FLOAT, "frac", ExpressionType.FLOAT),
   LOG(ExpressionType.FLOAT, "log", ExpressionType.FLOAT),
   POW(ExpressionType.FLOAT, "pow", ExpressionType.FLOAT, ExpressionType.FLOAT),
   RANDOM(ExpressionType.FLOAT, "random"),
   ROUND(ExpressionType.FLOAT, "round", ExpressionType.FLOAT),
   SIGNUM(ExpressionType.FLOAT, "signum", ExpressionType.FLOAT),
   SQRT(ExpressionType.FLOAT, "sqrt", ExpressionType.FLOAT),
   FMOD(ExpressionType.FLOAT, "fmod", ExpressionType.FLOAT, ExpressionType.FLOAT),
   TIME(ExpressionType.FLOAT, "time"),
   IF(
      ExpressionType.FLOAT,
      "if",
      new ParametersVariable().first(ExpressionType.BOOL, ExpressionType.FLOAT).repeat(ExpressionType.BOOL, ExpressionType.FLOAT).last(ExpressionType.FLOAT)
   ),
   NOT(12, ExpressionType.BOOL, "!", ExpressionType.BOOL),
   AND(3, ExpressionType.BOOL, "&&", ExpressionType.BOOL, ExpressionType.BOOL),
   OR(2, ExpressionType.BOOL, "||", ExpressionType.BOOL, ExpressionType.BOOL),
   GREATER(8, ExpressionType.BOOL, ">", ExpressionType.FLOAT, ExpressionType.FLOAT),
   GREATER_OR_EQUAL(8, ExpressionType.BOOL, ">=", ExpressionType.FLOAT, ExpressionType.FLOAT),
   SMALLER(8, ExpressionType.BOOL, "<", ExpressionType.FLOAT, ExpressionType.FLOAT),
   SMALLER_OR_EQUAL(8, ExpressionType.BOOL, "<=", ExpressionType.FLOAT, ExpressionType.FLOAT),
   EQUAL(7, ExpressionType.BOOL, "==", ExpressionType.FLOAT, ExpressionType.FLOAT),
   NOT_EQUAL(7, ExpressionType.BOOL, "!=", ExpressionType.FLOAT, ExpressionType.FLOAT),
   BETWEEN(7, ExpressionType.BOOL, "between", ExpressionType.FLOAT, ExpressionType.FLOAT, ExpressionType.FLOAT),
   EQUALS(7, ExpressionType.BOOL, "equals", ExpressionType.FLOAT, ExpressionType.FLOAT, ExpressionType.FLOAT),
   IN(ExpressionType.BOOL, "in", new ParametersVariable().first(ExpressionType.FLOAT).repeat(ExpressionType.FLOAT).last(ExpressionType.FLOAT)),
   SMOOTH(ExpressionType.FLOAT, "smooth", new ParametersVariable().first(ExpressionType.FLOAT).repeat(ExpressionType.FLOAT).maxCount(4)),
   TRUE(ExpressionType.BOOL, "true"),
   FALSE(ExpressionType.BOOL, "false"),
   VEC2(ExpressionType.FLOAT_ARRAY, "vec2", ExpressionType.FLOAT, ExpressionType.FLOAT),
   VEC3(ExpressionType.FLOAT_ARRAY, "vec3", ExpressionType.FLOAT, ExpressionType.FLOAT, ExpressionType.FLOAT),
   VEC4(ExpressionType.FLOAT_ARRAY, "vec4", ExpressionType.FLOAT, ExpressionType.FLOAT, ExpressionType.FLOAT, ExpressionType.FLOAT);

   private int precedence;
   private ExpressionType expressionType;
   private String name;
   private IParameters parameters;
   public static FunctionType[] VALUES = values();
   private static final Map<Integer, Float> mapSmooth = new HashMap<>();

   private FunctionType(ExpressionType expressionType, String name, ExpressionType... parameterTypes) {
      this(0, expressionType, name, parameterTypes);
   }

   private FunctionType(int precedence, ExpressionType expressionType, String name, ExpressionType... parameterTypes) {
      this(precedence, expressionType, name, new Parameters(parameterTypes));
   }

   private FunctionType(ExpressionType expressionType, String name, IParameters parameters) {
      this(0, expressionType, name, parameters);
   }

   private FunctionType(int precedence, ExpressionType expressionType, String name, IParameters parameters) {
      this.precedence = precedence;
      this.expressionType = expressionType;
      this.name = name;
      this.parameters = parameters;
   }

   public String getName() {
      return this.name;
   }

   public int getPrecedence() {
      return this.precedence;
   }

   public ExpressionType getExpressionType() {
      return this.expressionType;
   }

   public IParameters getParameters() {
      return this.parameters;
   }

   public int getParameterCount(IExpression[] arguments) {
      return this.parameters.getParameterTypes(arguments).length;
   }

   public ExpressionType[] getParameterTypes(IExpression[] arguments) {
      return this.parameters.getParameterTypes(arguments);
   }

   public float evalFloat(IExpression[] args) {
      switch (this) {
         case PLUS:
            return evalFloat(args, 0) + evalFloat(args, 1);
         case MINUS:
            return evalFloat(args, 0) - evalFloat(args, 1);
         case MUL:
            return evalFloat(args, 0) * evalFloat(args, 1);
         case DIV:
            return evalFloat(args, 0) / evalFloat(args, 1);
         case MOD:
            float modX = evalFloat(args, 0);
            float modY = evalFloat(args, 1);
            return modX - modY * (float)((int)(modX / modY));
         case NEG:
            return -evalFloat(args, 0);
         case PI:
            return ((float) Math.PI);
         case SIN:
            return MathHelper.sin(evalFloat(args, 0));
         case COS:
            return MathHelper.cos(evalFloat(args, 0));
         case ASIN:
            return (float) Math.asin(evalFloat(args, 0));
         case ACOS:
            return (float) Math.acos(evalFloat(args, 0));
         case TAN:
            return (float)Math.tan((double)evalFloat(args, 0));
         case ATAN:
            return (float)Math.atan((double)evalFloat(args, 0));
         case ATAN2:
            return (float)MathHelper.atan2((double)evalFloat(args, 0), (double)evalFloat(args, 1));
         case TORAD:
            return (float) Math.toRadians(evalFloat(args, 0));
         case TODEG:
            return (float) Math.toDegrees(evalFloat(args, 0));
         case MIN:
            return this.getMin(args);
         case MAX:
            return this.getMax(args);
         case CLAMP:
            return MathHelper.clamp(evalFloat(args, 0), evalFloat(args, 1), evalFloat(args, 2));
         case ABS:
            return MathHelper.abs(evalFloat(args, 0));
         case EXP:
            return (float)Math.exp((double)evalFloat(args, 0));
         case FLOOR:
            return (float)Math.floor(evalFloat(args, 0));
         case CEIL:
            return (float)Math.ceil(evalFloat(args, 0));
         case FRAC:
            return (float)MathHelper.fractionalPart((double)evalFloat(args, 0));
         case LOG:
            return (float)Math.log((double)evalFloat(args, 0));
         case POW:
            return (float)Math.pow((double)evalFloat(args, 0), (double)evalFloat(args, 1));
         case RANDOM:
            return (float)Math.random();
         case ROUND:
            return (float)Math.round(evalFloat(args, 0));
         case SIGNUM:
            return Math.signum(evalFloat(args, 0));
         case SQRT:
            return (float) Math.sqrt(evalFloat(args, 0));
         case FMOD:
            float fmodX = evalFloat(args, 0);
            float fmodY = evalFloat(args, 1);
            return fmodX - fmodY * (float)MathHelper.floor(fmodX / fmodY);
         case TIME:
            MinecraftClient mc = MinecraftClient.getInstance();
            World world = mc.world;
            if (world == null) {
               return 0.0F;
            }

            return (float)(world.getLastUpdateTime() % 24000L);
         case IF:
            int countChecks = (args.length - 1) / 2;

            for (int i = 0; i < countChecks; i++) {
               int index = i * 2;
               if (evalBool(args, index)) {
                  return evalFloat(args, index + 1);
               }
            }

            return evalFloat(args, countChecks * 2);
         case SMOOTH:
            int id = (int)evalFloat(args, 0);
            float valRaw = evalFloat(args, 1);
            float valFadeUp = args.length > 2 ? evalFloat(args, 2) : 1.0F;
            float valFadeDown = args.length > 3 ? evalFloat(args, 3) : valFadeUp;
            return Smoother.getSmoothValue(id, valRaw, valFadeUp, valFadeDown);
         default:
            Photon.LOGGER.warn("Unknown function type: " + this);
            return 0.0F;
      }
   }

   private float getMin(IExpression[] exprs) {
      if (exprs.length == 2) {
         return Math.min(evalFloat(exprs, 0), evalFloat(exprs, 1));
      } else {
         float valMin = evalFloat(exprs, 0);

         for (int i = 1; i < exprs.length; i++) {
            float valExpr = evalFloat(exprs, i);
            if (valExpr < valMin) {
               valMin = valExpr;
            }
         }

         return valMin;
      }
   }

   private float getMax(IExpression[] exprs) {
      if (exprs.length == 2) {
         return Math.max(evalFloat(exprs, 0), evalFloat(exprs, 1));
      } else {
         float valMax = evalFloat(exprs, 0);

         for (int i = 1; i < exprs.length; i++) {
            float valExpr = evalFloat(exprs, i);
            if (valExpr > valMax) {
               valMax = valExpr;
            }
         }

         return valMax;
      }
   }

   private static float evalFloat(IExpression[] exprs, int index) {
      IExpressionFloat ef = (IExpressionFloat)exprs[index];
      return ef.eval();
   }

   public boolean evalBool(IExpression[] args) {
      switch (this) {
         case TRUE:
            return true;
         case FALSE:
            return false;
         case NOT:
            return !evalBool(args, 0);
         case AND:
            return evalBool(args, 0) && evalBool(args, 1);
         case OR:
            return evalBool(args, 0) || evalBool(args, 1);
         case GREATER:
            return evalFloat(args, 0) > evalFloat(args, 1);
         case GREATER_OR_EQUAL:
            return evalFloat(args, 0) >= evalFloat(args, 1);
         case SMALLER:
            return evalFloat(args, 0) < evalFloat(args, 1);
         case SMALLER_OR_EQUAL:
            return evalFloat(args, 0) <= evalFloat(args, 1);
         case EQUAL:
            return evalFloat(args, 0) == evalFloat(args, 1);
         case NOT_EQUAL:
            return evalFloat(args, 0) != evalFloat(args, 1);
         case BETWEEN:
            float val = evalFloat(args, 0);
            return val >= evalFloat(args, 1) && val <= evalFloat(args, 2);
         case EQUALS:
            float diff = evalFloat(args, 0) - evalFloat(args, 1);
            float delta = evalFloat(args, 2);
            return Math.abs(diff) <= delta;
         case IN:
            float valIn = evalFloat(args, 0);

            for (int i = 1; i < args.length; i++) {
               float valCheck = evalFloat(args, i);
               if (valIn == valCheck) {
                  return true;
               }
            }

            return false;
         default:
            Photon.LOGGER.warn("Unknown function type: " + this);
            return false;
      }
   }

   private static boolean evalBool(IExpression[] exprs, int index) {
      IExpressionBool eb = (IExpressionBool)exprs[index];
      return eb.eval();
   }

   public float[] evalFloatArray(IExpression[] args) {
      switch (this) {
         case VEC2:
            return new float[]{evalFloat(args, 0), evalFloat(args, 1)};
         case VEC3:
            return new float[]{evalFloat(args, 0), evalFloat(args, 1), evalFloat(args, 2)};
         case VEC4:
            return new float[]{evalFloat(args, 0), evalFloat(args, 1), evalFloat(args, 2), evalFloat(args, 3)};
         default:
            Photon.LOGGER.warn("Unknown function type: " + this);
            return null;
      }
   }

   public static FunctionType parse(String str) {
      for (int i = 0; i < VALUES.length; i++) {
         FunctionType ef = VALUES[i];
         if (ef.getName().equals(str)) {
            return ef;
         }
      }

      return null;
   }
}
