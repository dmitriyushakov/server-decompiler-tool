import org.jetbrains.kotlin.gradle.targets.js.webpack.KotlinWebpack
import org.jetbrains.kotlin.gradle.targets.js.webpack.KotlinWebpackConfig

plugins {
    val kotlinVersion: String by System.getProperties()
    kotlin("plugin.serialization") version kotlinVersion
    kotlin("multiplatform") version kotlinVersion
    val kvisionVersion: String by System.getProperties()
    id("io.kvision") version kvisionVersion
}

group = "com.github.dmitriyushakov.srv_decompiler"
version = "0.1"

repositories {
    mavenCentral()
    mavenLocal()
    jcenter()
}

// Versions
val kotlinVersion: String by System.getProperties()
val kvisionVersion: String by System.getProperties()
val ktorVersion: String by System.getProperties()
val logbackVersion: String by project

val webDir = file("src/frontendMain/web")
val mainClassName = "com.github.dmitriyushakov.srv_decompiler.cli.MainKt"


kotlin {
    jvm("backend") {
        compilations.all {
            java {
                targetCompatibility = JavaVersion.VERSION_17
            }
            kotlinOptions {
                jvmTarget = "17"
                freeCompilerArgs = listOf("-Xjsr305=strict")
            }
        }
    }
    js("frontend") {
        browser {
            commonWebpackConfig {
                outputFileName = "main.bundle.js"
            }
            runTask {
                sourceMaps = false
                devServer = KotlinWebpackConfig.DevServer(
                    open = false,
                    port = 3000,
                    proxy = mutableMapOf(
                        "/api/*" to "http://localhost:8080",
                    ),
                    static = mutableListOf("$buildDir/processedResources/frontend/main")
                )
            }
            testTask {
                useKarma {
                    useChromeHeadless()
                }
            }
        }
        binaries.executable()
    }
    sourceSets {
        val backendMain by getting {
            dependencies {
                implementation("io.ktor:ktor-server-core:$ktorVersion")
                implementation("io.ktor:ktor-server-netty:$ktorVersion")
                implementation("io.ktor:ktor-server-config-yaml:$ktorVersion")
                implementation("io.ktor:ktor-server-status-pages:$ktorVersion")
                implementation("io.ktor:ktor-serialization-jackson:$ktorVersion")
                implementation("io.ktor:ktor-server-content-negotiation:$ktorVersion")
                implementation("io.ktor:ktor-server-websockets:$ktorVersion")
                implementation("org.ow2.asm:asm:9.4")

                implementation("org.apache.logging.log4j:log4j-core:2.20.0")
                implementation("org.apache.logging.log4j:log4j-slf4j-impl:2.20.0")

                implementation("com.fasterxml.jackson.dataformat:jackson-dataformat-yaml:2.7.3")
                implementation("com.fasterxml.jackson.core:jackson-databind:2.5.4")

                implementation("org.jd:jd-core:1.1.3")
                implementation("org.benf:cfr:0.152")
                implementation("org.bitbucket.mstrobel:procyon-compilertools:0.6.0")

                implementation("com.github.javaparser:javaparser-core-serialization:3.25.2")

                implementation("org.jetbrains.kotlinx:kotlinx-cli:0.3.6")
            }
        }
        val frontendMain by getting {
            resources.srcDir(webDir)
            dependencies {
                implementation("io.kvision:kvision:$kvisionVersion")
                implementation("io.kvision:kvision-state:$kvisionVersion")
                implementation("io.kvision:kvision-bootstrap:$kvisionVersion")
                implementation("io.kvision:kvision-fontawesome:$kvisionVersion")
                implementation("io.kvision:kvision-react:$kvisionVersion")
                implementation("io.ktor:ktor-client-core-js:$ktorVersion")
                implementation("io.ktor:ktor-client-content-negotiation-js:$ktorVersion")
                implementation("io.ktor:ktor-serialization-kotlinx-json-js:$ktorVersion")

                implementation(npm("rc-tree", "5.7.12"))
                implementation(npm("jetbrains-mono", "1.0.6"))
            }
        }
    }
}

afterEvaluate {
    tasks {
        create("frontendArchive", Jar::class).apply {
            dependsOn("frontendBrowserProductionWebpack")
            group = "package"
            archiveAppendix.set("frontend")
            val distribution =
                project.tasks.getByName("frontendBrowserProductionWebpack", KotlinWebpack::class).destinationDirectory
            from(distribution) {
                include("*.*")
            }
            from(webDir)
            duplicatesStrategy = DuplicatesStrategy.EXCLUDE
            into("/assets")
            inputs.files(distribution, webDir)
            outputs.file(archiveFile)
            manifest {
                attributes(
                    mapOf(
                        "Implementation-Title" to rootProject.name,
                        "Implementation-Group" to rootProject.group,
                        "Implementation-Version" to rootProject.version,
                        "Timestamp" to System.currentTimeMillis()
                    )
                )
            }
        }
        getByName("backendProcessResources", Copy::class) {
            duplicatesStrategy = DuplicatesStrategy.EXCLUDE
        }
        getByName("backendJar").group = "package"
        create("jar", Jar::class).apply {
            dependsOn("frontendArchive", "backendJar")
            group = "package"
            manifest {
                attributes(
                    mapOf(
                        "Implementation-Title" to rootProject.name,
                        "Implementation-Group" to rootProject.group,
                        "Implementation-Version" to rootProject.version,
                        "Timestamp" to System.currentTimeMillis(),
                        "Main-Class" to mainClassName
                    )
                )
            }
            val dependencies = configurations["backendRuntimeClasspath"].filter { it.name.endsWith(".jar") } +
                    project.tasks["backendJar"].outputs.files +
                    project.tasks["frontendArchive"].outputs.files
            dependencies.forEach {
                if (it.isDirectory) from(it) else from(zipTree(it))
            }
            exclude("META-INF/*.RSA", "META-INF/*.SF", "META-INF/*.DSA")
            inputs.files(dependencies)
            outputs.file(archiveFile)
            duplicatesStrategy = DuplicatesStrategy.EXCLUDE
        }
        create("backendRun", JavaExec::class) {
            dependsOn("compileKotlinBackend")
            group = "run"
            mainClass.set(mainClassName)
            classpath =
                configurations["backendRuntimeClasspath"] + project.tasks["compileKotlinBackend"].outputs.files +
                        project.tasks["backendProcessResources"].outputs.files
            workingDir = buildDir
        }
    }
}
