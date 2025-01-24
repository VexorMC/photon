package net.optifine.config;

import java.util.Arrays;
import java.util.regex.Pattern;

import dev.vexor.photon.Config;
import net.minecraft.nbt.NbtByte;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtDouble;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtFloat;
import net.minecraft.nbt.NbtInt;
import net.minecraft.nbt.NbtList;
import net.minecraft.nbt.NbtLong;
import net.minecraft.nbt.NbtShort;
import net.minecraft.nbt.NbtString;
import net.optifine.util.StrUtils;
import org.apache.commons.lang3.StringEscapeUtils;

public class NbtTagValue {
    private String[] parents = null;
    private String name = null;
    private boolean negative = false;
    private int type = 0;
    private String value = null;
    private int valueFormat = 0;
    private static final int TYPE_TEXT = 0;
    private static final int TYPE_PATTERN = 1;
    private static final int TYPE_IPATTERN = 2;
    private static final int TYPE_REGEX = 3;
    private static final int TYPE_IREGEX = 4;
    private static final String PREFIX_PATTERN = "pattern:";
    private static final String PREFIX_IPATTERN = "ipattern:";
    private static final String PREFIX_REGEX = "regex:";
    private static final String PREFIX_IREGEX = "iregex:";
    private static final int FORMAT_DEFAULT = 0;
    private static final int FORMAT_HEX_COLOR = 1;
    private static final String PREFIX_HEX_COLOR = "#";
    private static final Pattern PATTERN_HEX_COLOR = Pattern.compile("^#[0-9a-f]{6}+$");

    public NbtTagValue(String tag, String value) {
        String[] names = Config.tokenize(tag, ".");
        this.parents = Arrays.copyOfRange(names, 0, names.length - 1);
        this.name = names[names.length - 1];
        if (value.startsWith("!")) {
            this.negative = true;
            value = value.substring(1);
        }

        if (value.startsWith("pattern:")) {
            this.type = 1;
            value = value.substring("pattern:".length());
        } else if (value.startsWith("ipattern:")) {
            this.type = 2;
            value = value.substring("ipattern:".length()).toLowerCase();
        } else if (value.startsWith("regex:")) {
            this.type = 3;
            value = value.substring("regex:".length());
        } else if (value.startsWith("iregex:")) {
            this.type = 4;
            value = value.substring("iregex:".length()).toLowerCase();
        } else {
            this.type = 0;
        }

        value = StringEscapeUtils.unescapeJava(value);
        if (this.type == 0 && PATTERN_HEX_COLOR.matcher(value).matches()) {
            this.valueFormat = 1;
        }

        this.value = value;
    }

    public boolean matches(NbtCompound nbt) {
        return this.negative ? !this.matchesCompound(nbt) : this.matchesCompound(nbt);
    }

    public boolean matchesCompound(NbtCompound nbt) {
        if (nbt == null) {
            return false;
        } else {
            NbtElement tagBase = nbt;

            for (int i = 0; i < this.parents.length; i++) {
                String tag = this.parents[i];
                tagBase = getChildTag(tagBase, tag);
                if (tagBase == null) {
                    return false;
                }
            }

            if (this.name.equals("*")) {
                return this.matchesAnyChild(tagBase);
            } else {
                tagBase = getChildTag(tagBase, this.name);
                return tagBase == null ? false : this.matchesBase(tagBase);
            }
        }
    }

    private boolean matchesAnyChild(NbtElement tagBase) {
        if (tagBase instanceof NbtCompound) {
            NbtCompound tagCompound = (NbtCompound)tagBase;

            for (String key : tagCompound.getKeys()) {
                NbtElement nbtBase = tagCompound.get(key);
                if (this.matchesBase(nbtBase)) {
                    return true;
                }
            }
        }

        if (tagBase instanceof NbtList) {
            NbtList tagList = (NbtList)tagBase;
            int count = tagList.size();

            for (int i = 0; i < count; i++) {
                NbtElement nbtBase = tagList.get(i);
                if (this.matchesBase(nbtBase)) {
                    return true;
                }
            }
        }

        return false;
    }

    private static NbtElement getChildTag(NbtElement tagBase, String tag) {
        if (tagBase instanceof NbtCompound) {
            NbtCompound tagCompound = (NbtCompound)tagBase;
            return tagCompound.get(tag);
        } else if (tagBase instanceof NbtList) {
            NbtList tagList = (NbtList)tagBase;
            if (tag.equals("count")) {
                return new NbtInt(tagList.size());
            } else {
                int index = Config.parseInt(tag, -1);
                return index >= 0 && index < tagList.size() ? tagList.get(index) : null;
            }
        } else {
            return null;
        }
    }

    public boolean matchesBase(NbtElement nbtBase) {
        if (nbtBase == null) {
            return false;
        } else {
            String nbtValue = getNbtString(nbtBase, this.valueFormat);
            return this.matchesValue(nbtValue);
        }
    }

    public boolean matchesValue(String nbtValue) {
        if (nbtValue == null) {
            return false;
        } else {
            switch (this.type) {
                case 0:
                    return nbtValue.equals(this.value);
                case 1:
                    return this.matchesPattern(nbtValue, this.value);
                case 2:
                    return this.matchesPattern(nbtValue.toLowerCase(), this.value);
                case 3:
                    return this.matchesRegex(nbtValue, this.value);
                case 4:
                    return this.matchesRegex(nbtValue.toLowerCase(), this.value);
                default:
                    throw new IllegalArgumentException("Unknown NbtTagValue type: " + this.type);
            }
        }
    }

    private boolean matchesPattern(String str, String pattern) {
        return StrUtils.equalsMask(str, pattern, '*', '?');
    }

    private boolean matchesRegex(String str, String regex) {
        return str.matches(regex);
    }

    private static String getNbtString(NbtElement nbtBase, int format) {
        if (nbtBase == null) {
            return null;
        } else if (nbtBase instanceof NbtString) {
            NbtString nbtString = (NbtString)nbtBase;
            return nbtString.asString();
        } else if (nbtBase instanceof NbtInt) {
            NbtInt i = (NbtInt)nbtBase;
            return format == 1 ? "#" + StrUtils.fillLeft(Integer.toHexString(i.intValue()), 6, '0') : Integer.toString(i.intValue());
        } else if (nbtBase instanceof NbtByte) {
            NbtByte b = (NbtByte)nbtBase;
            return Byte.toString(b.byteValue());
        } else if (nbtBase instanceof NbtShort) {
            NbtShort s = (NbtShort)nbtBase;
            return Short.toString(s.shortValue());
        } else if (nbtBase instanceof NbtLong) {
            NbtLong l = (NbtLong)nbtBase;
            return Long.toString(l.longValue());
        } else if (nbtBase instanceof NbtFloat) {
            NbtFloat f = (NbtFloat)nbtBase;
            return Float.toString(f.floatValue());
        } else if (nbtBase instanceof NbtDouble) {
            NbtDouble d = (NbtDouble)nbtBase;
            return Double.toString(d.doubleValue());
        } else {
            return nbtBase.toString();
        }
    }

    @Override
    public String toString() {
        StringBuffer sb = new StringBuffer();

        for (int i = 0; i < this.parents.length; i++) {
            String parent = this.parents[i];
            if (i > 0) {
                sb.append(".");
            }

            sb.append(parent);
        }

        if (sb.length() > 0) {
            sb.append(".");
        }

        sb.append(this.name);
        sb.append(" = ");
        sb.append(this.value);
        return sb.toString();
    }
}
