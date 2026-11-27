package in.nijatva.guard;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.gradle.testkit.runner.BuildResult;
import org.gradle.testkit.runner.GradleRunner;
import org.gradle.testkit.runner.UnexpectedBuildFailure;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/** Runs the plugin against real builds, so the resolution path is exercised. */
class GuardPluginTest {

    @TempDir
    Path projectDir;

    private GradleRunner runner(String buildScript) throws IOException {
        Files.writeString(projectDir.resolve("settings.gradle.kts"), "rootProject.name = \"sample\"\n");
        Files.writeString(projectDir.resolve("build.gradle.kts"), buildScript);
        return GradleRunner.create()
                .withProjectDir(projectDir.toFile())
                .withPluginClasspath()
                .withArguments("checkGuard", "--stacktrace");
    }

    @Test
    void passesWhenNothingBannedIsOnTheClasspath() throws IOException {
        BuildResult result = runner("""
                plugins {
                    java
                    id("in.nijatva.nijatva-guard")
                }
                repositories { mavenCentral() }
                dependencies { implementation("org.slf4j:slf4j-api:2.0.13") }
                """).build();

        assertEquals(org.gradle.testkit.runner.TaskOutcome.SUCCESS,
                result.task(":checkGuard").getOutcome());
    }

    @Test
    void failsOnAPlayServicesArtefact() throws IOException {
        GradleRunner runner = runner("""
                plugins {
                    java
                    id("in.nijatva.nijatva-guard")
                }
                repositories {
                    mavenCentral()
                    google()
                }
                dependencies { implementation("com.google.android.gms:play-services-basement:18.4.0") }
                """);

        UnexpectedBuildFailure failure = assertThrows(UnexpectedBuildFailure.class, runner::build);
        String message = failure.getMessage();
        assertTrue(message.contains("play-services-basement"),
                "the failure should name the offending artefact, got: " + message);
        assertTrue(message.contains("nijatva-guard"),
                "the failure should say which check failed, got: " + message);
    }

    @Test
    void anAllowlistedCoordinateIsNotAViolation() throws IOException {
        BuildResult result = runner("""
                plugins {
                    java
                    id("in.nijatva.nijatva-guard")
                }
                repositories {
                    mavenCentral()
                    google()
                }
                guard {
                    allowlist.add("com.google.android.gms:play-services-basement")
                }
                dependencies { implementation("com.google.android.gms:play-services-basement:18.4.0") }
                """).build();

        assertEquals(org.gradle.testkit.runner.TaskOutcome.SUCCESS,
                result.task(":checkGuard").getOutcome());
    }
}
