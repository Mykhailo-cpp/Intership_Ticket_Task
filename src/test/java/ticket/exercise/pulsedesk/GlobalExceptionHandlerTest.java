package ticket.exercise.pulsedesk;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;

import ticket.exercise.pulsedesk.exception.AiAnalysisException;
import ticket.exercise.pulsedesk.exception.GlobalExceptionHandler;
import ticket.exercise.pulsedesk.exception.ResourceNotFoundException;
import ticket.exercise.pulsedesk.model.dto.response.ApiResponse;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@DisplayName("GlobalExceptionHandler Tests")
class GlobalExceptionHandlerTest {

    private GlobalExceptionHandler exceptionHandler;

    @BeforeEach
    void setUp() {
        exceptionHandler = new GlobalExceptionHandler();
    }

    @Test
    @DisplayName("Should return 404 when ResourceNotFoundException is thrown")
    void handleResourceNotFoundException_shouldReturn404() {
        ResourceNotFoundException ex = new ResourceNotFoundException("Comment", 99L);

        ResponseEntity<ApiResponse<Void>> response =
                exceptionHandler.handleResourceNotFoundException(ex);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().isSuccess()).isFalse();
        assertThat(response.getBody().getMessage()).contains("Comment");
        assertThat(response.getBody().getMessage()).contains("99");
    }

    @Test
    @DisplayName("Should return 404 for Ticket not found")
    void handleResourceNotFoundException_forTicket_shouldReturn404() {
        ResourceNotFoundException ex = new ResourceNotFoundException("Ticket", 42L);

        ResponseEntity<ApiResponse<Void>> response =
                exceptionHandler.handleResourceNotFoundException(ex);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody().getMessage()).contains("Ticket");
        assertThat(response.getBody().getMessage()).contains("42");
    }

    @Test
    @DisplayName("Should return 503 when AiAnalysisException is thrown")
    void handleAiAnalysisException_shouldReturn503() {
        AiAnalysisException ex = new AiAnalysisException("HuggingFace API error, status: 400");

        ResponseEntity<ApiResponse<Void>> response =
                exceptionHandler.handleAiAnalysisException(ex);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().isSuccess()).isFalse();
        assertThat(response.getBody().getMessage()).contains("AI analysis service unavailable");
    }

    @Test
    @DisplayName("Should return 400 with field errors when validation fails")
    void handleValidationException_shouldReturn400WithFieldErrors() {
        MethodArgumentNotValidException ex = mock(MethodArgumentNotValidException.class);
        BindingResult bindingResult = mock(BindingResult.class);

        FieldError fieldError = new FieldError(
                "commentRequest", "content", "Comment content must not be blank"
        );

        when(ex.getBindingResult()).thenReturn(bindingResult);
        when(bindingResult.getAllErrors()).thenReturn(List.of(fieldError));

        ResponseEntity<ApiResponse<Map<String, String>>> response =
                exceptionHandler.handleValidationException(ex);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().isSuccess()).isFalse();
        assertThat(response.getBody().getMessage()).isEqualTo("Validation failed");
        assertThat(response.getBody().getData()).containsKey("content");
        assertThat(response.getBody().getData().get("content"))
                .isEqualTo("Comment content must not be blank");
    }

    @Test
    @DisplayName("Should return 500 for unexpected exceptions")
    void handleGenericException_shouldReturn500() {
        Exception ex = new RuntimeException("Unexpected database error");

        ResponseEntity<ApiResponse<Void>> response =
                exceptionHandler.handleGenericException(ex);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().isSuccess()).isFalse();
        assertThat(response.getBody().getMessage()).contains("unexpected error");
    }

    @Test
    @DisplayName("Should include timestamp in all error responses")
    void allHandlers_shouldIncludeTimestamp() {
        ResourceNotFoundException ex = new ResourceNotFoundException("Comment", 1L);

        ResponseEntity<ApiResponse<Void>> response =
                exceptionHandler.handleResourceNotFoundException(ex);

        assertThat(response.getBody().getTimestamp()).isNotNull();
    }
}