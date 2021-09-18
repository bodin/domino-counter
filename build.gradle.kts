plugins {
    `java-library`
    application
    id("org.openjfx.javafxplugin") version "0.0.10"
}

group = "org.example"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

application {
    mainClass.set("com.bodins.WebCam")
}

dependencies {
    implementation("org.openpnp:opencv:4.5.1-2")
    testImplementation("org.junit.jupiter:junit-jupiter-api:5.6.0")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine")
}

tasks.getByName<Test>("test") {
    useJUnitPlatform()
}
javafx {
    modules("javafx.controls", "javafx.swing")
}
