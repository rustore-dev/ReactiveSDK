plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.jetbrains.kotlin.android)
    id("maven-publish")
}

android {
    namespace = "ru.rustore.reactive.test"
    compileSdk = 35

    defaultConfig {
        minSdk = 24

        proguardFiles(
            getDefaultProguardFile("proguard-android-optimize.txt"),
            "proguard-rules.pro",
        )

        consumerProguardFile("consumer-rules.pro")
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
    kotlinOptions {
        jvmTarget = "1.8"
    }

    packaging {
        resources.excludes += "META-INF/LICENSE*"
    }

    configurePublishing()
}

dependencies {
    implementation(project(":reactive"))

    implementation(libs.junit.jupiter.api)
}

private fun Project.disableGenerateModuleMetadataTask() {
    tasks.withType(GenerateModuleMetadata::class.java).configureEach {
        enabled = false
    }
}

private fun Project.getProperty(key: String): String =
    requireNotNull(project.extra[key]?.toString()) { "can't find property $key" }

private fun Project.configurePublishing() {
    afterEvaluate {
        publishing {
            publications {
                create<MavenPublication>("release") {
                    from(components["release"])
                    groupId = "ru.rustore"
                    artifactId = "reactive-test"
                    version = "0.0.1"
                }
            }
            with(repositories) {
                maven("https://artifactory-external.vkpartner.ru/artifactory/rustore-maven/") {
                    name = "ExternalArtifactory"
                    credentials {
                        if (project.hasProperty("vkRepoWriteUsername")
                            && project.hasProperty("vkRepoWritePassword")
                        ) {
                            username = getProperty("vkRepoWriteUsername")
                            password = project.getProperty("vkRepoWritePassword")
                        }
                    }
                }
            }
        }
    }
    disableGenerateModuleMetadataTask()
}
