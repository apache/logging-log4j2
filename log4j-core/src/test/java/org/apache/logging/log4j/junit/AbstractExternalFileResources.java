package org.apache.logging.log4j.junit;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.rules.ExternalResource;

public abstract class AbstractExternalFileResources extends ExternalResource {

    private final List<File> files;

    public AbstractExternalFileResources(final File... files) {
        this.files = Arrays.asList(files);
    }

    public AbstractExternalFileResources(final String... fileNames) {
        this.files = new ArrayList<>(fileNames.length);
        for (final String fileName : fileNames) {
            this.files.add(new File(fileName));
        }
    }

    public List<File> getFiles() {
        return files;
    }

    @Override
    public String toString() {
        final StringBuilder builder = new StringBuilder();
        builder.append(getClass().getSimpleName());
        builder.append(" [");
        builder.append(files);
        builder.append("]");
        return builder.toString();
    }

}
