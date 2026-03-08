package ticket.exercise.pulsedesk;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import ticket.exercise.pulsedesk.mapper.TicketMapper;
import ticket.exercise.pulsedesk.model.dto.response.TicketAnalysisResult;
import ticket.exercise.pulsedesk.model.dto.response.TicketResponse;
import ticket.exercise.pulsedesk.model.entity.Comment;
import ticket.exercise.pulsedesk.model.entity.Ticket;
import ticket.exercise.pulsedesk.model.enums.Category;
import ticket.exercise.pulsedesk.model.enums.Priority;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("TicketMapper Tests")
class TicketMapperTest {

    private TicketMapper ticketMapper;

    @BeforeEach
    void setUp() {
        ticketMapper = new TicketMapper();
    }

    @Test
    @DisplayName("Should map TicketAnalysisResult and Comment to Ticket entity")
    void toEntity_shouldMapAllFields() {
        Comment comment = Comment.builder()
                .id(1L)
                .content("The billing page shows wrong amount.")
                .build();

        TicketAnalysisResult result = TicketAnalysisResult.builder()
                .shouldCreateTicket(true)
                .title("Incorrect billing amount displayed")
                .category(Category.BILLING)
                .priority(Priority.HIGH)
                .summary("Billing page shows incorrect charge amount.")
                .build();

        Ticket ticket = ticketMapper.toEntity(result, comment);

        assertThat(ticket).isNotNull();
        assertThat(ticket.getTitle()).isEqualTo("Incorrect billing amount displayed");
        assertThat(ticket.getCategory()).isEqualTo(Category.BILLING);
        assertThat(ticket.getPriority()).isEqualTo(Priority.HIGH);
        assertThat(ticket.getSummary()).isEqualTo("Billing page shows incorrect charge amount.");
        assertThat(ticket.getComment()).isEqualTo(comment);
        assertThat(ticket.getComment().getId()).isEqualTo(1L);
    }

    @Test
    @DisplayName("Should map Ticket entity to TicketResponse with original comment")
    void toResponse_shouldMapAllFieldsIncludingComment() {
        Comment comment = Comment.builder()
                .id(5L)
                .content("I was charged twice this month!")
                .build();

        Ticket ticket = Ticket.builder()
                .id(10L)
                .title("Duplicate charge reported")
                .category(Category.BILLING)
                .priority(Priority.HIGH)
                .summary("User reports being charged twice.")
                .createdAt(LocalDateTime.now())
                .comment(comment)
                .build();

        TicketResponse response = ticketMapper.toResponse(ticket);

        assertThat(response).isNotNull();
        assertThat(response.getId()).isEqualTo(10L);
        assertThat(response.getTitle()).isEqualTo("Duplicate charge reported");
        assertThat(response.getCategory()).isEqualTo(Category.BILLING);
        assertThat(response.getPriority()).isEqualTo(Priority.HIGH);
        assertThat(response.getSummary()).isEqualTo("User reports being charged twice.");
        assertThat(response.getCommentId()).isEqualTo(5L);
        assertThat(response.getOriginalComment()).isEqualTo("I was charged twice this month!");
    }

    @Test
    @DisplayName("Should correctly map FEATURE category ticket")
    void toEntity_withFeatureCategory_shouldMapCorrectly() {
        Comment comment = Comment.builder().id(2L).content("Please add CSV export.").build();

        TicketAnalysisResult result = TicketAnalysisResult.builder()
                .shouldCreateTicket(true)
                .title("CSV export feature request")
                .category(Category.FEATURE)
                .priority(Priority.MEDIUM)
                .summary("User requests CSV export functionality.")
                .build();

        Ticket ticket = ticketMapper.toEntity(result, comment);

        assertThat(ticket.getCategory()).isEqualTo(Category.FEATURE);
        assertThat(ticket.getPriority()).isEqualTo(Priority.MEDIUM);
    }

    @Test
    @DisplayName("Should correctly map all priority levels")
    void toEntity_shouldMapAllPriorityLevels() {
        Comment comment = Comment.builder().id(1L).content("Minor UI issue.").build();

        TicketAnalysisResult lowResult = TicketAnalysisResult.builder()
                .shouldCreateTicket(true)
                .title("Minor UI glitch")
                .category(Category.BUG)
                .priority(Priority.LOW)
                .summary("Small cosmetic issue.")
                .build();

        Ticket lowTicket = ticketMapper.toEntity(lowResult, comment);
        assertThat(lowTicket.getPriority()).isEqualTo(Priority.LOW);

        TicketAnalysisResult mediumResult = TicketAnalysisResult.builder()
                .shouldCreateTicket(true)
                .title("Feature request")
                .category(Category.FEATURE)
                .priority(Priority.MEDIUM)
                .summary("Feature enhancement.")
                .build();

        Ticket mediumTicket = ticketMapper.toEntity(mediumResult, comment);
        assertThat(mediumTicket.getPriority()).isEqualTo(Priority.MEDIUM);

        TicketAnalysisResult highResult = TicketAnalysisResult.builder()
                .shouldCreateTicket(true)
                .title("Critical bug")
                .category(Category.BUG)
                .priority(Priority.HIGH)
                .summary("Critical system failure.")
                .build();

        Ticket highTicket = ticketMapper.toEntity(highResult, comment);
        assertThat(highTicket.getPriority()).isEqualTo(Priority.HIGH);
    }
}