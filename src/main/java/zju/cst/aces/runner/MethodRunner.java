package zju.cst.aces.runner;

import zju.cst.aces.api.Phase;
import zju.cst.aces.api.config.Config;
import zju.cst.aces.api.impl.PromptConstructorImpl;
import zju.cst.aces.dto.*;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;

/**
 * Inherited from {@link ClassRunner},
 * using multithreading, calling {@code startRounds} repeatedly within {@code TestNumber} times to
 * generate test code for methods in the focal class,
 * and perform testing, verification, and repair.
 */

public class MethodRunner extends ClassRunner {

    public MethodInfo methodInfo;

    public MethodRunner(Config config, String fullClassName, MethodInfo methodInfo) throws IOException {
        super(config, fullClassName);
        this.methodInfo = methodInfo;
    }

    @Override
    public void start() throws IOException {
        if (!config.isStopWhenSuccess() && config.isEnableMultithreading()) {
            ExecutorService executor = Executors.newFixedThreadPool(config.getTestNumber());
            List<Future<String>> futures = new ArrayList<>();
            for (int num = 0; num < config.getTestNumber(); num++) {
                int finalNum = num;
                Callable<String> callable = () -> {
                    startRounds(finalNum);
                    return "";
                };
                Future<String> future = executor.submit(callable);
                futures.add(future);
            }
            Runtime.getRuntime().addShutdownHook(new Thread(executor::shutdownNow));

            for (Future<String> future : futures) {
                try {
                    String result = future.get();
                    System.out.println(result);
                } catch (InterruptedException | ExecutionException e) {
                    e.printStackTrace();
                }
            }

            executor.shutdown();
        } else {
            for (int num = 0; num < config.getTestNumber(); num++) {
                boolean result = startRounds(num);
                if (result && config.isStopWhenSuccess()) {
                    break;
                }
            }
        }
    }

    /**
     * Call the {@code execute} method of {@link Phase.PromptGeneration} according to {@code config} to create prompt words,
     * use the generated prompt words as parameters to call the {@code execute} method of {@link Phase.TestGeneration}
     * to build unit test code, and verify and repair the unit test code.
     * @param num the number of current loops
     * @return {@code true} if the unit test code validation passes;
     *         {@code false} otherwise.
     */
    public boolean startRounds(final int num) {

        Phase phase = new Phase(config);

        // Prompt Construction Phase
        PromptConstructorImpl pc = phase.new PromptGeneration(classInfo, methodInfo).execute(num);
        PromptInfo promptInfo = pc.getPromptInfo();
        promptInfo.setRound(0);

        // Test Generation Phase
        phase.new TestGeneration().execute(pc);

        // Validation
        if (phase.new Validation().execute(pc)) {
            exportRecord(pc.getPromptInfo(), classInfo, num);
            return true;
        }

        // Validation and Repair Phase
        for (int rounds = 1; rounds < config.getMaxRounds(); rounds++) {

            promptInfo.setRound(rounds);

            // Repair
            phase.new Repair().execute(pc);

            // Validation
            if (phase.new Validation().execute(pc)) {
                exportRecord(pc.getPromptInfo(), classInfo, num);
                return true;
            }

        }

        exportRecord(pc.getPromptInfo(), classInfo, num);
        return false;
    }
}