plugins {
    id("java")
    id("com.gradleup.shadow") version "9.5.0"
}

group = "de.tasticgames"
version = "0.1.0-SNAPSHOT"

repositories {
    mavenCentral()

    maven {
        name = "papermc"
        url = uri(
            "https://repo.papermc.io/repository/maven-public/"
        )
    }
}

dependencies {
    compileOnly(
        "io.papermc.paper:paper-api:1.21.11-R0.1-SNAPSHOT"
    )

    implementation(
        "de.tasticgames:tasticgames-api-client:0.1.0-SNAPSHOT"
    )

    testImplementation(
        platform("org.junit:junit-bom:6.0.0")
    )
    testImplementation(
        "org.junit.jupiter:junit-jupiter"
    )
    testRuntimeOnly(
        "org.junit.platform:junit-platform-launcher"
    )
}

tasks.withType<JavaCompile>().configureEach {
    options.release.set(25)
    options.encoding = "UTF-8"
}

tasks.test {
    useJUnitPlatform()
}

tasks.processResources {
    inputs.property("version", project.version)

    filesMatching("plugin.yml") {
        expand(
            "version" to project.version
        )
    }
}

tasks.shadowJar {
    archiveClassifier.set("")

    duplicatesStrategy = DuplicatesStrategy.EXCLUDE

    exclude(
        "META-INF/LICENSE",
        "META-INF/LICENSE.txt",
        "META-INF/NOTICE",
        "META-INF/NOTICE.txt",
        "META-INF/DEPENDENCIES",
        "META-INF/INDEX.LIST"
    )

    relocate(
        "de.tasticgames.client",
        "de.tasticgames.libs.apiclient"
    )

    relocate(
        "com.fasterxml.jackson",
        "de.tasticgames.libs.jackson"
    )

    mergeServiceFiles()
}

tasks.jar {
    enabled = false
}

tasks.build {
    dependsOn(tasks.shadowJar)
}