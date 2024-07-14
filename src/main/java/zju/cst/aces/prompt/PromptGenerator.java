package zju.cst.aces.prompt;

import zju.cst.aces.api.config.Config;
import zju.cst.aces.dto.*;
import zju.cst.aces.util.TokenCounter;

import java.io.IOException;
import java.util.*;


/**
 * Prompt word generator，
 * providing system prompt word and user prompt word generation.
 */

public class PromptGenerator {
    public Config config;
    public PromptTemplate promptTemplate;

    public PromptGenerator(Config config) {
        this.config = config;
        this.promptTemplate = new PromptTemplate(config, config.getProperties(), config.getPromptPath(), config.getMaxPromptTokens());
    }

    public void setConfig(Config config) {
        this.config = config;
        this.promptTemplate = new PromptTemplate(config, config.getProperties(), config.getPromptPath(), config.getMaxPromptTokens());
    }

    /**
     * Generate messages by promptInfo with no errors (generation - round 0) or errors (repair - round > 0)
     * @param promptInfo prompt word information
     * @return prompt word
     */
    public List<ChatMessage> generateMessages(PromptInfo promptInfo) {
        List<ChatMessage> chatMessages = new ArrayList<>();
        if (promptInfo.errorMsg == null) { // round 0
            chatMessages.add(ChatMessage.ofSystem(createSystemPrompt(promptInfo, promptTemplate.TEMPLATE_INIT)));
            chatMessages.add(ChatMessage.of(createUserPrompt(promptInfo, promptTemplate.TEMPLATE_INIT)));
        } else {
            chatMessages.add(ChatMessage.of(createUserPrompt(promptInfo, promptTemplate.TEMPLATE_REPAIR)));
        }
        return chatMessages;
    }

    public List<ChatMessage> generateMessages(PromptInfo promptInfo, String templateName) {
        List<ChatMessage> chatMessages = new ArrayList<>();
        chatMessages.add(ChatMessage.ofSystem(createSystemPrompt(promptInfo, templateName)));
        chatMessages.add(ChatMessage.of(createUserPrompt(promptInfo, templateName)));
        return chatMessages;
    }

    /**
     * Generate user prompts, call {@link PromptTemplate#buildDataModel} to generate {@code datamodel},
     * and add error information and defective unit test code generated in the previous round to the prompts when repairs are needed.
     * Call {@link PromptTemplate#renderTemplate} to render the datamodel to the prompt template
     * to generate the final prompt.
     * @param promptInfo prompt word information
     * @param templateName prompt word template name
     * @return prompt word text.
     */
    public String createUserPrompt(PromptInfo promptInfo, String templateName) {
        try {
            this.promptTemplate.buildDataModel(config, promptInfo);
            if (templateName.equals(promptTemplate.TEMPLATE_REPAIR)) { // repair process

                int promptTokens = TokenCounter.countToken(promptInfo.getUnitTest())
                        + TokenCounter.countToken(promptInfo.getMethodSignature())
                        + TokenCounter.countToken(promptInfo.getClassName())
                        + TokenCounter.countToken(promptInfo.getContext())
                        + TokenCounter.countToken(promptInfo.getOtherMethodBrief());
                int allowedTokens = Math.max(config.getMaxPromptTokens() - promptTokens, config.getMinErrorTokens());
                TestMessage errorMsg = promptInfo.getErrorMsg();
                String processedErrorMsg = "";
                for (String error : errorMsg.getErrorMessage()) {
                    if (TokenCounter.countToken(processedErrorMsg + error + "\n") <= allowedTokens) {
                        processedErrorMsg += error + "\n";
                    }
                }
                config.getLogger().debug("Allowed tokens: " + allowedTokens);
                config.getLogger().debug("Processed error message: \n" + processedErrorMsg);

                promptTemplate.dataModel.put("unit_test", promptInfo.getUnitTest());
                promptTemplate.dataModel.put("error_message", processedErrorMsg);

                return promptTemplate.renderTemplate(promptTemplate.TEMPLATE_REPAIR);
            } else {
                return promptTemplate.renderTemplate(templateName);
            }
        } catch (Exception e) {
            throw new RuntimeException("An error occurred while generating the user prompt: " + e);
        }
    }

    public String createSystemPrompt(PromptInfo promptInfo, String templateName) {
        try {
            String filename;
            filename = addSystemFileName(templateName);
            return promptTemplate.renderTemplate(filename);
        } catch (Exception e) {
            if (e instanceof IOException) {
                return "";
            }
            throw new RuntimeException("An error occurred while generating the system prompt: " + e);
        }
    }

    public String addSystemFileName(String filename) {
        String[] parts = filename.split("\\.");
        if (parts.length > 1) {
            return parts[0] + "_system." + parts[1];
        }
        return filename;
    }

    public String buildCOT(COT<?> cot) {
        return "";
    }

    public String buildTOT(TOT<?> tot) {
        return "";
    }

}
