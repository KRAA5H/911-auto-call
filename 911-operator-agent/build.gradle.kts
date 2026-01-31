plugins {
    kotlin("jvm") version "2.2.0"
    kotlin("plugin.serialization") version "2.2.0"
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

    // Ktor Server (Required for the Webhook Architecture)
    implementation("io.ktor:ktor-server-core:2.3.8")
    implementation("io.ktor:ktor-server-netty:2.3.8")
    implementation("io.ktor:ktor-server-content-negotiation:2.3.8")

    // Ktor Client (Required for Agents to make API calls)
    implementation("io.ktor:ktor-client-core:2.3.8")
    implementation("io.ktor:ktor-client-cio:2.3.8")
    implementation("io.ktor:ktor-client-content-negotiation:2.3.8")
    implementation("io.ktor:ktor-serialization-kotlinx-json:2.3.8")

    // Logging
    implementation("ch.qos.logback:logback-classic:1.4.14")
    implementation("io.github.microutils:kotlin-logging-jvm:3.0.5")

    // SQLite Database
    implementation("org.xerial:sqlite-jdbc:3.45.1.0")
    implementation("com.zaxxer:HikariCP:5.1.0")
    
    // Exposed ORM (Kotlin-friendly database framework)
    implementation("org.jetbrains.exposed:exposed-core:0.47.0")
    implementation("org.jetbrains.exposed:exposed-dao:0.47.0")
    implementation("org.jetbrains.exposed:exposed-jdbc:0.47.0")
    implementation("org.jetbrains.exposed:exposed-java-time:0.47.0")

    // Anthropic Claude API client (The CORRECT one)
    implementation("com.anthropic:anthropic-java:0.8.0")


    // Environment variable loading
    implementation("io.github.cdimascio:dotenv-kotlin:6.4.1")

    // JSON parsing (Required for LocationTools)
    implementation("org.json:json:20231013")

    // OkHttp (Required for LocationTools)
    implementation("com.squareup.okhttp3:okhttp:4.12.0")

    // Testing
    testImplementation(kotlin("test"))
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.8.0")
}

kotlin {
    jvmToolchain(21)
}

application {
    mainClass.set("org.example.MainKt")
}

tasks.test {
    useJUnitPlatform()
}