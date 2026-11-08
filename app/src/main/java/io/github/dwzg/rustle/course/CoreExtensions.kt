package io.github.dwzg.rustle.course

import io.github.dwzg.rustle.core.Lesson
import io.github.dwzg.rustle.core.LevelInfo
import io.github.dwzg.rustle.core.Step
import io.github.dwzg.rustle.core.StepContent

/** Small conveniences on top of the types generated from the Rust core. */
val Step.isExercise: Boolean get() = content !is StepContent.Explain

val Lesson.exerciseCount: Int get() = steps.count { it.isExercise }

val LevelInfo.fraction: Float get() = into.toFloat() / span
