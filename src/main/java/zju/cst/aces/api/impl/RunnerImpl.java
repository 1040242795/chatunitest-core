package zju.cst.aces.api.impl;

import zju.cst.aces.api.Runner;
import zju.cst.aces.api.config.Config;
import zju.cst.aces.dto.ClassInfo;
import zju.cst.aces.dto.MethodInfo;
import zju.cst.aces.dto.PromptInfo;
import zju.cst.aces.dto.RoundRecord;
import zju.cst.aces.runner.AbstractRunner;
import zju.cst.aces.runner.ClassRunner;
import zju.cst.aces.runner.MethodRunner;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;

/**
 * Provides an implements of the {@link Runner} interface,
 * as a starter for a class or method test generator.
 *
 * <P>
 * Receive configuration parameters, class name, method name, and method information,
 * and call {@link ClassRunner} and {@link MethodRunner}
 * to generate unit test code for the class or method.
 * </P>
 */

public class RunnerImpl implements Runner {
    Config config;

    public RunnerImpl(Config config) {
        this.config = config;
    }

    /**
     * Call {@link ClassRunner} with {@code fullClassName} and {@code config}
     * as parameters to start unit test code generation for the target class.
     * @param fullClassName The class name for the test code to be generated.
     */
    public void runClass(String fullClassName) {
        try {
            new ClassRunner(config, fullClassName).start();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
    /**
     * Call {@link MethodRunner} with {@code fullClassName}, {@code config} and {@code methodInfo}
     * as parameters to start unit test code generation for the target method.
     * @param fullClassName The class name for the test code to be generated
     * @param methodInfo Information about the method to generate test code
     */
    public void runMethod(String fullClassName, MethodInfo methodInfo) {
        try {
            new MethodRunner(config, fullClassName, methodInfo).start();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }


}
