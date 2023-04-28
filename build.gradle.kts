import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    id("io.ktor.plugin") version "2.2.4"
    kotlin("jvm") version "1.6.20"
}

group = "com.github.dmitriyushakov.srv_decompiler"
version = "0.1"

repositories {
    mavenCentral()
    jcenter()
}

dependencies {
    testImplementation(kotlin("test"))
    implementation("io.ktor:ktor-server-core:2.2.4")
    implementation("io.ktor:ktor-server-netty:2.2.4")
    implementation("io.ktor:ktor-server-config-yaml:2.2.4")
    implementation("io.ktor:ktor-server-status-pages:2.2.4")
    implementation("io.ktor:ktor-serialization-jackson:2.2.4")
    implementation("io.ktor:ktor-server-content-negotiation:2.2.4")
    implementation("org.ow2.asm:asm:9.4")

    implementation("org.apache.logging.log4j:log4j-core:2.20.0")
    implementation("org.apache.logging.log4j:log4j-slf4j-impl:2.20.0")

    implementation("com.fasterxml.jackson.dataformat:jackson-dataformat-yaml:2.7.3")
    implementation("com.fasterxml.jackson.core:jackson-databind:2.5.4")

    implementation("org.jd:jd-core:1.1.3")
    implementation("org.benf:cfr:0.152")
    implementation("org.bitbucket.mstrobel:procyon-compilertools:0.6.0")

    implementation("com.github.javaparser:javaparser-core-serialization:3.25.2")
}

application {
    mainClass.set("io.ktor.server.netty.EngineMain")
}

tasks.test {
    useJUnitPlatform()
}

tasks.withType<KotlinCompile> {
    kotlinOptions.jvmTarget = "1.8"
}

ktor {
    fatJar {
        archiveFileName.set("server-decompiler-tool.jar")
    }
}