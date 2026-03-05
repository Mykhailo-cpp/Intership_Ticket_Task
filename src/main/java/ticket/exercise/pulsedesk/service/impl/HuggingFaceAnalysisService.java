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

@Slf4j
@Service
@Primary
public class HuggingFaceAnalysisService implements AiAnalysisService {

    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;

    @Value("${huggingface.api.url}")
    private String apiUrl;

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
        String requestBody = objectMapper.writeValueAsString(
                new HuggingFaceRequest(prompt)
        );

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
        if (root.isArray() && root.size() > 0) {
            JsonNode first = root.get(0);
            if (first.has("generated_text")) {
                return first.get("generated_text").asText().trim();
            }
        }
        if (root.has("generated_text")) {
            return root.get("generated_text").asText().trim();
        }
        throw new AiAnalysisException("Unexpected response format from HuggingFace API: " + responseBody);
    }

    private String buildTriagePrompt(String comment) {
        return String.format(
                "Classify this user comment as either a support issue or general feedback.\n" +
                        "Comment: \"%s\"\n" +
                        "Answer with only YES if this is a bug, error, billing issue, account problem, or feature request that needs support. " +
                        "Answer with only NO if it is a compliment, general praise, or non-actionable feedback.\n" +
                        "Answer:",
                comment
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

    record HuggingFaceRequest(String inputs) {}
}
