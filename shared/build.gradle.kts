import com.vanniktech.maven.publish.SonatypeHost

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    //alias(libs.plugins.androidLibrary)
    alias(libs.plugins.vannipublish) // https://github.com/vanniktech/gradle-maven-publish-plugin
    id("maven-publish")
    alias(libs.plugins.buildconfig) // https://github.com/gmazzo/gradle-buildconfig-plugin
}

// The semversion, propagated through buildConfig to source code (major, minor, patch)
val vMajor: Int = 0
val vMinor: Int = 1
val vPatch: Int = 1


// https://github.com/gmazzo/gradle-buildconfig-plugin
buildConfig {
    useKotlinOutput { topLevelConstants = true }

    buildConfigField("V_MAJOR", vMajor)
    buildConfigField("V_MINOR", vMinor)
    buildConfigField("V_PATCH", vPatch)
}

kotlin {
    /*
    androidTarget {
        compilations.all {
            compileTaskProvider.configure {
                compilerOptions {
                    jvmTarget.set(JvmTarget.JVM_1_8)
                }
            }
        }
    }
     */

    macosX64()
    macosArm64()
    mingwX64()

    /*
    listOf(
        iosX64(),
        iosArm64(),
        iosSimulatorArm64()
    ).forEach {
        it.binaries.framework {
            baseName = "shared"
            isStatic = true
        }
    }
     */

    sourceSets {
        commonMain.dependencies {
            //put your multiplatform dependencies here
        }
        commonTest.dependencies {
            implementation(libs.kotlin.test)
        }
        macosMain.dependencies {
            // Add native dependencies here. Example:
            // api(platform("org.jetbrains.kotlinx:kotlinx-coroutines-core-macosx:1.6.4"))
        }
    }
    withSourcesJar()
}

/*
android {
    namespace = "es.elhaso.quarrelParser"
    compileSdk = 35
    defaultConfig {
        minSdk = 24
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
}
 */

group = "es.elhaso.quarrelParser"
version = "${vMajor}.${vMinor}.${vPatch}"

publishing {
    repositories {
        maven {
            // https://stackoverflow.com/a/71176846/172690
            name = "localDist-quarrelParser"
            url = uri(layout.buildDirectory.dir("localDist"))
        }
    }
}

// https://github.com/Kotlin/multiplatform-library-template
mavenPublishing {
    publishToMavenCentral(SonatypeHost.CENTRAL_PORTAL)

    //val isRelease = if (extra.has("isRelease")) extra.get("isRelease") as? String else null
    //if (isRelease == "true")
    //    signAllPublications()

    coordinates(group.toString(), "quarrelParser", version.toString())

    pom {
        name = "Python inspired command line argument parsing"
        description = "Old Nim code for parsing command line arguments ported to KMP."
        inceptionYear = "2025"
        url = "https://github.com/gradha/quarrelParser"
        licenses {
            license {
                name = "The MIT License"
                url = "https://opensource.org/license/mit"
                distribution = "https://opensource.org/license/mit"
            }
        }
        developers {
            developer {
                id = "gradha"
                name = "Grzegorz Adam Hankiewicz"
                url = "https://github.com/gradha"
            }
        }
        scm {
            url = "https://github.com/gradha/quarrelParser"
            connection = "scm:git:git://github.com/gradha/quarrelParser.git"
            developerConnection = "scm:git:ssh://git@github.com/gradha/quarrelParser.git"
        }
    }
}

// build.gradle.kts

dokka {
    moduleName.set("quarrelParser")
    dokkaPublications.html {
        //suppressInheritedMembers.set(true)
        failOnWarning.set(true)
    }
    dokkaSourceSets.commonMain {
        //includes.from("README.md")
        /*
        sourceLink {
            localDirectory.set(file("src/main/kotlin"))
            remoteUrl("https://example.com/src")
            remoteLineSuffix.set("#L")
        }
         */
    }
    pluginsConfiguration.html {
        //customStyleSheets.from("styles.css")
        //customAssets.from("logo.png")
        //footerMessage.set("(c) Electric Hands Software")
    }
}
