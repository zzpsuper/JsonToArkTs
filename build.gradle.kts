plugins {
    id("java")
    id("org.jetbrains.kotlin.jvm") version "1.9.21"
    id("org.jetbrains.intellij") version "1.17.0"
}

group = "com.example"
version = "1.0.1"

repositories {
    maven { url = uri("https://maven.aliyun.com/repository/public") }
    mavenCentral()
}

dependencies {
    implementation("com.google.code.gson:gson:2.10.1")
}

// Configure Gradle IntelliJ Plugin
intellij {
    version.set("2022.3") // Lower target version for better compatibility
    type.set("IC") // Target IDE type - IntelliJ Community
    
    // 如果需要特定的插件依赖，可以在这里添加
    // plugins.set(listOf("com.intellij.java"))
}

tasks {
    // Set the JVM compatibility for the kotlin compiler
    withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
        kotlinOptions.jvmTarget = "17"
    }

    patchPluginXml {
        sinceBuild.set("223")
        untilBuild.set(provider { null })
    }

    signPlugin {
        certificateChain.set(System.getenv("CERTIFICATE_CHAIN"))
        privateKey.set(System.getenv("PRIVATE_KEY"))
        password.set(System.getenv("PRIVATE_KEY_PASSWORD"))
    }

    publishPlugin {
        token.set(System.getenv("PUBLISH_TOKEN"))
    }
}
