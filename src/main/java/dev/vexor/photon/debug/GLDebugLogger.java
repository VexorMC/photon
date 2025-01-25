package dev.vexor.photon.debug;

import net.minecraft.client.main.Main;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.KHRDebug;
import org.lwjgl.opengl.KHRDebugCallback;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * A utility class for routing OpenGL debug messages to the Log4J loggers.
 */
public final class GLDebugLogger {
    private static final Logger API_LOG = createDebugLogger("OpenGL API");
    private static final Logger WINDOW_SYSTEM_LOG = createDebugLogger("OpenGL Window System");
    private static final Logger SHADER_COMPILER_LOG = createDebugLogger("OpenGL Shader Compiler");
    private static final Logger THIRD_PARTY_LOG = createDebugLogger("OpenGL Third Party");
    private static final Logger APPLICATION_LOG = createDebugLogger("OpenGL Application");
    private static final Logger OTHER_LOG = createDebugLogger("OpenGL Other");
    private static final Logger UNKNOWN_LOG = createDebugLogger("OpenGL Unknown");

    /**
     * Initializes the OpenGL debug logger.
     */
    public static void init() {
        enableDebugOutput();
        setDebugMessageCallback();
        enableAllDebugMessages();
    }

    /**
     * Creates a Logger and tries to set it to the debug level using an unsafe cast.
     *
     * @param name The name of the logger.
     * @return A logger with the given name that logs at the {@link Level#DEBUG}.
     */
    private static Logger createDebugLogger(String name) {
        var logger = LogManager.getLogger(name);

        if (logger instanceof org.apache.logging.log4j.core.Logger) {
            var realLog = (org.apache.logging.log4j.core.Logger) logger;
            realLog.setLevel(Level.DEBUG);
        } else {
            logger.warn("Failed to set this log to debug level! Some messages may be voided!");
        }

        return logger;
    }

    /**
     * Enables the OpenGL debug output and makes it synchronous.
     */
    private static void enableDebugOutput() {
        GL11.glEnable(KHRDebug.GL_DEBUG_OUTPUT);
        GL11.glEnable(KHRDebug.GL_DEBUG_OUTPUT_SYNCHRONOUS);
    }

    /**
     * Sets the debug message callback.
     * <p>
     * There can only be one callback at a time, so this method will overwrite any existing callback.
     */
    private static void setDebugMessageCallback() {
        KHRDebug.glDebugMessageCallback(createCallback());
    }

    /**
     * Creates a new callback that passes all messages to {@link #log(int, int, int, int, String)}.
     *
     * @return the new callback.
     */
    private static KHRDebugCallback createCallback() {
        return new KHRDebugCallback(GLDebugLogger::log);
    }

    /**
     * Enables all debug messages.
     * <p>
     * This must be called after {@link #setDebugMessageCallback()}, as it configures for the callback.
     */
    private static void enableAllDebugMessages() {
        KHRDebug.glDebugMessageControl(GL11.GL_DONT_CARE,
                                       GL11.GL_DONT_CARE,
                                       GL11.GL_DONT_CARE,
                                       null,
                                       true);
    }

    /**
     * Logs a message with appropriate formatting to the appropriate logger.
     *
     * @param sourceGLEnum   the source of the message.
     * @param typeGLEnum     the type of the message.
     * @param id             the id of the message.
     * @param severityGLEnum the severity of the message.
     * @param message        the message text.
     */
    private static void log(int sourceGLEnum, int typeGLEnum, int id, int severityGLEnum, String message) {
        var stringBuilder = new StringBuilder();

        var messageHeader = messageHeader(typeGLEnum);
        var messageContent = messageContent(message);
        stringBuilder.append(messageHeader)
                     .append(": ")
                     .append(messageContent);

        var stackTrace = Thread.currentThread().getStackTrace();
        var trimmedStackTrace = trimStackTrace(stackTrace);
        for (var stackTraceElement : trimmedStackTrace)
            stringBuilder.append("\n\t at ")
                         .append(stackTraceElement);


        var log = sourceLog(sourceGLEnum);
        var level = logLevel(typeGLEnum);
        log.log(level, stringBuilder.toString());
    }

    /**
     * Provides a header for a given {@code typeGLEnum}.
     *
     * @param typeGLEnum the type of the message.
     * @return a header for the message.
     */
    private static String messageHeader(int typeGLEnum) {
        switch (typeGLEnum) {
            case KHRDebug.GL_DEBUG_TYPE_ERROR:
                return "Caused by";
            case KHRDebug.GL_DEBUG_TYPE_DEPRECATED_BEHAVIOR:
                return "Use of deprecated behaviour";
            case KHRDebug.GL_DEBUG_TYPE_UNDEFINED_BEHAVIOR:
                return "Use of behaviour";
            case KHRDebug.GL_DEBUG_TYPE_PORTABILITY:
                return "Possible portability issue";
            case KHRDebug.GL_DEBUG_TYPE_PERFORMANCE:
                return "Possible performance issue";
            case KHRDebug.GL_DEBUG_TYPE_MARKER:
                return "Marker";
            case KHRDebug.GL_DEBUG_TYPE_PUSH_GROUP:
                return "Group push";
            case KHRDebug.GL_DEBUG_TYPE_POP_GROUP:
                return "Group pop";
            default:
                return String.format("Unknown Type (0x%08X)", typeGLEnum);
        }
    }

    /**
     * Provides the content of a message.
     * <p>
     * If the message is {@code null} or empty, the string {@code "No message"} is returned.
     *
     * @param message the message.
     * @return the message content.
     */
    private static String messageContent(String message) {
        if (message == null || message.isEmpty())
            return "No message";
        return message;
    }

    /**
     * Trim the stack trace to only include the stack trace elements from the {@link Main Minecraft Main method} up to to the first call from the {@link org.lwjgl.opengl LWJGL OpenGL package}.
     *
     * @param stackTrace the stack trace to trim.
     * @return the trimmed stack trace.
     */
    private static List<String> trimStackTrace(StackTraceElement[] stackTrace) {
        var trimmedStackTrace = new ArrayList<String>();

        var foundEnd = false;
        var foundStart = false;
        for (var i = stackTrace.length - 1; i >= 0; i--) {
            var stackTraceElement = stackTrace[i];

            if (!foundEnd) {
                foundEnd = isFromMinecraftMainMethod(stackTraceElement);
            } else {
                foundStart = isFromLWJGLOpenGLPackage(stackTraceElement);
            }

            if (foundEnd)
                trimmedStackTrace.add(stackTraceElement.toString());

            if (foundStart)
                break;
        }

        if (trimmedStackTrace.isEmpty()) {
            for (var stackTraceElement : stackTrace)
                trimmedStackTrace.add(stackTraceElement.toString());
        } else {
            Collections.reverse(trimmedStackTrace);
        }

        return trimmedStackTrace;
    }

    /**
     * Checks if the given stack trace element is from the {@link Main Minecraft Main method}.
     *
     * @param stackTraceElement the stack trace element to check.
     * @return {@code true} if the stack trace element is from the Minecraft main method, {@code false} otherwise.
     */
    private static boolean isFromMinecraftMainMethod(StackTraceElement stackTraceElement) {
        var className = stackTraceElement.getClassName();
        if (!"net.minecraft.client.main.Main".equals(className))
            return false;

        var methodName = stackTraceElement.getMethodName();
        return "main".equals(methodName);
    }

    /**
     * Checks if the given stack trace element is from the {@link org.lwjgl.opengl LWJGL OpenGL package}.
     *
     * @param stackTraceElement the stack trace element to check.
     * @return {@code true} if the stack trace element is from the LWJGL OpenGL package, {@code false} otherwise.
     */
    private static boolean isFromLWJGLOpenGLPackage(StackTraceElement stackTraceElement) {
        var className = stackTraceElement.getClassName();

        return className.startsWith("org.lwjgl.opengl.");
    }

    /**
     * Provides a logger for a given message source.
     *
     * @param sourceGLEnum the source of the message.
     * @return the appropriate logger for the given source.
     */
    private static Logger sourceLog(int sourceGLEnum) {
        switch (sourceGLEnum) {
            case KHRDebug.GL_DEBUG_SOURCE_API:
                return API_LOG;
            case KHRDebug.GL_DEBUG_SOURCE_WINDOW_SYSTEM:
                return WINDOW_SYSTEM_LOG;
            case KHRDebug.GL_DEBUG_SOURCE_SHADER_COMPILER:
                return SHADER_COMPILER_LOG;
            case KHRDebug.GL_DEBUG_SOURCE_THIRD_PARTY:
                return THIRD_PARTY_LOG;
            case KHRDebug.GL_DEBUG_SOURCE_APPLICATION:
                return APPLICATION_LOG;
            case KHRDebug.GL_DEBUG_SOURCE_OTHER:
                return OTHER_LOG;
            default:
                return UNKNOWN_LOG;
        }
    }

    /**
     * Provides the log level for a given message type.
     *
     * @param typeGLEnum the type of the message.
     * @return the appropriate log level for the given type.
     */
    private static Level logLevel(int typeGLEnum) {
        switch (typeGLEnum) {
            case KHRDebug.GL_DEBUG_TYPE_ERROR:
                return Level.ERROR;
            case KHRDebug.GL_DEBUG_TYPE_DEPRECATED_BEHAVIOR:
            case KHRDebug.GL_DEBUG_TYPE_UNDEFINED_BEHAVIOR:
            case KHRDebug.GL_DEBUG_TYPE_PERFORMANCE:
                return Level.WARN;
            default:
                return Level.DEBUG;
        }
    }
}
