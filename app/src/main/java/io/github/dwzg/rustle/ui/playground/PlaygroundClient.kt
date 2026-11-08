package io.github.dwzg.rustle.ui.playground

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

data class RunResult(val success: Boolean, val stdout: String, val stderr: String)

/** Runs code on the official Rust Playground (play.rust-lang.org). */
object PlaygroundClient {
    private const val ENDPOINT = "https://play.rust-lang.org/execute"

    suspend fun run(code: String): RunResult = withContext(Dispatchers.IO) {
        val body = JSONObject()
            .put("channel", "stable")
            .put("mode", "debug")
            .put("edition", "2024")
            .put("crateType", "bin")
            .put("tests", false)
            .put("backtrace", false)
            .put("code", code)
            .toString()
        val conn = (URL(ENDPOINT).openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            connectTimeout = 15_000
            readTimeout = 60_000
            doOutput = true
            setRequestProperty("Content-Type", "application/json")
            setRequestProperty("User-Agent", "Rustle-Android")
        }
        try {
            conn.outputStream.use { it.write(body.toByteArray()) }
            val ok = conn.responseCode in 200..299
            val text = (if (ok) conn.inputStream else conn.errorStream)?.bufferedReader()?.use { it.readText() }.orEmpty()
            if (!ok) return@withContext RunResult(false, "", "Playground returned HTTP ${conn.responseCode}\n$text")
            val json = JSONObject(text)
            RunResult(
                success = json.optBoolean("success", false),
                stdout = json.optString("stdout"),
                stderr = cleanStderr(json.optString("stderr").ifEmpty { json.optString("error") }),
            )
        } finally {
            conn.disconnect()
        }
    }

    /** Drops cargo's progress noise so the compiler messages stand out. */
    fun cleanStderr(stderr: String): String = stderr.lines()
        .filterNot { line ->
            val t = line.trim()
            t.startsWith("Compiling playground") || t.startsWith("Finished ") || t.startsWith("Running `")
        }
        .joinToString("\n")
        .trim()
}
