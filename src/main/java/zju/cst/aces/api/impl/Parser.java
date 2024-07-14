package zju.cst.aces.api.impl;

import lombok.Data;
import zju.cst.aces.api.*;
import zju.cst.aces.api.config.Config;
import zju.cst.aces.parser.ProjectParser;

import zju.cst.aces.api.PreProcess;

import java.nio.file.Path;

/**
 * A preliminary analysis of the current project before starting ChatUniTest,
 * including whether the project is installed and whether the output path is created.
 */
@Data
public class Parser implements PreProcess {

    ProjectParser parser;

    Project project;
    Path parseOutput;
    Logger log;

    public Parser(ProjectParser parser, Project project, Path parseOutput, Logger log) {
        this.parser = parser;
        this.project = project;
        this.parseOutput = parseOutput;
        this.log = log;
    }

    @Override
    public void process() {
        this.parse();
    }

    /**
     * Check whether the current project is installed successfully.
     * If it is the first time to run ChatUniTest, call {@link Parser#parse} to perform detailed analysis of the project.
     */
    public void parse() {
        try {
            Task.checkTargetFolder(project);
        } catch (RuntimeException e) {
            getLog().error(e.toString());
            return;
        }
        if (project.getPackaging().equals("pom")) {
            log.info("\n==========================\n[ChatUniTest] Skip pom-packaging ...");
            return;
        }
        if (! parseOutput.toFile().exists()) {
            log.info("\n==========================\n[ChatUniTest] Parsing class info ...");
            parser.parse();
            log.info("\n==========================\n[ChatUniTest] Parse finished");
        } else {
            log.info("\n==========================\n[ChatUniTest] Parse output already exists, skip parsing!");
        }
    }
}
