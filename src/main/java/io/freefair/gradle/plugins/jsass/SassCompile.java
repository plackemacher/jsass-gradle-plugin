package io.freefair.gradle.plugins.jsass;

import com.google.gson.Gson;
import io.bit3.jsass.*;
import io.bit3.jsass.Compiler;
import io.bit3.jsass.annotation.DebugFunction;
import io.bit3.jsass.annotation.ErrorFunction;
import io.bit3.jsass.annotation.WarnFunction;
import io.bit3.jsass.importer.Importer;
import lombok.Getter;
import lombok.Setter;
import org.codehaus.groovy.runtime.ResourceGroovyMethods;
import org.gradle.api.file.ConfigurableFileTree;
import org.gradle.api.file.FileCollection;
import org.gradle.api.file.FileTree;
import org.gradle.api.internal.ConventionTask;
import org.gradle.api.tasks.*;
import org.gradle.api.tasks.incremental.IncrementalTaskInputs;
import org.gradle.api.tasks.incremental.InputFileDetails;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

/**
 * @author Lars Grefer
 * @author Pat Lackemacher
 */
@Getter
@Setter
public class SassCompile extends ConventionTask {

    @InputFiles
    public FileTree getSourceFiles() {
        ConfigurableFileTree files = getProject().fileTree(new File(sourceDir, sassPath));
        files.include("**/*.scss", "**/*.sass");
        return files;
    }

    @OutputFiles
    public FileTree getOutputFiles() {
        ConfigurableFileTree files = getProject().fileTree(new File(getDestinationDir(), cssPath));
        files.include("**/*.css", "**/*.css.map");
        return files;
    }

    @Internal
    private File sourceDir;

    @Internal
    private File destinationDir;

    @Input
    private String cssPath = "";

    @Input
    private String sassPath = "";

    @Internal
    private Compiler compiler;

    @Internal
    private Options options;

    private void handleOutOfDate(InputFileDetails inputFileDetails) {
        Compiler compiler = getCompilerInstance();
        Options options = getOptionsInstance();

        File file = inputFileDetails.getFile();

        if (isSassPartialFile(file) || !isSassFile(file))
            return;

        String sourceSetFilePath = createSourceSetFilePath(inputFileDetails);

        File realDestinationDir = new File(getDestinationDir(), cssPath);
        File cssOutput = new File(realDestinationDir, sourceSetFilePath + ".css");

        options.setIsIndentedSyntaxSrc(file.getName().endsWith(".sass"));

        File tempCssMapOutput = null;
        boolean sourceMapEnabled = isSourceMapEnabled();
        if (sourceMapEnabled) {
            tempCssMapOutput = createTempFile(sourceSetFilePath, "css.map");

            options.setSourceMapFile(tempCssMapOutput.toURI());
        } else {
            options.setSourceMapFile(null);
        }

        try {
            URI inputPath = file.getAbsoluteFile().toURI();

            File tempCssOutput = createTempFile(sourceSetFilePath, "css");

            Output output = compiler.compileFile(inputPath, tempCssOutput.toURI(), options);

            deleteAndWarnIfFailed(tempCssOutput);

            if (cssOutput.getParentFile().exists() || cssOutput.getParentFile().mkdirs()) {
                ResourceGroovyMethods.write(cssOutput, output.getCss());
            } else {
                getLogger().error("Cannot write into {}", cssOutput.getParentFile());
                throw new TaskExecutionException(this, null);
            }

            if (sourceMapEnabled) {
                deleteAndWarnIfFailed(tempCssMapOutput);

                File cssMapOutput = new File(realDestinationDir, sourceSetFilePath + ".css.map");

                if (cssMapOutput.getParentFile().exists() || cssMapOutput.getParentFile().mkdirs()) {
                    ResourceGroovyMethods.write(cssMapOutput, output.getSourceMap());
                } else {
                    getLogger().error("Cannot write into {}", cssMapOutput.getParentFile());
                    throw new TaskExecutionException(this, null);
                }
            }
        } catch (CompilationException e) {
            SassError sassError = new Gson().fromJson(e.getErrorJson(), SassError.class);

            getLogger().error("{}:{}:{}", sassError.getFile(), sassError.getLine(), sassError.getColumn());
            getLogger().error(e.getErrorMessage());

            throw new TaskExecutionException(this, e);
        } catch (IOException e) {
            getLogger().error(e.getLocalizedMessage());
            throw new TaskExecutionException(this, e);
        }
    }

    private void handleRemoved(InputFileDetails inputFileDetails) {
        String sourceSetFilePath = createSourceSetFilePath(inputFileDetails);

        File realDestinationDir = new File(getDestinationDir(), cssPath);
        File cssOutput = new File(realDestinationDir, sourceSetFilePath + ".css");
        File cssMapOutput = new File(realDestinationDir, sourceSetFilePath + ".css.map");

        deleteAndWarnIfFailed(cssOutput);
        deleteAndWarnIfFailed(cssMapOutput);
    }

    @TaskAction
    public void compileSass(IncrementalTaskInputs inputs) {
        if (!inputs.isIncremental()) {
            getProject().delete(getOutputFiles().getFiles());
        }

        inputs.outOfDate(this::handleOutOfDate);
        inputs.removed(this::handleRemoved);

        resetCompilerRelatedInstances();
    }

    private void deleteAndWarnIfFailed(File file) {
        if (file != null && !file.delete()) {
            getLogger().info("Unable to delete file {}", file);
        }
    }

    private String createSourceSetFilePath(InputFileDetails inputFileDetails) {
        String filePath = inputFileDetails.getFile().getAbsolutePath();

        return filePath.substring(0, filePath.length() - 5)
                .replace(getFinalSourceDirPath(), "");
    }

    @Internal
    private String getFinalSourceDirPath() {
        return new File(sourceDir, sassPath).getAbsolutePath();
    }

    private File createTempFile(String baseFilePath, String suffix) {
        try {
            return File.createTempFile(baseFilePath, suffix);
        } catch (IOException e) {
            throw new TaskExecutionException(this, e);
        }
    }

    private static boolean isSassPartialFile(File file) {
        return file.getName().startsWith("_");
    }

    private static boolean isSassFile(File file) {
        String name = file.getName();
        return name.endsWith(".scss") || name.endsWith(".sass");
    }

    @Internal
    private Compiler getCompilerInstance() {
        if (compiler == null) {
            compiler = new Compiler();
        }

        return compiler;
    }

    @Internal
    private Options getOptionsInstance() {
        if (options == null) {
            options = new Options();

            options.setFunctionProviders(new ArrayList<>(getFunctionProviders()));
            options.getFunctionProviders().add(new LoggingFunctionProvider());
            options.setHeaderImporters(getHeaderImporters());
            options.setImporters(getImporters());
            if (getIncludePaths() != null) {
                options.setIncludePaths(new ArrayList<>(getIncludePaths().getFiles()));
            }
            options.setIndent(getIndent());
            options.setLinefeed(getLinefeed());
            options.setOmitSourceMapUrl(isOmitSourceMapUrl());
            options.setOutputStyle(getOutputStyle());
            options.setPluginPath(getPluginPath());
            options.setPrecision(getPrecision());
            options.setSourceComments(isSourceComments());
            options.setSourceMapContents(isSourceMapContents());
            options.setSourceMapEmbed(isSourceMapEmbed());
            options.setSourceMapRoot(getSourceMapRoot());
        }

        return options;
    }

    private void resetCompilerRelatedInstances() {
        compiler = null;
        options = null;
    }

    /**
     * Custom import functions.
     */
    @Input
    @Optional
    private List<Object> functionProviders = new LinkedList<>();

    @Input
    @Optional
    private List<Importer> headerImporters = new LinkedList<>();

    /**
     * Custom import functions.
     */
    @Input
    @Optional
    private Collection<Importer> importers = new LinkedList<>();

    /**
     * SassList of paths.
     */
    @InputFiles
    @Optional
    private FileCollection includePaths;

    @Input
    private String indent;

    @Input
    private String linefeed;

    /**
     * Disable sourceMappingUrl in css output.
     */
    @Input
    private boolean omitSourceMapUrl;

    /**
     * Output style for the generated css code.
     */
    @Input
    private OutputStyle outputStyle;

    @Input
    @Optional
    private String pluginPath;

    /**
     * Precision for outputting fractional numbers.
     */
    @Input
    private int precision;

    /**
     * If you want inline source comments.
     */
    @Input
    private boolean sourceComments;

    /**
     * Embed include contents in maps.
     */
    @Input
    private boolean sourceMapContents;

    /**
     * Embed sourceMappingUrl as data uri.
     */
    @Input
    private boolean sourceMapEmbed;

    @Input
    private boolean sourceMapEnabled;

    @Input
    @Optional
    private URI sourceMapRoot;

    public class LoggingFunctionProvider {

        @WarnFunction
        @SuppressWarnings("unused")
        public void warn(String message) {
            getLogger().warn(message);
        }

        @ErrorFunction
        @SuppressWarnings("unused")
        public void error(String message) {
            getLogger().error(message);
        }

        @DebugFunction
        @SuppressWarnings("unused")
        public void debug(String message) {
            getLogger().info(message);
        }
    }
}
