plugins {
    id("java")
    id("com.gradleup.shadow") version "9.2.2"
}

group = "me.geyserextensionists"
version = "1.0.4"

repositories {
    mavenCentral()

    maven("https://repo.opencollab.dev/main/")
}

dependencies {
    compileOnly("org.geysermc.geyser:core:2.9.3-SNAPSHOT")
    compileOnly("org.geysermc.geyser:api:2.9.3-SNAPSHOT")

    implementation("org.spongepowered:configurate-yaml:4.2.0-GeyserMC-SNAPSHOT")
}

java {
    toolchain.languageVersion.set(JavaLanguageVersion.of(17))
}

tasks.compileJava {
    options.encoding = "UTF-8"
}

tasks.shadowJar {
    archiveFileName.set("${rootProject.name}-${version}.jar")

    relocate("org.spongepowered.configurate", "me.geyserextensionists.geyserdisplayentity.libs.configurate")
}

tasks.build {
    dependsOn("shadowJar")
}

tasks.processResources {
    val props = mapOf("version" to version)
    inputs.properties(props)
    filteringCharset = "UTF-8"
    filesMatching("extension.yml") {
        expand(props)
    }
}