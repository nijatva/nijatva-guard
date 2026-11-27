package in.nijatva.guard;

import java.util.List;

import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.tasks.TaskProvider;

/**
 * Registers the {@code checkGuard} task and wires it into {@code check}.
 *
 * <pre>
 * plugins { id("in.nijatva.guard") version "0.1.0" }
 * </pre>
 */
public class GuardPlugin implements Plugin<Project> {

    @Override
    public void apply(Project project) {
        GuardExtension extension =
                project.getExtensions().create("guard", GuardExtension.class);

        TaskProvider<GuardTask> task = project.getTasks().register("checkGuard", GuardTask.class, t -> {
            t.setGroup("verification");
            t.setDescription("Fails the build if a Google Play Services or Firebase artefact is on any classpath.");

            List<String> banned = extension.getBannedGroups();
            List<String> allowed = extension.getAllowlist();
            List<String> allowedProjects = extension.getAllowedProjectDependencies();

            t.getBannedGroups().set(project.provider(() -> banned));
            t.getAllowlist().set(project.provider(() -> allowed));
            t.getAllowedProjectDependencies().set(project.provider(() -> allowedProjects));
            t.getFailOnViolation().set(project.provider(extension::isFailOnViolation));

            // Only the classpaths that end up inside the APK matter. Checking every
            // configuration would resolve the build tooling's own classpath too.
            t.getCheckedConfigurations().set(project.provider(() ->
                    project.getConfigurations().stream()
                            .filter(Configuration::isCanBeResolved)
                            .filter(c -> c.getName().endsWith("RuntimeClasspath")
                                    || c.getName().endsWith("CompileClasspath"))
                            .filter(c -> !c.getName().startsWith("lint"))
                            .toList()));
        });

        // Attach to `check` without assuming it exists yet.
        project.getTasks().matching(t -> t.getName().equals("check"))
                .configureEach(check -> check.dependsOn(task));
    }
}
