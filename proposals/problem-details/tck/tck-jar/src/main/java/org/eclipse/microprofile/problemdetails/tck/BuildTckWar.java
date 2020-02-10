package org.eclipse.microprofile.problemdetails.tck;

import org.testcontainers.utility.MountableFile;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static java.nio.file.Files.isDirectory;
import static java.nio.file.Files.move;
import static java.nio.file.StandardCopyOption.REPLACE_EXISTING;
import static org.eclipse.microprofile.problemdetails.tck.ContainerLaunchingExtension.buildJeeContainer;

public class BuildTckWar {
    /** Dev tool: build a modified tck.war */
    public static void main(String[] args) throws IOException {
        List<String> libs = new ArrayList<>();
        Path outputFile = null;

        for (int i = 0; i < args.length; i++) {
            String arg = args[i];
            switch (arg) {
                case "-o":
                case "--output-file":
                    outputFile = Paths.get(args[++i]);
                    break;
                default:
                    libs.add(arg);
            }
        }

        Path warFile = buildWar(libs);
        System.out.println(warFile);

        if (outputFile != null) {
            if (isDirectory(outputFile))
                outputFile = outputFile.resolve(warFile.getFileName());
            System.out.println("=> " + outputFile);
            move(warFile, outputFile, REPLACE_EXISTING);
        }
    }

    private static Path buildWar(List<String> libs) {
        Map<MountableFile, String> mountableFiles = buildJeeContainer(libs.stream())
            .getCopyToFileContainerPathMap();
        MountableFile mountableFile = mountableFiles.entrySet().iterator().next().getKey();
        return Paths.get(mountableFile.getFilesystemPath());
    }
}
