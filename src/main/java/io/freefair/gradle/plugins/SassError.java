package io.freefair.gradle.plugins;

import lombok.Getter;
import lombok.Setter;

/**
 * @author Lars Grefer
 */
@Getter
@Setter
public class SassError {

    private int status;
    private String file;
    private int line;
    private int column;
    private String message;
    private String formatted;
}
