val ktor_version: String by project
val kotlin_version: String by project
val logback_version: String by project
val kmongo_version: String by project

plugins {
    application
    kotlin("jvm") version "1.6.10"
}

group = "com.example"
version = "0.0.1"
application {
    mainClass.set("io.ktor.server.netty.EngineMain")

    val isDevelopment: Boolean = project.ext.has("development")
    applicationDefaultJvmArgs = listOf("-Dio.ktor.development=$isDevelopment")
}

repositories {
    mavenCentral()
    maven { url = uri("https://maven.pkg.jetbrains.space/public/p/ktor/eap") }
}

dependencies {
    implementation("ch.qos.logback:logback-classic:$logback_version")
    implementation("io.ktor:ktor-server-core-jvm:2.3.1")
    implementation("io.ktor:ktor-server-content-negotiation-jvm:2.3.1")
    implementation("io.ktor:ktor-serialization-gson-jvm:2.3.1")
    implementation("io.ktor:ktor-server-netty-jvm:2.3.1")
    testImplementation("org.jetbrains.kotlin:kotlin-test-junit:$kotlin_version")

    // KMongo
    implementation("org.litote.kmongo:kmongo:$kmongo_version")
    implementation("org.litote.kmongo:kmongo-coroutine:$kmongo_version")

    //implementation("com.google.code.gson:gson:2.9.0")

    //password security
    implementation("commons-codec:commons-codec:1.15")
    testImplementation("io.ktor:ktor-server-tests-jvm:2.3.1")
}