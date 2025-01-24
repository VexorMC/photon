package dev.vexor.photon;

import dev.vexor.photon.format.VertexFormatHooks;
import net.fabricmc.api.ModInitializer;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class Photon implements ModInitializer {
    public static Logger LOGGER = LogManager.getLogger("Photon");

    @Override
    public void onInitialize() {
        VertexFormatHooks.init();
    }
}
