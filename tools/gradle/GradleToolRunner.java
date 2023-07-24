package tools.gradle;

import org.gradle.tooling.GradleConnector;
import org.gradle.tooling.ProgressEvent;
import org.gradle.tooling.ProgressListener;
import org.gradle.tooling.ProjectConnection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;

/**
 * TBD.
 */
public class GradleToolRunner {
    private static Logger logging = LoggerFactory.getLogger(GradleToolRunner.class);

    /**
     * TBD.
     *
     * @param args Command-line arguments to parse and apply.
     */
    public static void main(String[] args) throws IOException {
        if (args.length < 1) {
            System.out.println("usage: java -jar gradle-tool-runner.jar <gradle-root> (options)");
            System.exit(1);
            return;
        }
        final String root = args[0];
        List<String> allArgs = Arrays.asList(args);
        List<String> taskList = allArgs.subList(1, allArgs.size() - 1);
        ArrayList<String> taskArgs = new ArrayList<>(taskList.size());
        taskArgs.addAll(taskList);

        if (taskArgs.isEmpty()) {
            // run `build` task by default
            taskArgs.add("build");
        }
        logging.debug("Gradle tool root: " + root);
        var path = Path.of(root);

        try {
            if (!Files.exists(path) || !Files.isReadable(path)) {
                logging.error("could not find gradle tool root or can't read");
                System.exit(1);
                return;
            }

            logging.info(
                "Launching Gradle embedded build (tasks: \"" + String.join("\", \"", taskList) + "\")"
            );
            try (ProjectConnection connection = GradleConnector.newConnector()
                    .forProjectDirectory(path.toFile())
                    .connect()) {
                connection.newBuild()
                        .forTasks(taskArgs.toArray(new String[0]))
                        .setColorOutput(true)
                        .addArguments("--info")
                        .setStandardInput(System.in)
                        .setStandardOutput(System.out)
                        .setStandardError(System.err)
                        .addProgressListener((ProgressListener) progressEvent ->
                                logging.info("Gradle status: " + progressEvent.getDescription())
                        ).run();
            }
        } catch (RuntimeException e) {
            logging.error("could not find gradle tool root (error)", e);
            // print cwd
            logging.error("Current working directory: " + System.getProperty("user.dir"));
            Files.list(path).forEach(path1 -> logging.error("Path:  " + path1));
            System.exit(1);
        }
    }
}
