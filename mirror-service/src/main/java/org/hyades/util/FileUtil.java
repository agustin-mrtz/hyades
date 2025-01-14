package org.hyades.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class FileUtil {

    private static final Logger LOGGER = LoggerFactory.getLogger(FileUtil.class);
    public static void deleteFileAndDir(Path filePath) {
        try {
            boolean fileDeleted = filePath.toFile().delete();
            if (fileDeleted) {
                boolean dirDeleted = filePath.getParent().toFile().delete();
                if (!dirDeleted) {
                    LOGGER.info("Error while deleting the directory path %s", filePath.getParent().getFileName().toString());
                }
            }
        } catch (Exception e) {
            LOGGER.info("Error while deleting the file path %s : %s", filePath.getFileName().toString(), e);
        }
    }

    public static Path getTempFileLocation(String dirName, String fileSuffix) throws IOException {
        Path tempDir = Files.createTempDirectory(dirName);
        Path tempFile = Files.createTempFile(tempDir, "", fileSuffix);
        return tempFile;
    }
}
