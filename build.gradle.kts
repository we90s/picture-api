val ktor_version: String by project
val kotlin_version: String by project
val logback_version: String by project

val exposed_version: String by project
val h2_version: String by project
plugins {
    kotlin("jvm") version "1.9.22"
    id("io.ktor.plugin") version "2.3.7"
    id("org.jetbrains.kotlin.plugin.serialization") version "1.9.22"
}

group = "site.petpic.api"
version = "0.0.1"

application {
    mainClass.set("site.petpic.api.ApplicationKt")

    val isDevelopment: Boolean = project.ext.has("development")
    applicationDefaultJvmArgs = listOf("-Dio.ktor.development=$isDevelopment")
}

repositories {
    mavenCentral()
}

dependencies {
    implementation("io.ktor:ktor-server-core-jvm")
    implementation("io.ktor:ktor-server-auth-jvm")
    implementation("io.ktor:ktor-server-auth-jwt-jvm")
    implementation("io.ktor:ktor-server-cors-jvm")
    implementation("io.ktor:ktor-server-openapi")
    implementation("io.ktor:ktor-server-swagger-jvm")
    implementation("io.ktor:ktor-server-content-negotiation-jvm")
    implementation("io.ktor:ktor-serialization-kotlinx-json-jvm")
    implementation("org.jetbrains.exposed:exposed-core:$exposed_version")
    implementation("org.jetbrains.exposed:exposed-jdbc:$exposed_version")

    implementation("io.ktor:ktor-server-netty-jvm")
    implementation("ch.qos.logback:logback-classic:$logback_version")
    testImplementation("io.ktor:ktor-server-tests-jvm")
    testImplementation("org.jetbrains.kotlin:kotlin-test-junit:$kotlin_version")


    implementation("io.ktor:ktor-server-config-yaml-jvm:2.3.7")
    implementation("org.apache.kafka:kafka-clients:3.6.1")
    // https://mvnrepository.com/artifact/com.google.cloud/google-cloud-storage
    implementation("com.google.cloud:google-cloud-storage:2.32.1")
    implementation("com.google.cloud:google-cloud-vision:3.31.0")
    // https://mvnrepository.com/artifact/mysql/mysql-connector-java
    //implementation("mysql:mysql-connector-java:8.0.33")
    implementation("org.postgresql:postgresql:42.7.4")
    implementation("io.ktor:ktor-client-cio:$ktor_version")

    implementation("org.jetbrains.exposed:exposed-java-time:$exposed_version")
    implementation ("org.jetbrains.kotlinx:kotlinx-serialization-json:1.4.1")
}
