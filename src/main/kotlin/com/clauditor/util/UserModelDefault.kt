package com.clauditor.util

import com.google.gson.JsonParser
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption

/**
 * The `model` key of the user's ~/.claude/settings.json: Claude Code saves it whenever a
 * session's model is set through its settings (the in-session route the model dropdown takes),
 * so the dropdown reads it first and puts it back after. A live session keeps the model it was
 * switched to when the default changes under it. Only the one top-level line is edited, so the
 * rest of the file keeps its layout.
 */
object UserModelDefault {
    fun file(): Path = Path.of(System.getProperty("user.home"), ".claude", "settings.json")

    /** The saved default, or null when the file has none (or cannot be read). */
    fun read(path: Path = file()): String? = try { topLevelModel(Files.readString(path)) } catch (_: Exception) { null }

    /** Put [value] back as the default (null: no default). False when the file was left alone. */
    fun restore(value: String?, path: Path = file()): Boolean = try {
        val text = Files.readString(path)
        if (topLevelModel(text) == value) true
        else {
            val next = withModel(text, value)
            if (next == null) false
            else {
                val tmp = path.resolveSibling(path.fileName.toString() + ".oc-tmp")
                Files.writeString(tmp, next)
                Files.move(tmp, path, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE)
                true
            }
        }
    } catch (_: Exception) { false }

    fun topLevelModel(text: String): String? =
        JsonParser.parseString(text).asJsonObject.get("model")?.takeIf { it.isJsonPrimitive }?.asString

    private val MODEL_LINE = Regex("""(?m)^[ \t]*"model"[ \t]*:[ \t]*"(?:[^"\\]|\\.)*"[ \t]*(,?)[ \t]*\r?\n?""")

    /**
     * [text] with the top-level `model` set to [value], or removed when [value] is null. Null when
     * it cannot be done by editing that one line (no such line, or the result would not parse to it).
     */
    fun withModel(text: String, value: String?): String? {
        val m = MODEL_LINE.findAll(text).firstOrNull { depthAt(text, it.range.first) == 1 }
        val out = when {
            m == null -> return null
            value != null -> {
                val line = m.value
                val start = line.indexOf(':') + 1
                val q1 = line.indexOf('"', start)
                val q2 = line.lastIndexOf('"')
                text.substring(0, m.range.first) + line.substring(0, q1) + com.google.gson.Gson().toJson(value) +
                    line.substring(q2 + 1) + text.substring(m.range.last + 1)
            }
            m.groupValues[1] == "," -> text.removeRange(m.range)
            else -> {
                // the last key: drop the comma ending the key before it
                val before = text.substring(0, m.range.first).trimEnd()
                if (!before.endsWith(",")) return null
                before.dropLast(1) + "\n" + text.substring(m.range.last + 1)
            }
        }
        return try { out.takeIf { topLevelModel(it) == value } } catch (_: Exception) { null }
    }

    /** Object nesting at [index], strings skipped. */
    private fun depthAt(text: String, index: Int): Int {
        var depth = 0
        var inString = false
        var i = 0
        while (i < index) {
            val c = text[i]
            if (inString) {
                if (c == '\\') i++ else if (c == '"') inString = false
            } else when (c) {
                '"' -> inString = true
                '{', '[' -> depth++
                '}', ']' -> depth--
            }
            i++
        }
        return depth
    }
}
