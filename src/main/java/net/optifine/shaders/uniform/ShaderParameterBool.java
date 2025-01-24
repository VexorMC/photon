package net.optifine.shaders.uniform;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.entity.EntityRenderDispatcher;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.optifine.expr.ExpressionType;
import net.optifine.expr.IExpressionBool;

public enum ShaderParameterBool implements IExpressionBool {
   IS_ALIVE("is_alive"),
   IS_BURNING("is_burning"),
   IS_CHILD("is_child"),
   IS_GLOWING("is_glowing"),
   IS_HURT("is_hurt"),
   IS_IN_LAVA("is_in_lava"),
   IS_IN_WATER("is_in_water"),
   IS_INVISIBLE("is_invisible"),
   IS_ON_GROUND("is_on_ground"),
   IS_RIDDEN("is_ridden"),
   IS_RIDING("is_riding"),
   IS_SNEAKING("is_sneaking"),
   IS_SPRINTING("is_sprinting"),
   IS_WET("is_wet");

   private String name;
   private EntityRenderDispatcher renderManager;
   private static final ShaderParameterBool[] VALUES = values();

   private ShaderParameterBool(String name) {
      this.name = name;
      this.renderManager = MinecraftClient.getInstance().getEntityRenderManager();
   }

   public String getName() {
      return this.name;
   }

   @Override
   public ExpressionType getExpressionType() {
      return ExpressionType.BOOL;
   }

   @Override
   public boolean eval() {
      Entity entityGeneral = MinecraftClient.getInstance().getCameraEntity();
      if (entityGeneral instanceof LivingEntity) {
         LivingEntity entity = (LivingEntity)entityGeneral;
         switch (this) {
            case IS_ALIVE:
               return entity.isAlive();
            case IS_BURNING:
               return entity.isOnFire();
            case IS_CHILD:
               return entity.isBaby();
            case IS_HURT:
               return entity.hurtTime > 0;
            case IS_IN_LAVA:
               return entity.isTouchingLava();
            case IS_IN_WATER:
               return entity.isTouchingWater();
            case IS_INVISIBLE:
               return entity.isInvisible();
            case IS_ON_GROUND:
               return entity.onGround;
            case IS_RIDDEN:
               return entity.rider != null;
            case IS_RIDING:
               return entity.hasVehicle();
            case IS_SNEAKING:
               return entity.isSneaking();
            case IS_SPRINTING:
               return entity.isSprinting();
            case IS_WET:
               return entity.tickFire();
         }
      }

      return false;
   }

   public static ShaderParameterBool parse(String str) {
      if (str == null) {
         return null;
      } else {
         for (int i = 0; i < VALUES.length; i++) {
            ShaderParameterBool type = VALUES[i];
            if (type.getName().equals(str)) {
               return type;
            }
         }

         return null;
      }
   }
}
