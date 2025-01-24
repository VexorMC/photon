package net.optifine.gui;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import dev.vexor.photon.Config;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.option.GameOptions;
import net.minecraft.client.resource.language.I18n;
import net.optifine.shaders.config.ShaderOption;
import net.optifine.shaders.gui.GuiButtonShaderOption;
import net.optifine.util.StrUtils;

public class TooltipProviderShaderOptions extends TooltipProviderOptions {
    @Override
    public String[] getTooltipLines(ButtonWidget btn, int width) {
        if (!(btn instanceof GuiButtonShaderOption)) {
            return null;
        } else {
            GuiButtonShaderOption btnSo = (GuiButtonShaderOption)btn;
            ShaderOption so = btnSo.getShaderOption();
            return this.makeTooltipLines(so, width);
        }
    }

    private String[] makeTooltipLines(ShaderOption so, int width) {
        String name = so.getNameText();
        String desc = Config.normalize(so.getDescriptionText()).trim();
        String[] descs = this.splitDescription(desc);
        GameOptions settings = Config.getGameSettings();
        String id = null;
        if (!name.equals(so.getName()) && settings.advancedItemTooltips) {
            id = "§8" + I18n.translate("of.general.id") + ": " + so.getName();
        }

        String source = null;
        if (so.getPaths() != null && settings.advancedItemTooltips) {
            source = "§8" + I18n.translate("of.general.from") + ": " + Config.arrayToString((Object[])so.getPaths());
        }

        String def = null;
        if (so.getValueDefault() != null && settings.advancedItemTooltips) {
            String defVal = so.isEnabled() ? so.getValueText(so.getValueDefault()) : I18n.translate("of.general.ambiguous");
            def = "§8" + "Default" + ": " + defVal;
        }

        List<String> list = new ArrayList<>();
        list.add(name);
        list.addAll(Arrays.asList(descs));
        if (id != null) {
            list.add(id);
        }

        if (source != null) {
            list.add(source);
        }

        if (def != null) {
            list.add(def);
        }

        return this.makeTooltipLines(width, list);
    }

    private String[] splitDescription(String desc) {
        if (desc.length() <= 0) {
            return new String[0];
        } else {
            desc = StrUtils.removePrefix(desc, "//");
            String[] descs = desc.split("\\. ");

            for (int i = 0; i < descs.length; i++) {
                descs[i] = "- " + descs[i].trim();
                descs[i] = StrUtils.removeSuffix(descs[i], ".");
            }

            return descs;
        }
    }

    private String[] makeTooltipLines(int width, List<String> args) {
        TextRenderer fr = Config.getMinecraft().textRenderer;
        List<String> list = new ArrayList<>();

        for (int i = 0; i < args.size(); i++) {
            String arg = args.get(i);
            if (arg != null && arg.length() > 0) {
                for (String part : fr.wrapLines(arg, width)) {
                    list.add(part);
                }
            }
        }

        return list.toArray(new String[list.size()]);
    }
}
