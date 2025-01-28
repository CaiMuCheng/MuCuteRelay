@file:Suppress("VulnerableLibrariesLocal")

plugins {
    kotlin("jvm") version "2.0.21"
    id("maven-publish")
}

group = "com.mucheng.mucute.relay"
version = "1.0-SNAPSHOT"

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(17))
    }
}

repositories {
    mavenLocal()
    mavenCentral()
    maven("https://repo.opencollab.dev/maven-snapshots")
    maven("https://repo.opencollab.dev/maven-releases")
    maven("https://mccheatz.github.io/maven_repo")
}

dependencies {
    implementation(platform(libs.log4j.bom))
    implementation(libs.log4j.api)
    implementation(libs.log4j.core)
    implementation(libs.minecraftauth)
    implementation(libs.jackson.databind)
    implementation(libs.bedrock.codec)
    implementation(libs.bedrock.common)
    implementation(libs.bedrock.connection)
    implementation(libs.kotlinx.coroutines)
    testImplementation(kotlin("test"))
}

publishing {
    publications {
        create<MavenPublication>("App") {
            from(components["kotlin"])
        }
    }
}

tasks.test {
    useJUnitPlatform()
}

kotlin {
    jvmToolchain(17)
}