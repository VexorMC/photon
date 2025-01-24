package net.optifine.shaders;

import dev.vexor.photon.Photon;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public abstract class SMCLog {
   public static void severe(String message) {
      Photon.LOGGER.error(message);
   }

   public static void warning(String message) {
      Photon.LOGGER.warn(message);
   }

   public static void info(String message) {
      Photon.LOGGER.info(message);
   }

   public static void fine(String message) {
      Photon.LOGGER.debug(message);
   }

   public static void severe(String format, Object... args) {
      String message = String.format(format, args);
      Photon.LOGGER.error(message);
   }

   public static void warning(String format, Object... args) {
      String message = String.format(format, args);
      Photon.LOGGER.warn(message);
   }

   public static void info(String format, Object... args) {
      String message = String.format(format, args);
      Photon.LOGGER.info(message);
   }

   public static void fine(String format, Object... args) {
      String message = String.format(format, args);
      Photon.LOGGER.debug(message);
   }
}
