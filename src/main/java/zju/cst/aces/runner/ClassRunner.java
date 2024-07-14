package zju.cst.aces.runner;

import zju.cst.aces.dto.ClassInfo;
import zju.cst.aces.dto.MethodInfo;
import zju.cst.aces.api.config.Config;
import zju.cst.aces.util.Counter;
import zju.cst.aces.util.TestClassMerger;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;

/**
 * Inherit the {@link AbstractRunner} class,
 * parse the parameters, configure the class information for which the unit test code is to be generated,
 * and call {@link MethodRunner} to generate unit test code for the methods in the focal class.
 */

public class ClassRunner extends AbstractRunner {
    public ClassInfo classInfo;
    public File infoDir;
    public int index;

    /**
     * The constructor of the {@link ClassRunner} class configures {@code classInfo} according to {@code config}
     *
     * @param config global configuration information
     * @param fullClassName the name of the focal class
     * @throws IOException if an input or output exception occurred
     */
    public ClassRunner(Config config, String fullClassName) throws IOException {
        super(config, fullClassName);
        infoDir = config.getParseOutput().resolve(fullClassName.replace(".", File.separator)).toFile();
        if (!infoDir.isDirectory()) {
            config.getLogger().warn("Error: " + fullClassName + " no parsed info found");
        }
        File classInfoFile = new File(infoDir + File.separator + "class.json");
        classInfo = GSON.fromJson(Files.readString(classInfoFile.toPath(), StandardCharsets.UTF_8), ClassInfo.class);
    }

    /**
     * if {@link Config#isEnableMultithreading()}
     * Multithreading calls {@link ClassRunner#methodJob()}, which uses multithreading to call the start method of {@link MethodRunner}
     * to start unit test code generation for the methods in the focal class.
     * else
     * Generate unit test code for methods in the focal class by looping
     *
     * @throws IOException If an input or output exception occurred
     */
    @Override
    public void start() throws IOException {
        if (config.isEnableMultithreading() == true) {
            methodJob();
        } else {
            for (String mSig : classInfo.methodSigs.keySet()) {
                MethodInfo methodInfo = getMethodInfo(config, classInfo, mSig);
                if (!Counter.filter(methodInfo)) {
                    config.getLogger().info("Skip method: " + mSig + " in class: " + fullClassName);
                    continue;
                }
                new MethodRunner(config, fullClassName, methodInfo).start();
                int newCount = config.getCompletedJobCount().incrementAndGet();
                config.getLogger().info(String.format("\n==========================\n[%s] Completed Method Jobs:   [ %s /  %s]", config.pluginSign, newCount, config.getJobCount()));
            }
        }
        if (config.isEnableMerge()) {
            new TestClassMerger(config, fullClassName).mergeWithSuite();
        }
    }

    public void methodJob() {
        ExecutorService executor = Executors.newFixedThreadPool(config.getMethodThreads());
        List<Future<String>> futures = new ArrayList<>();
        for (String mSig : classInfo.methodSigs.keySet()) {
            Callable<String> callable = new Callable<String>() {
                @Override
                public String call() throws Exception {
                    MethodInfo methodInfo = getMethodInfo(config, classInfo, mSig);
                    if (methodInfo == null) {
                        return "No parsed info found for " + mSig + " in " + fullClassName;
                    }
                    if (!Counter.filter(methodInfo)) {
                        return "Skip method: " + mSig + " in class: " + fullClassName;
                    }
                    new MethodRunner(config, fullClassName, methodInfo).start();
                    int newCount = config.getCompletedJobCount().incrementAndGet();
                    config.getLogger().info(String.format("\n==========================\n[%s] Completed Method Jobs:   [ %s /  %s]", config.pluginSign, newCount, config.getJobCount()));
                    return "Processed " + mSig;
                }
            };
            Future<String> future = executor.submit(callable);
            futures.add(future);
        }

        Runtime.getRuntime().addShutdownHook(new Thread() {
            public void run() {
                executor.shutdownNow();
            }
        });

        for (Future<String> future : futures) {
            try {
                String result = future.get();
                System.out.println(result);
            } catch (InterruptedException | ExecutionException e) {
                e.printStackTrace();
            }
        }

        executor.shutdown();
    }
}
