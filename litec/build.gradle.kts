import org.gradle.jvm.toolchain.JavaLanguageVersion
import java.time.Duration

plugins {
    java
    kotlin("jvm") version "2.2.0"
    application
}

group = "com.xiaoli"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    implementation(project(":lutil"))
    implementation(project(":lg"))

    implementation("com.vdurmont:emoji-java:5.1.1")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.3")
    implementation("io.arrow-kt:arrow-core:2.1.2") // 版本号请查阅最新

    // 添加 Kotlin 测试库和 JUnit 5
    testImplementation(kotlin("test"))
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(23))
    }
}

application {
    mainClass.set("ldk.l.litec.MainKt")
}

tasks.test {
    useJUnitPlatform() // 使用 JUnit 5 平台
    testLogging {
        events("PASSED", "SKIPPED", "FAILED")
        showStandardStreams = true
    }
    timeout.set(Duration.ofSeconds(300))
    maxParallelForks = if (project.hasProperty("ci")) 4 else 2
}

tasks.jar {
    exclude("META-INF/versions/9/module-info.class")

    manifest {
        attributes["Main-Class"] = "ldk.l.litec.MainKt"
    }

    from(configurations.runtimeClasspath.get().map {
        if (it.isDirectory) it else zipTree(it)
    })

    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
}