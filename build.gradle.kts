plugins {
    id("java-gradle-plugin")
    id("maven-publish")
}

repositories {
    mavenCentral()
}

group = "in.nijatva"
version = "0.1.0"

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(17))
    }
}

gradlePlugin {
    plugins {
        create("noGmsCheck") {
            id = "in.nijatva.no-gms-check"
            implementationClass = "in.nijatva.nogms.NoGmsCheckPlugin"
            displayName = "Nijatva no-GMS check"
            description = "Fails the build if a Google Play Services or Firebase artefact is present."
        }
    }
}

tasks.withType<JavaCompile>().configureEach {
    options.encoding = "UTF-8"
    options.compilerArgs.add("-Xlint:all")
}

dependencies {
    testImplementation(gradleTestKit())
    testImplementation("org.junit.jupiter:junit-jupiter:5.10.2")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

tasks.test {
    useJUnitPlatform()
    testLogging {
        events("passed", "failed", "skipped")
        exceptionFormat = org.gradle.api.tasks.testing.logging.TestExceptionFormat.FULL
    }
}
