package ticket.exercise.pulsedesk.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import ticket.exercise.pulsedesk.exception.AiAnalysisException;
import ticket.exercise.pulsedesk.model.dto.response.TicketAnalysisResult;
import ticket.exercise.pulsedesk.model.enums.Category;
import ticket.exercise.pulsedesk.model.enums.Priority;
import ticket.exercise.pulsedesk.service.AiAnalysisService;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@Primary
public class HuggingFaceAnalysisService implements AiAnalysisService {

    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;

    @Value("${huggingface.api.url}")
    private String apiUrl;

    @Value("${huggingface.api.model}")
    private String apiModel;

    @Value("${huggingface.api.token}")
    private String apiToken;

    @Value("${huggingface.api.timeout}")
    private int timeoutMs;

    public HuggingFaceAnalysisService(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofMillis(10000))
                .build();
    }

    @Override
    public TicketAnalysisResult analyze(String commentContent) {
        log.info("Analyzing comment with HuggingFace API, length={}", commentContent.length());

        try {
            String triagePrompt = buildTriagePrompt(commentContent);
            String triageResponse = callHuggingFaceApi(triagePrompt);
            boolean shouldCreateTicket = parseShouldCreateTicket(triageResponse);

            if (!shouldCreateTicket) {
                log.info("Comment classified as non-actionable (compliment/general feedback)");
                return TicketAnalysisResult.builder()
                        .shouldCreateTicket(false)
                        .build();
            }

            String detailsPrompt = buildDetailsPrompt(commentContent);
            String detailsResponse = callHuggingFaceApi(detailsPrompt);

            return parseTicketDetails(detailsResponse, commentContent);

        } catch (AiAnalysisException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new AiAnalysisException("Failed to analyze comment via HuggingFace API", ex);
        }
    }

    private String callHuggingFaceApi(String prompt) throws Exception {
        String requestBody = objectMapper.writeValueAsString(Map.of(
                "model", apiModel,
                "messages", List.of(Map.of(
                        "role", "user",
                        "content", prompt
                )),
                "max_tokens", 300
        ));

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(apiUrl))
                .timeout(Duration.ofMillis(timeoutMs))
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + apiToken)
                .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() != 200) {
            log.error("HuggingFace API returned status {}: {}", response.statusCode(), response.body());
            throw new AiAnalysisException("HuggingFace API error, status: " + response.statusCode());
        }

        return extractTextFromResponse(response.body());
    }

    private String extractTextFromResponse(String responseBody) throws Exception {
        JsonNode root = objectMapper.readTree(responseBody);
        if (root.has("choices")) {
            JsonNode content = root
                    .path("choices")
                    .get(0)
                    .path("message")
                    .path("content");
            if (!content.isMissingNode()) {
                return content.asText().trim();
            }
        }
        throw new AiAnalysisException("Unexpected response format from HuggingFace API: " + responseBody);
    }

    private String buildTriagePrompt(String comment) {
        return String.format(
                "You are a support ticket classifier. Analyze this user comment carefully.\n" +
                        "Comment: \"%s\"\n\n" +
                        "Answer YES if the comment is ANY of these:\n" +
                        "- A bug report or error\n" +
                        "- A billing or payment issue\n" +
                        "- An account access problem\n" +
                        "- A feature request or suggestion for improvement\n" +
                        "- Any request that requires action from a support team\n\n" +
                        "Answer NO only if it is purely a compliment, general praise, or contains absolutely no actionable request.\n\n" +
                        "Examples:\n" +
                        "Comment: 'I love the app!' → NO\n" +
                        "Comment: 'It would be great to export to CSV' → YES\n" +
                        "Comment: 'The login button is broken' → YES\n" +
                        "Comment: 'Can you add dark mode?' → YES\n" +
                        "Comment: 'Great job on the new update!' → NO\n\n" +
                        "Comment: \"%s\"\n" +
                        "Answer (YES or NO):",
                comment, comment
        );
    }

    private String buildDetailsPrompt(String comment) {
        return String.format(
                "You are a support ticket assistant. Given this user comment, extract structured support ticket data.\n" +
                        "Comment: \"%s\"\n" +
                        "Respond in this exact format:\n" +
                        "TITLE: <short title under 10 words>\n" +
                        "CATEGORY: <one of: BUG, FEATURE, BILLING, ACCOUNT, OTHER>\n" +
                        "PRIORITY: <one of: LOW, MEDIUM, HIGH>\n" +
                        "SUMMARY: <one sentence summary of the issue>\n",
                comment
        );
    }

    private boolean parseShouldCreateTicket(String response) {
        String normalized = response.toUpperCase().trim();
        return normalized.startsWith("YES") || normalized.contains("YES");
    }

    private TicketAnalysisResult parseTicketDetails(String response, String originalComment) {
        String title = extractField(response, "TITLE");
        String categoryStr = extractField(response, "CATEGORY");
        String priorityStr = extractField(response, "PRIORITY");
        String summary = extractField(response, "SUMMARY");

        Category category = parseCategory(categoryStr);
        Priority priority = parsePriority(priorityStr);

        if (title.isEmpty()) {
            title = generateFallbackTitle(originalComment);
        }
        if (summary.isEmpty()) {
            summary = originalComment.length() > 200
                    ? originalComment.substring(0, 197) + "..."
                    : originalComment;
        }

        return TicketAnalysisResult.builder()
                .shouldCreateTicket(true)
                .title(title)
                .category(category)
                .priority(priority)
                .summary(summary)
                .build();
    }

    private String extractField(String response, String fieldName) {
        String[] lines = response.split("\n");
        for (String line : lines) {
            if (line.toUpperCase().startsWith(fieldName + ":")) {
                return line.substring(fieldName.length() + 1).trim();
            }
        }
        return "";
    }

    private Category parseCategory(String value) {
        try {
            return Category.valueOf(value.toUpperCase().trim());
        } catch (IllegalArgumentException e) {
            log.warn("Could not parse category '{}', defaulting to OTHER", value);
            return Category.OTHER;
        }
    }

    private Priority parsePriority(String value) {
        try {
            return Priority.valueOf(value.toUpperCase().trim());
        } catch (IllegalArgumentException e) {
            log.warn("Could not parse priority '{}', defaulting to MEDIUM", value);
            return Priority.MEDIUM;
        }
    }

    private String generateFallbackTitle(String comment) {
        String trimmed = comment.trim();
        return trimmed.length() > 60 ? trimmed.substring(0, 57) + "..." : trimmed;
    }
}
