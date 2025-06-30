package com.wedgess.spotless

import com.diffplug.gradle.spotless.SpotlessExtension
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure

internal fun Project.configureSpotless() {
//    plugins.apply("com.diffplug.spotless")
    configure<SpotlessExtension> {
        kotlin {
            target("**/*.kt")
            targetExclude("**/build/**")
            ktlint("1.5.0").apply {
                setEditorConfigPath(rootProject.file(".editorconfig"))
//                customRuleSets(
//                    listOf(
//                        "io.nlopez.compose.rules:ktlint:0.4.16",
//                    ),
//                )
                editorConfigOverride(
                    mapOf(
                        "android" to "true",
//                        "ij_kotlin_allow_trailing_comma" to "false",
//                        "ij_kotlin_allow_trailing_comma_on_call_site" to "false",
//                        "ktlint_standard_trailing-comma" to "disabled"
                    )
                )
            }
        }
        format("kts") {
            target("**/*.kts")
            targetExclude("**/build/**/*.kts")
        }
        format("xml") {
            target("**/*.xml")
            targetExclude("**/build/**/*.xml")
        }

        kotlinGradle {
            target("*.gradle.kts")
            ktlint("1.5.0").apply {
                setEditorConfigPath(rootProject.file(".editorconfig"))
            }
        }
    }
}
