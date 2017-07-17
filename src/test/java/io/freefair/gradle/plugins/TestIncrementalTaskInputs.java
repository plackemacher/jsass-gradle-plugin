package io.freefair.gradle.plugins;

import org.gradle.api.Action;
import org.gradle.api.tasks.incremental.IncrementalTaskInputs;
import org.gradle.api.tasks.incremental.InputFileDetails;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class TestIncrementalTaskInputs implements IncrementalTaskInputs {
    private final boolean incremental;
    private final List<InputFileDetails> outOfDateInputFileDetails;
    private final List<InputFileDetails> removedInputFileDetails;

    private boolean outOfDateProcessed;
    private boolean removedProcessed;

    private TestIncrementalTaskInputs(boolean incremental, List<InputFileDetails> outOfDateInputFileDetails, List<InputFileDetails> removedInputFileDetails) {
        this.incremental = incremental;
        this.outOfDateInputFileDetails = outOfDateInputFileDetails;
        this.removedInputFileDetails = removedInputFileDetails;
    }

    @Override
    public boolean isIncremental() {
        return incremental;
    }

    @Override
    public void outOfDate(Action<? super InputFileDetails> outOfDateAction) {
        if (outOfDateProcessed) {
            throw new IllegalStateException("Cannot process outOfDate files multiple times");
        }
        outOfDateInputFileDetails.forEach(outOfDateAction::execute);
        outOfDateProcessed = true;
    }

    @Override
    public void removed(Action<? super InputFileDetails> removedAction) {
        if (!outOfDateProcessed) {
            throw new IllegalStateException("Must first process outOfDate files before processing removed files");
        }
        if (removedProcessed) {
            throw new IllegalStateException("Cannot process removed files multiple times");
        }
        removedInputFileDetails.forEach(removedAction::execute);
        removedProcessed = true;
    }

    public static class Builder {
        private boolean incremental;
        private List<InputFileDetails> outOfDateInputFileDetails;
        private List<InputFileDetails> removedInputFileDetails;

        public Builder setIncremental(boolean incremental) {
            this.incremental = incremental;
            return this;
        }

        public Builder addOutOfDateInputFileDetails(InputFileDetails inputFileDetails) {
            if (outOfDateInputFileDetails == null) {
                outOfDateInputFileDetails = new ArrayList<>();
            }

            outOfDateInputFileDetails.add(inputFileDetails);
            return this;
        }

        public Builder addRemovedInputFileDetails(InputFileDetails inputFileDetails) {
            if (removedInputFileDetails == null) {
                removedInputFileDetails = new ArrayList<>();
            }

            removedInputFileDetails.add(inputFileDetails);
            return this;
        }

        public TestIncrementalTaskInputs build() {
            if (outOfDateInputFileDetails == null) {
                outOfDateInputFileDetails = Collections.emptyList();
            }
            if (removedInputFileDetails == null) {
                removedInputFileDetails = Collections.emptyList();
            }

            return new TestIncrementalTaskInputs(incremental, outOfDateInputFileDetails, removedInputFileDetails);
        }
    }
}
