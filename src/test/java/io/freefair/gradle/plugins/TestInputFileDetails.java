package io.freefair.gradle.plugins;

import org.gradle.api.internal.changedetection.rules.ChangeType;
import org.gradle.api.tasks.incremental.InputFileDetails;

import java.io.File;

public class TestInputFileDetails implements InputFileDetails {
    private final ChangeType changeType;
    private final File file;

    public TestInputFileDetails(ChangeType changeType, File file) {
        this.changeType = changeType;
        this.file = file;
    }

    @Override
    public boolean isAdded() {
        return changeType == ChangeType.ADDED;
    }

    @Override
    public boolean isModified() {
        return changeType == ChangeType.MODIFIED;
    }

    @Override
    public boolean isRemoved() {
        return changeType == ChangeType.REMOVED;
    }

    @Override
    public File getFile() {
        return file;
    }
}
