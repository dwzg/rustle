package io.github.dwzg.rustle.course

import android.content.res.AssetManager

object CourseLoader {
    private const val DIR = "course"

    fun load(assets: AssetManager): Course {
        val files = assets.list(DIR).orEmpty().filter { it.endsWith(".md") }.sorted()
        val units = files.map { name ->
            val text = assets.open("$DIR/$name").bufferedReader().use { it.readText() }
            CourseParser.parseUnit(text, name)
        }
        return Course(units)
    }
}
