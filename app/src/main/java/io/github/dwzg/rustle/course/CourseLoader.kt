package io.github.dwzg.rustle.course

import android.content.res.AssetManager
import io.github.dwzg.rustle.core.CourseFile
import io.github.dwzg.rustle.core.Engine

object CourseLoader {
    private const val DIR = "course"

    /** Reads the course files from assets and hands them to the Rust core. */
    fun load(assets: AssetManager): Engine {
        val files = assets.list(DIR).orEmpty().filter { it.endsWith(".md") }.map { name ->
            CourseFile(name, assets.open("$DIR/$name").bufferedReader().use { it.readText() })
        }
        return Engine(files)
    }
}
