package net.optifine.shaders.uniform;

import org.lwjgl.opengl.ARBShaderObjects;

public class ShaderUniform3f extends ShaderUniformBase {
   private float[][] programValues;
   private static final float VALUE_UNKNOWN = -Float.MAX_VALUE;

   public ShaderUniform3f(String name) {
      super(name);
      this.resetValue();
   }

   public void setValue(float v0, float v1, float v2) {
      int program = this.getProgram();
      float[] valueOld = this.programValues[program];
      if (valueOld[0] != v0 || valueOld[1] != v1 || valueOld[2] != v2) {
         valueOld[0] = v0;
         valueOld[1] = v1;
         valueOld[2] = v2;
         int location = this.getLocation();
         if (location >= 0) {
            ARBShaderObjects.glUniform3fARB(location, v0, v1, v2);
            this.checkGLError();
         }
      }
   }

   public float[] getValue() {
      int program = this.getProgram();
      return this.programValues[program];
   }

   @Override
   protected void onProgramSet(int program) {
      if (program >= this.programValues.length) {
         float[][] valuesOld = this.programValues;
         float[][] valuesNew = new float[program + 10][];
         System.arraycopy(valuesOld, 0, valuesNew, 0, valuesOld.length);
         this.programValues = valuesNew;
      }

      if (this.programValues[program] == null) {
         this.programValues[program] = new float[]{-Float.MAX_VALUE, -Float.MAX_VALUE, -Float.MAX_VALUE};
      }
   }

   @Override
   protected void resetValue() {
      this.programValues = new float[][]{{-Float.MAX_VALUE, -Float.MAX_VALUE, -Float.MAX_VALUE}};
   }
}
