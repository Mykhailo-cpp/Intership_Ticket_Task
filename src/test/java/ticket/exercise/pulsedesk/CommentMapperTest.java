package ticket.exercise.pulsedesk;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import ticket.exercise.pulsedesk.mapper.CommentMapper;
import ticket.exercise.pulsedesk.model.dto.request.CommentRequest;
import ticket.exercise.pulsedesk.model.dto.response.CommentResponse;
import ticket.exercise.pulsedesk.model.entity.Comment;
import ticket.exercise.pulsedesk.model.entity.Ticket;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("CommentMapper Tests")
class CommentMapperTest {

    private CommentMapper commentMapper;

    @BeforeEach
    void setUp() {
        commentMapper = new CommentMapper();
    }

    @Test
    @DisplayName("Should map CommentRequest to Comment entity correctly")
    void toEntity_shouldMapAllFields() {
        CommentRequest request = new CommentRequest(
                "The app keeps crashing on startup.",
                "app-review"
        );

        Comment result = commentMapper.toEntity(request);

        assertThat(result).isNotNull();
        assertThat(result.getContent()).isEqualTo("The app keeps crashing on startup.");
        assertThat(result.getSource()).isEqualTo("app-review");
    }

    @Test
    @DisplayName("Should map Comment entity to CommentResponse without ticket")
    void toResponse_whenNoTicket_shouldMapCorrectly() {
        Comment comment = Comment.builder()
                .id(1L)
                .content("I cannot reset my password.")
                .source("web-form")
                .createdAt(LocalDateTime.now())
                .analyzed(true)
                .convertedToTicket(false)
                .ticket(null)
                .build();

        CommentResponse result = commentMapper.toResponse(comment);

        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(1L);
        assertThat(result.getContent()).isEqualTo("I cannot reset my password.");
        assertThat(result.getSource()).isEqualTo("web-form");
        assertThat(result.isAnalyzed()).isTrue();
        assertThat(result.isConvertedToTicket()).isFalse();
        assertThat(result.getTicketId()).isNull();
    }

    @Test
    @DisplayName("Should include ticketId in response when ticket exists")
    void toResponse_whenTicketExists_shouldIncludeTicketId() {
        Ticket ticket = Ticket.builder().id(42L).build();

        Comment comment = Comment.builder()
                .id(1L)
                .content("Login page is broken.")
                .source("web-form")
                .createdAt(LocalDateTime.now())
                .analyzed(true)
                .convertedToTicket(true)
                .ticket(ticket)
                .build();

        CommentResponse result = commentMapper.toResponse(comment);

        assertThat(result.isConvertedToTicket()).isTrue();
        assertThat(result.getTicketId()).isEqualTo(42L);
    }

    @Test
    @DisplayName("Should not set ID or dates when mapping request to entity")
    void toEntity_shouldNotSetIdOrDates() {
        CommentRequest request = new CommentRequest("Some content", "web-form");

        Comment result = commentMapper.toEntity(request);

        // ID and dates are set by JPA @PrePersist, not the mapper
        assertThat(result.getId()).isNull();
        assertThat(result.getCreatedAt()).isNull();
    }
}