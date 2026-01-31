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
    // Koog AI Agent framework
    implementation("ai.koog:koog-agents:0.6.1")

    // Kotlin coroutines
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.8.0")

    // Serialization
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.3")

    // Ktor HTTP client
    implementation("io.ktor:ktor-client-core:2.3.8")
    implementation("io.ktor:ktor-client-cio:2.3.8")
    implementation("io.ktor:ktor-client-content-negotiation:2.3.8")
    implementation("io.ktor:ktor-serialization-kotlinx-json:2.3.8")

    // Logging
    implementation("ch.qos.logback:logback-classic:1.4.14")
    implementation("io.github.microutils:kotlin-logging-jvm:3.0.5")

    // PostgreSQL driver
    implementation("org.postgresql:postgresql:42.7.1")

    // HikariCP connection pool
    implementation("com.zaxxer:HikariCP:5.1.0")

    // Anthropic Claude API client (CORRECTED NAME)
    implementation("com.anthropic:anthropic-java:0.8.0")

    // pgvector support for PostgreSQL
    implementation("com.pgvector:pgvector:0.1.4")

    // Anthropic Claude API client
    implementation("com.anthropic:anthropic-sdk-java:0.1.0") // If available

    // Environment variable loading
    implementation("io.github.cdimascio:dotenv-kotlin:6.4.1")

    // PostgreSQL driver
    implementation("org.postgresql:postgresql:42.7.1")

    // JSON parsing
    implementation("org.json:json:20231013")

    // OkHttp (REQUIRED for your LocationTools class)
    implementation("com.squareup.okhttp3:okhttp:4.12.0")

    // JSON parsing (REQUIRED for your LocationTools class)
    implementation("org.json:json:20231013")

    // Environment variable loading
    implementation("io.github.cdimascio:dotenv-kotlin:6.4.1")

    // Testing
    testImplementation(kotlin("test"))
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.8.0")
}

kotlin {
    jvmToolchain(17)
}

application {
    mainClass.set("medical-protocol-agent.MedicalAgentKt")
}

tasks.test {
    useJUnitPlatform()
}