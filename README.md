# no-gms-check

A Gradle plugin that fails the build if Google Play Services or Firebase is on
any classpath. The suite's promise is that no app links them; this is the thing
that makes the promise a build failure instead of a good intention.

```kotlin
plugins {
    id("in.nijatva.no-gms-check") version "0.1.0"
}
```

`checkNoGms` runs as part of `check`, so a normal `./gradlew build` catches it.

## What it catches

- Direct dependencies on a banned group.
- **Transitive** dependencies on a banned group — a library that quietly pulls in
  `play-services-basement` fails the build exactly like a direct one.
- Project dependencies on a sibling app module. Apps talk over intents, not
  artifacts; see [nijatva-core/INTEROP.md](https://github.com/nijatva/nijatva-core/blob/main/INTEROP.md).

Banned groups by default:

```
com.google.android.gms
com.google.firebase
com.google.android.play
com.google.android.ads
com.google.android.tagmanager
com.crashlytics.sdk.android
com.google.ads.mediation
```

## What it deliberately allows

`com.google.mlkit:*` — the **bundled** ML Kit variants. They ship the model inside
the APK and do not need Play Services. The Play Services variants live under
`com.google.android.gms:play-services-mlkit-*`, which is banned by group, so the
two cannot be confused.

Nijatva Photos uses `com.google.mlkit:image-labeling`, `face-detection` and
`text-recognition`. Those are bundled and pass.

## Configuration

```kotlin
noGmsCheck {
    // exact coordinates that are fine even though their group is banned
    allowlist.add("com.google.android.gms:play-services-basement")

    // project dependencies that are allowed; everything else that looks like a
    // sibling app module is a violation
    allowedProjectDependencies.add(":nijatva-core")

    // report instead of failing, for a migration in progress
    failOnViolation = false
}
```

Every entry in `allowlist` should have a comment next to it saying why. An
allowlist with no reasons is how a guard quietly stops guarding.

## Using it before it is published

The plugin is not on Maven Central yet. Until it is, include it as a build:

```kotlin
// settings.gradle.kts
includeBuild("../no-gms-check")
```

```kotlin
// app/build.gradle.kts
plugins {
    id("in.nijatva.no-gms-check")
}
```

## Status

Version 0.1.0. The functional tests build a real project with Gradle TestKit:
one clean classpath, one with `play-services-basement`, one with an allowlisted
coordinate. CI runs them on every push.

Not done yet: publishing to a repository, and a CI action wrapper for repositories
that build without this plugin.

## Licence

Apache-2.0.
