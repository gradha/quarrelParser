plugins {
    //alias(libs.plugins.androidLibrary).apply(false)
    alias(libs.plugins.kotlinMultiplatform).apply(false)
    alias(libs.plugins.dokka)
    //alias(libs.plugins.dokkaJavadoc)
}
subprojects {
    apply(plugin = "org.jetbrains.dokka")
    //apply(plugin = "org.jetbrains.dokka-javadoc")
}

dependencies {
    dokka(project(":shared:"))
}