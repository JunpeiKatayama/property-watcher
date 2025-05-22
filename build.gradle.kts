plugins {
    kotlin("jvm") version "1.9.20"
    kotlin("plugin.serialization") version "1.9.20"
    application
}

group = "com.property.watcher"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    // Kotlin標準ライブラリ
    implementation("org.jetbrains.kotlin:kotlin-stdlib")
    
    // コルーチン
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.3")
    
    // HTTP通信
    implementation("io.ktor:ktor-client-core:2.3.6")
    implementation("io.ktor:ktor-client-cio:2.3.6")
    
    // HTMLパース
    implementation("org.jsoup:jsoup:1.16.2")
    
    // JSON処理
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.0")
    
    // LINE Messaging API SDK
    implementation("com.linecorp.bot:line-bot-api-client:5.0.0")
    implementation("com.linecorp.bot:line-bot-model:5.0.0")
    
    // ロギング
    implementation("ch.qos.logback:logback-classic:1.4.11")
    
    // テスト
    testImplementation("org.jetbrains.kotlin:kotlin-test")

    implementation("com.fasterxml.jackson.core:jackson-databind:2.15.2")

    implementation("com.squareup.okhttp3:okhttp:4.9.3")
    implementation("com.squareup.okhttp3:logging-interceptor:4.9.3")
    implementation("com.squareup.retrofit2:retrofit:2.9.0")
    implementation("com.squareup.retrofit2:converter-jackson:2.9.0")
    implementation("org.slf4j:slf4j-simple:1.7.32")
}

application {
    mainClass.set("com.property.watcher.MainKt")
}

tasks.test {
    useJUnitPlatform()
}

kotlin {
    jvmToolchain(21)
} 