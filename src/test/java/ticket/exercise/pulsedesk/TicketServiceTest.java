package ticket.exercise.pulsedesk;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import ticket.exercise.pulsedesk.exception.ResourceNotFoundException;
import ticket.exercise.pulsedesk.mapper.TicketMapper;
import ticket.exercise.pulsedesk.model.dto.response.TicketResponse;
import ticket.exercise.pulsedesk.model.entity.Comment;
import ticket.exercise.pulsedesk.model.entity.Ticket;
import ticket.exercise.pulsedesk.model.enums.Category;
import ticket.exercise.pulsedesk.model.enums.Priority;
import ticket.exercise.pulsedesk.repository.TicketRepository;
import ticket.exercise.pulsedesk.service.impl.TicketServiceImpl;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("TicketService Tests")
class TicketServiceTest {

    @Mock
    private TicketRepository ticketRepository;

    @Mock
    private TicketMapper ticketMapper;

    @InjectMocks
    private TicketServiceImpl ticketService;

    private Ticket mockBugTicket;
    private Ticket mockFeatureTicket;
    private Ticket mockBillingTicket;

    @BeforeEach
    void setUp() {
        Comment comment1 = Comment.builder().id(1L).content("Can't log in").build();
        Comment comment2 = Comment.builder().id(2L).content("Need CSV export").build();
        Comment comment3 = Comment.builder().id(3L).content("Charged twice").build();

        mockBugTicket = Ticket.builder()
                .id(1L)
                .title("Login issue reported")
                .category(Category.BUG)
                .priority(Priority.HIGH)
                .summary("User cannot access account.")
                .createdAt(LocalDateTime.now())
                .comment(comment1)
                .build();

        mockFeatureTicket = Ticket.builder()
                .id(2L)
                .title("CSV export requested")
                .category(Category.FEATURE)
                .priority(Priority.MEDIUM)
                .summary("User wants to export data to CSV.")
                .createdAt(LocalDateTime.now())
                .comment(comment2)
                .build();

        mockBillingTicket = Ticket.builder()
                .id(3L)
                .title("Double charge on account")
                .category(Category.BILLING)
                .priority(Priority.HIGH)
                .summary("User was charged twice for subscription.")
                .createdAt(LocalDateTime.now())
                .comment(comment3)
                .build();
    }

    // -------------------------------------------------------
    // getAllTickets tests
    // -------------------------------------------------------

    @Test
    @DisplayName("Should return all tickets ordered by date")
    void getAllTickets_shouldReturnAllTickets() {
        when(ticketRepository.findAllByOrderByCreatedAtDesc())
                .thenReturn(List.of(mockBugTicket, mockFeatureTicket, mockBillingTicket));
        when(ticketMapper.toResponse(mockBugTicket))
                .thenReturn(TicketResponse.builder().id(1L).category(Category.BUG).build());
        when(ticketMapper.toResponse(mockFeatureTicket))
                .thenReturn(TicketResponse.builder().id(2L).category(Category.FEATURE).build());
        when(ticketMapper.toResponse(mockBillingTicket))
                .thenReturn(TicketResponse.builder().id(3L).category(Category.BILLING).build());

        List<TicketResponse> results = ticketService.getAllTickets();

        assertThat(results).hasSize(3);
        assertThat(results.get(0).getId()).isEqualTo(1L);
        assertThat(results.get(1).getId()).isEqualTo(2L);
        assertThat(results.get(2).getId()).isEqualTo(3L);
        verify(ticketRepository, times(1)).findAllByOrderByCreatedAtDesc();
    }

    @Test
    @DisplayName("Should return empty list when no tickets exist")
    void getAllTickets_whenNoTickets_shouldReturnEmptyList() {
        when(ticketRepository.findAllByOrderByCreatedAtDesc()).thenReturn(List.of());

        List<TicketResponse> results = ticketService.getAllTickets();

        assertThat(results).isEmpty();
        verify(ticketRepository, times(1)).findAllByOrderByCreatedAtDesc();
    }

    @Test
    @DisplayName("Should map all ticket fields correctly")
    void getAllTickets_shouldMapFieldsCorrectly() {
        TicketResponse expectedResponse = TicketResponse.builder()
                .id(1L)
                .title("Login issue reported")
                .category(Category.BUG)
                .priority(Priority.HIGH)
                .summary("User cannot access account.")
                .commentId(1L)
                .originalComment("Can't log in")
                .build();

        when(ticketRepository.findAllByOrderByCreatedAtDesc())
                .thenReturn(List.of(mockBugTicket));
        when(ticketMapper.toResponse(mockBugTicket)).thenReturn(expectedResponse);

        List<TicketResponse> results = ticketService.getAllTickets();

        TicketResponse result = results.get(0);
        assertThat(result.getTitle()).isEqualTo("Login issue reported");
        assertThat(result.getCategory()).isEqualTo(Category.BUG);
        assertThat(result.getPriority()).isEqualTo(Priority.HIGH);
        assertThat(result.getSummary()).isEqualTo("User cannot access account.");
        assertThat(result.getCommentId()).isEqualTo(1L);
        assertThat(result.getOriginalComment()).isEqualTo("Can't log in");
    }

    // -------------------------------------------------------
    // getTicketById tests
    // -------------------------------------------------------

    @Test
    @DisplayName("Should return ticket when found by ID")
    void getTicketById_whenExists_shouldReturnTicket() {
        TicketResponse expectedResponse = TicketResponse.builder()
                .id(1L)
                .title("Login issue reported")
                .category(Category.BUG)
                .priority(Priority.HIGH)
                .build();

        when(ticketRepository.findById(1L)).thenReturn(Optional.of(mockBugTicket));
        when(ticketMapper.toResponse(mockBugTicket)).thenReturn(expectedResponse);

        TicketResponse result = ticketService.getTicketById(1L);

        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(1L);
        assertThat(result.getCategory()).isEqualTo(Category.BUG);
        assertThat(result.getPriority()).isEqualTo(Priority.HIGH);
    }

    @Test
    @DisplayName("Should throw ResourceNotFoundException when ticket not found")
    void getTicketById_whenNotFound_shouldThrowResourceNotFoundException() {
        when(ticketRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> ticketService.getTicketById(99L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Ticket")
                .hasMessageContaining("99");
    }

    @Test
    @DisplayName("Should return correct ticket for FEATURE category")
    void getTicketById_whenFeatureTicket_shouldReturnCorrectCategory() {
        TicketResponse featureResponse = TicketResponse.builder()
                .id(2L)
                .category(Category.FEATURE)
                .priority(Priority.MEDIUM)
                .build();

        when(ticketRepository.findById(2L)).thenReturn(Optional.of(mockFeatureTicket));
        when(ticketMapper.toResponse(mockFeatureTicket)).thenReturn(featureResponse);

        TicketResponse result = ticketService.getTicketById(2L);

        assertThat(result.getCategory()).isEqualTo(Category.FEATURE);
        assertThat(result.getPriority()).isEqualTo(Priority.MEDIUM);
    }

    @Test
    @DisplayName("Should return correct ticket for BILLING category")
    void getTicketById_whenBillingTicket_shouldReturnCorrectCategory() {
        TicketResponse billingResponse = TicketResponse.builder()
                .id(3L)
                .category(Category.BILLING)
                .priority(Priority.HIGH)
                .build();

        when(ticketRepository.findById(3L)).thenReturn(Optional.of(mockBillingTicket));
        when(ticketMapper.toResponse(mockBillingTicket)).thenReturn(billingResponse);

        TicketResponse result = ticketService.getTicketById(3L);

        assertThat(result.getCategory()).isEqualTo(Category.BILLING);
        assertThat(result.getPriority()).isEqualTo(Priority.HIGH);
    }
}