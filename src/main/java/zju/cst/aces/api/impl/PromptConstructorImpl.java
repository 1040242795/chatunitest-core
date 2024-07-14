package zju.cst.aces.api.impl;

import lombok.Data;
import zju.cst.aces.api.PromptConstructor;
import zju.cst.aces.api.config.Config;
import zju.cst.aces.dto.ChatMessage;
import zju.cst.aces.dto.ClassInfo;
import zju.cst.aces.dto.MethodInfo;
import zju.cst.aces.dto.PromptInfo;
import zju.cst.aces.prompt.PromptGenerator;
import zju.cst.aces.runner.AbstractRunner;
import zju.cst.aces.util.TokenCounter;

import java.io.IOException;
import java.util.List;

/**
 * Prompt word generation starter.
 * Initializes the relevant information of the prompt word
 * and calls {@link PromptGenerator#generateMessages} to generate the prompt word.
 */

@Data
public class PromptConstructorImpl implements PromptConstructor {

    Config config;
    PromptInfo promptInfo;
    List<ChatMessage> chatMessages;
    int tokenCount = 0;
    String testName;
    String fullTestName;
    static final String separator = "_";

    public PromptConstructorImpl(Config config) {
        this.config = config;
    }

    @Override
    public List<ChatMessage> generate() {
        try {
            if (promptInfo == null) {
                throw new RuntimeException("PromptInfo is null, you need to initialize it first.");
            }
            this.chatMessages = new PromptGenerator(config).generateMessages(promptInfo);
            countToken();
            return this.chatMessages;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Call{@link  AbstractRunner#generatePromptInfoWithDep} to set prompt information, including dependencies.
     * @param classInfo focal class information
     * @param methodInfo focal method information
     * @throws IOException Exception handling
     */
    public void setPromptInfoWithDep(ClassInfo classInfo, MethodInfo methodInfo) throws IOException {
        this.promptInfo = AbstractRunner.generatePromptInfoWithDep(config, classInfo, methodInfo);
    }

    /**
     * Call{@link  AbstractRunner#generatePromptInfoWithDep} to set prompt information, excluding dependencies
     * @param classInfo focal class information
     * @param methodInfo focal method information
     * @throws IOException Exception handling
     */
    public void setPromptInfoWithoutDep(ClassInfo classInfo, MethodInfo methodInfo) throws IOException {
        this.promptInfo = AbstractRunner.generatePromptInfoWithoutDep(config, classInfo, methodInfo);
    }

    /**
     * Set the full test name
     * @param fullTestName full test name
     */
    public void setFullTestName(String fullTestName) {
        this.fullTestName = fullTestName;
        this.testName = fullTestName.substring(fullTestName.lastIndexOf(".") + 1);
        this.promptInfo.setFullTestName(this.fullTestName);
    }

    public void setTestName(String testName) {
        this.testName = testName;
    }

    /**
     * Set the length of the current prompt word.
     */
    public void countToken() {
        for (ChatMessage p : chatMessages) {
            this.tokenCount += TokenCounter.countToken(p.getContent());
        }
    }

    /**
     * Determine whether the length of the current prompt word exceeds maxPromptTokens.
     * @return {@code true} if maxPromptTokens is exceeded
     *         {@code false} otherwise
     */
    public boolean isExceedMaxTokens() {
        if (this.tokenCount > config.maxPromptTokens) {
            return true;
        } else {
            return false;
        }
    }
}
