package com.wedgess.detekt

import io.gitlab.arturbosch.detekt.extensions.DetektExtension
import org.gradle.api.JavaVersion
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.artifacts.VersionCatalogsExtension
import org.gradle.kotlin.dsl.getByType
import org.gradle.kotlin.dsl.withType

class DetektConventionPlugin : Plugin<Project> {
    override fun apply(project: Project) {
        with(project) {
            val libs = extensions.getByType<VersionCatalogsExtension>().named("libs")

            pluginManager.apply(libs.findPlugin("detekt").get().get().pluginId)

            tasks.withType<io.gitlab.arturbosch.detekt.Detekt>().configureEach {
                jvmTarget = JavaVersion.VERSION_17.toString()
            }
            tasks.withType<io.gitlab.arturbosch.detekt.DetektCreateBaselineTask>().configureEach {
                jvmTarget = JavaVersion.VERSION_17.toString()
            }

            // Configure detekt
            extensions.getByType<DetektExtension>().apply {
                buildUponDefaultConfig = true // preconfigure defaults.
                allRules = false // activate all available (even unstable) rules.
                autoCorrect = false // To enable or disable auto formatting.
                parallel = true
                config.setFrom("${project.rootProject.projectDir}/config/detekt/detekt.yml")
                baseline =
                    file("${project.rootProject.projectDir}/config/detekt/detekt-baseline.xml")
            }

            tasks.withType<io.gitlab.arturbosch.detekt.Detekt>().configureEach {
                reports {
                    html.required.set(true)
                    txt.required.set(true)
                    md.required.set(true)
                }
            }

            dependencies.apply {
                add(
                    "detektPlugins",
                    libs.findLibrary("detekt-compose").get()
                )
                add(
                    "detektPlugins",
                    libs.findLibrary("detekt-formatting").get()
                )
            }
        }
    }
}