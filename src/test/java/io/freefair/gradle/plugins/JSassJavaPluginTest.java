package io.freefair.gradle.plugins;

import io.freefair.gradle.plugins.jsass.SassCompile;
import org.gradle.api.Action;
import org.gradle.api.Project;
import org.gradle.api.internal.changedetection.changes.RebuildIncrementalTaskInputs;
import org.gradle.api.tasks.incremental.InputFileDetails;
import org.gradle.internal.impldep.com.google.common.io.Files;
import org.gradle.testfixtures.ProjectBuilder;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Lars Grefer
 * @author Pat Lackemacher
 */
public class JSassJavaPluginTest {

    private Project project;

    @Rule
    public final TemporaryFolder testProjectDir = new TemporaryFolder();

    @Before
    public void setUp() {
        project = ProjectBuilder.builder()
                .withProjectDir(testProjectDir.getRoot())
                .build();
    }

    @Test
    public void testSources() throws IOException {
        File cssFolder = testProjectDir.newFolder("src", "main", "resources", "sass").getCanonicalFile();
        File mainCss = new File(cssFolder, "main.scss");

        boolean isNewFile = mainCss.createNewFile();
        assertThat(isNewFile).isTrue();

        Files.write("body { color: red; }", mainCss, Charset.defaultCharset());

        project.getPlugins().apply(JSassJavaPlugin.class);

        SassCompile compileSass = (SassCompile) project.getTasks().getByName("compileSass");

        RebuildIncrementalTaskInputs incrementalTaskInputs = new RebuildIncrementalTaskInputs(compileSass) {
            @Override
            public void doOutOfDate(Action<? super InputFileDetails> outOfDateAction) {
                getDiscoveredInputs().forEach(file -> outOfDateAction.execute(new InputFileDetails() {
                    @Override
                    public boolean isAdded() {
                        return true;
                    }

                    @Override
                    public boolean isModified() {
                        return false;
                    }

                    @Override
                    public boolean isRemoved() {
                        return false;
                    }

                    @Override
                    public File getFile() {
                        return file;
                    }
                }));
            }
        };
        incrementalTaskInputs.newInput(mainCss);

        compileSass.compileSass(incrementalTaskInputs);

        File buildDir = project.getBuildDir();
        assertThat(new File(buildDir, "resources/main/sass/main.css")).exists();
        assertThat(new File(buildDir, "resources/main/sass/main.css.map")).exists();
    }

    @Test
    public void testChanges() throws Exception {
        File cssFolder = testProjectDir.newFolder("src", "main", "resources", "sass").getCanonicalFile();
        String rootName = "changes";
        File mainCss = new File(cssFolder, rootName + ".scss");
        Files.write("body { color: red; }", mainCss, Charset.defaultCharset());

        project.getPlugins().apply(JSassJavaPlugin.class);

        SassCompile compileSass = (SassCompile) project.getTasks().getByName("compileSass");

        RebuildIncrementalTaskInputs incrementalTaskInputs = new RebuildIncrementalTaskInputs(compileSass) {
            @Override
            public void doOutOfDate(Action<? super InputFileDetails> outOfDateAction) {
                getDiscoveredInputs().forEach(file -> outOfDateAction.execute(new InputFileDetails() {
                    @Override
                    public boolean isAdded() {
                        return true;
                    }

                    @Override
                    public boolean isModified() {
                        return false;
                    }

                    @Override
                    public boolean isRemoved() {
                        return false;
                    }

                    @Override
                    public File getFile() {
                        return file;
                    }
                }));
            }
        };
        incrementalTaskInputs.newInput(mainCss);

        compileSass.compileSass(incrementalTaskInputs);

        File buildDir = project.getBuildDir();
        File cssFile = new File(buildDir, "resources/main/sass/" + rootName + ".css");
        File cssMapFile = new File(buildDir, "resources/main/sass/" + rootName + ".css.map");
        assertThat(cssFile).exists();
        assertThat(cssMapFile).exists();

        List<String> cssFileLines = Files.readLines(cssFile, Charset.defaultCharset());

        // Change color to blue
        Files.write("body { color: blue; }", mainCss, Charset.defaultCharset());

        incrementalTaskInputs = new RebuildIncrementalTaskInputs(compileSass) {
            @Override
            public void doOutOfDate(Action<? super InputFileDetails> outOfDateAction) {
                getDiscoveredInputs().forEach(file -> outOfDateAction.execute(new InputFileDetails() {
                    @Override
                    public boolean isAdded() {
                        return false;
                    }

                    @Override
                    public boolean isModified() {
                        return true;
                    }

                    @Override
                    public boolean isRemoved() {
                        return false;
                    }

                    @Override
                    public File getFile() {
                        return file;
                    }
                }));
            }
        };
        incrementalTaskInputs.newInput(mainCss);

        compileSass.compileSass(incrementalTaskInputs);

        assertThat(cssFile).exists();
        assertThat(cssMapFile).exists();

        List<String> incrementalCssFileLines = Files.readLines(cssFile, Charset.defaultCharset());

        assertThat(incrementalCssFileLines.get(1)).isNotEqualTo(cssFileLines.get(1));
    }

    @Test
    public void testRemoved() throws Exception {
        File cssFolder = testProjectDir.newFolder("src", "main", "resources", "sass").getCanonicalFile();
        String rootName = "removed";
        File mainCss = new File(cssFolder, rootName + ".scss");
        Files.write("body { color: red; }", mainCss, Charset.defaultCharset());

        project.getPlugins().apply(JSassJavaPlugin.class);

        SassCompile compileSass = (SassCompile) project.getTasks().getByName("compileSass");

        RebuildIncrementalTaskInputs incrementalTaskInputs = new RebuildIncrementalTaskInputs(compileSass) {
            @Override
            public void doOutOfDate(Action<? super InputFileDetails> outOfDateAction) {
                getDiscoveredInputs().forEach(file -> outOfDateAction.execute(new InputFileDetails() {
                    @Override
                    public boolean isAdded() {
                        return true;
                    }

                    @Override
                    public boolean isModified() {
                        return false;
                    }

                    @Override
                    public boolean isRemoved() {
                        return false;
                    }

                    @Override
                    public File getFile() {
                        return file;
                    }
                }));
            }
        };
        incrementalTaskInputs.newInput(mainCss);

        compileSass.compileSass(incrementalTaskInputs);

        assertThat(mainCss).exists();
        File buildDir = project.getBuildDir();
        File cssFile = new File(buildDir, "resources/main/sass/" + rootName + ".css");
        File cssMapFile = new File(buildDir, "resources/main/sass/" + rootName + ".css.map");
        assertThat(cssFile).exists();
        assertThat(cssMapFile).exists();

        assertThat(mainCss.delete()).isTrue();
        assertThat(mainCss).doesNotExist();

        incrementalTaskInputs = new RebuildIncrementalTaskInputs(compileSass) {
            @Override
            public void doRemoved(Action<? super InputFileDetails> removedAction) {
                getDiscoveredInputs().forEach(file -> removedAction.execute(new InputFileDetails() {
                    @Override
                    public boolean isAdded() {
                        return false;
                    }

                    @Override
                    public boolean isModified() {
                        return false;
                    }

                    @Override
                    public boolean isRemoved() {
                        return true;
                    }

                    @Override
                    public File getFile() {
                        return file;
                    }
                }));
            }

            @Override
            public void doOutOfDate(Action<? super InputFileDetails> outOfDateAction) {
            }
        };
        incrementalTaskInputs.newInput(mainCss);

        compileSass.compileSass(incrementalTaskInputs);

        assertThat(cssFile).doesNotExist();
        assertThat(cssMapFile).doesNotExist();
    }

    @Test
    public void testIncrementalDelete() throws Exception {
        String rootName = "incrementalDelete";

        File buildDir = project.getBuildDir();
        File cssFile = new File(buildDir, "resources/main/sass/" + rootName + ".css");
        File cssMapFile = new File(buildDir, "resources/main/sass/" + rootName + ".css.map");
        Files.createParentDirs(cssFile);
        assertThat(cssFile.createNewFile()).isTrue();
        assertThat(cssMapFile.createNewFile()).isTrue();

        project.getPlugins().apply(JSassJavaPlugin.class);

        SassCompile compileSass = (SassCompile) project.getTasks().getByName("compileSass");

        RebuildIncrementalTaskInputs incrementalTaskInputs = new RebuildIncrementalTaskInputs(compileSass);

        compileSass.compileSass(incrementalTaskInputs);

        assertThat(cssFile).doesNotExist();
        assertThat(cssMapFile).doesNotExist();
    }
}
