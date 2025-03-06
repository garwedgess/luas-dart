plugins {
    `kotlin-dsl`
}

dependencies {
    api(libs.jetbrains.kotlin.gradle.plugin)
    api(libs.android.tools.build.gradle)
    gradleApi()
    compileOnly(libs.detekt.gradle)
}

gradlePlugin {
    plugins {
        register("detekt") {
            id = "android-detekt-convention"
            implementationClass = "com.wedgess.detekt.DetektConventionPlugin"
        }
    }
}