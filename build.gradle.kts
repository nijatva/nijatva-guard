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
        create("guard") {
            id = "in.nijatva.guard"
            implementationClass = "in.nijatva.guard.GuardPlugin"
            displayName = "Nijatva Guard"
            description = "Fails the build if Google Play Services, Firebase, Play Core or an ad/analytics SDK is present."
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

// Publishes the plugin marker and the jar to GitHub Packages, so an app build can
// resolve: plugins { id("in.nijatva.guard") version "0.1.0" }
publishing {
    repositories {
        maven {
            name = "GitHubPackages"
            url = uri("https://maven.pkg.github.com/nijatva/nijatva-guard")
            credentials {
                username = System.getenv("GITHUB_ACTOR") ?: ""
                password = System.getenv("GITHUB_TOKEN") ?: ""
            }
        }
    }
}
