plugins {
    kotlin("jvm") version "2.1.0"
    kotlin("plugin.serialization") version "2.1.0"
    application
}

group = "org.example"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    // 1. Ktor BOM: Forces ALL Ktor libraries (Client & Server) to version 3.0.0
    // This prevents the conflict between Koog's version and yours.
    implementation(platform("io.ktor:ktor-bom:3.0.0"))

    // 2. Koog AI Framework
    implementation("ai.koog:koog-agents:0.6.1")

    // 3. Ktor Server (Remove version numbers, let BOM handle it)
    implementation("io.ktor:ktor-server-core")
    implementation("io.ktor:ktor-server-netty")
    implementation("io.ktor:ktor-server-content-negotiation")
    implementation("io.ktor:ktor-serialization-kotlinx-json")

    // 4. Ktor Client (Remove version numbers)
    implementation("io.ktor:ktor-client-core")
    implementation("io.ktor:ktor-client-cio")
    implementation("io.ktor:ktor-client-content-negotiation")

    // 5. Standard Utilities
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.8.0")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.3")
    implementation("ch.qos.logback:logback-classic:1.4.14")
    implementation("io.github.microutils:kotlin-logging-jvm:3.0.5")
    implementation("io.github.cdimascio:dotenv-kotlin:6.4.1")

    // 6. Tools & Database
    implementation("com.anthropic:anthropic-java:0.8.0")
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("org.json:json:20231013")

    // Database (If you are using these yet)
    implementation("org.postgresql:postgresql:42.7.1")
    implementation("com.zaxxer:HikariCP:5.1.0")
    implementation("com.pgvector:pgvector:0.1.4")

    // Testing
    testImplementation(kotlin("test"))
}

kotlin {
    jvmToolchain(17)
}

application {
    mainClass.set("Application")
}

tasks.test {
    useJUnitPlatform()
}