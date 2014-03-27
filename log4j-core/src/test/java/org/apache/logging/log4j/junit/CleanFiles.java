package org.apache.logging.log4j.junit;

import org.junit.rules.ExternalResource;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.assertTrue;

/**
 * A JUnit test rule to automatically delete certain files before and after a test is run.
 */
public class CleanFiles extends ExternalResource {
    private final List<File> files;

    public CleanFiles(final File... files) {
        this.files = Arrays.asList(files);
    }

    public CleanFiles(final String... fileNames) {
        this.files = new ArrayList<File>(fileNames.length);
        for (final String fileName : fileNames) {
            this.files.add(new File(fileName));
        }
    }

    private void clean() {
        for (final File file : files) {
            delete(file);
        }
    }

    private static void delete(final File file) {
        if (file.exists()) {
            assertTrue(file.delete());
        }
    }

    @Override
    protected void before() throws Throwable {
        this.clean();
    }

    @Override
    protected void after() {
        this.clean();
    }
}
