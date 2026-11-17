package in.nijatva.nogms;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.gradle.api.DefaultTask;
import org.gradle.api.GradleException;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.artifacts.ModuleIdentifier;
import org.gradle.api.artifacts.component.ComponentIdentifier;
import org.gradle.api.artifacts.component.ModuleComponentIdentifier;
import org.gradle.api.artifacts.component.ProjectComponentIdentifier;
import org.gradle.api.artifacts.result.ResolvedComponentResult;
import org.gradle.api.provider.ListProperty;
import org.gradle.api.provider.Property;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.Internal;
import org.gradle.api.tasks.TaskAction;

/**
 * Resolves the compile and runtime classpaths and fails if a banned group is on
 * any of them. Transitives count: a library that pulls in Play Services is as
 * much a violation as a direct dependency.
 */
public abstract class NoGmsCheckTask extends DefaultTask {

    @Input
    public abstract ListProperty<String> getBannedGroups();

    @Input
    public abstract ListProperty<String> getAllowlist();

    @Input
    public abstract ListProperty<String> getAllowedProjectDependencies();

    @Input
    public abstract Property<Boolean> getFailOnViolation();

    @Internal
    public abstract ListProperty<Configuration> getCheckedConfigurations();

    @TaskAction
    public void check() {
        Set<String> violations = new LinkedHashSet<>();
        List<String> banned = getBannedGroups().get();
        List<String> allowed = getAllowlist().get();
        List<String> allowedProjects = getAllowedProjectDependencies().get();

        for (Configuration configuration : getCheckedConfigurations().get()) {
            if (!configuration.isCanBeResolved()) {
                continue;
            }
            Set<ResolvedComponentResult> components;
            try {
                components = configuration.getIncoming().getResolutionResult().getAllComponents();
            } catch (Exception e) {
                // An unresolvable configuration is not this task's problem.
                getLogger().info("no-gms-check: skipped {} ({})", configuration.getName(), e.getMessage());
                continue;
            }
            for (ResolvedComponentResult component : components) {
                ComponentIdentifier id = component.getId();
                if (id instanceof ModuleComponentIdentifier) {
                    ModuleComponentIdentifier module = (ModuleComponentIdentifier) id;
                    String coordinate = module.getGroup() + ":" + module.getModule();
                    if (allowed.contains(coordinate)) {
                        continue;
                    }
                    for (String group : banned) {
                        if (module.getGroup().equals(group) || module.getGroup().startsWith(group + ".")) {
                            violations.add(coordinate + "  (in " + configuration.getName() + ")");
                            break;
                        }
                    }
                } else if (id instanceof ProjectComponentIdentifier) {
                    String path = ((ProjectComponentIdentifier) id).getProjectPath();
                    if (path == null || path.equals(":") || path.isEmpty()) {
                        continue;
                    }
                    if (allowedProjects.contains(path)) {
                        continue;
                    }
                    if (path.contains("nijatva-")) {
                        violations.add(path + "  (project dependency, in " + configuration.getName() + ")");
                    }
                }
            }
        }

        if (violations.isEmpty()) {
            getLogger().lifecycle("no-gms-check: clean, no Play Services or Firebase on any classpath");
            return;
        }

        List<String> sorted = new ArrayList<>(violations);
        sorted.sort(String::compareTo);
        StringBuilder message = new StringBuilder();
        message.append("\n\nno-gms-check: this build pulls in something the suite does not allow.\n\n");
        for (String violation : sorted) {
            message.append("  ").append(violation).append('\n');
        }
        message.append("\nPlay Services and Firebase are the one thing Nijatva promises never to ship.\n")
                .append("Remove the dependency, or replace it with the bundled alternative:\n")
                .append("  maps          -> osmdroid or MapLibre + OpenStreetMap\n")
                .append("  push          -> UnifiedPush, ntfy, or a foreground socket\n")
                .append("  ML            -> com.google.mlkit bundled variants, TFLite, whisper.cpp, Vosk\n")
                .append("  crash reports -> nothing, or a local log export\n")
                .append("\nIf an entry above is a false positive, add its exact coordinate to\n")
                .append("noGmsCheck { allowlist.add(\"...\") } and say why in the build file.\n");

        if (getFailOnViolation().get()) {
            throw new GradleException(message.toString());
        }
        getLogger().warn(message.toString());
    }
}
