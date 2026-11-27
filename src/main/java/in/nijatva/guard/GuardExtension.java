package in.nijatva.guard;

import java.util.ArrayList;
import java.util.List;

/**
 * Configuration for the no-GMS check.
 *
 * <pre>
 * guard {
 *     allowlist.add("com.google.mlkit:image-labeling")   // bundled model, no Play Services
 *     failOnViolation.set(true)
 * }
 * </pre>
 */
public class GuardExtension {

    /**
     * Group ids that must never appear on a classpath. Anything under these
     * prefixes is a violation, including transitives.
     */
    private final List<String> bannedGroups = new ArrayList<>(List.of(
            "com.google.android.gms",
            "com.google.firebase",
            "com.google.android.play",
            "com.google.android.ads",
            "com.google.android.tagmanager",
            "com.crashlytics.sdk.android",
            "com.google.ads.mediation"
    ));

    /**
     * Exact coordinates that are allowed even though their group is banned.
     * Use this sparingly and write down why next to the entry.
     */
    private final List<String> allowlist = new ArrayList<>();

    /**
     * Project dependencies that are allowed. Everything else that looks like a
     * sibling app module is a violation: apps talk over intents, not artifacts.
     */
    private final List<String> allowedProjectDependencies = new ArrayList<>(List.of(
            ":nijatva-core",
            ":nijatva-design"
    ));

    private boolean failOnViolation = true;

    public List<String> getBannedGroups() {
        return bannedGroups;
    }

    public List<String> getAllowlist() {
        return allowlist;
    }

    public List<String> getAllowedProjectDependencies() {
        return allowedProjectDependencies;
    }

    public boolean isFailOnViolation() {
        return failOnViolation;
    }

    public void setFailOnViolation(boolean failOnViolation) {
        this.failOnViolation = failOnViolation;
    }
}
