package ticket.exercise.pulsedesk;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import ticket.exercise.pulsedesk.exception.ResourceNotFoundException;
import ticket.exercise.pulsedesk.mapper.CommentMapper;
import ticket.exercise.pulsedesk.mapper.TicketMapper;
import ticket.exercise.pulsedesk.model.dto.request.CommentRequest;
import ticket.exercise.pulsedesk.model.dto.response.CommentResponse;
import ticket.exercise.pulsedesk.model.dto.response.TicketAnalysisResult;
import ticket.exercise.pulsedesk.model.entity.Comment;
import ticket.exercise.pulsedesk.model.entity.Ticket;
import ticket.exercise.pulsedesk.model.enums.Category;
import ticket.exercise.pulsedesk.model.enums.Priority;
import ticket.exercise.pulsedesk.repository.CommentRepository;
import ticket.exercise.pulsedesk.repository.TicketRepository;
import ticket.exercise.pulsedesk.service.AiAnalysisService;
import ticket.exercise.pulsedesk.service.impl.CommentServiceImpl;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("CommentService Tests")
class CommentServiceTest {

    @Mock
    private CommentRepository commentRepository;

    @Mock
    private TicketRepository ticketRepository;

    @Mock
    private AiAnalysisService aiAnalysisService;

    @Mock
    private CommentMapper commentMapper;

    @Mock
    private TicketMapper ticketMapper;

    @InjectMocks
    private CommentServiceImpl commentService;

    private Comment mockComment;
    private CommentRequest commentRequest;

    @BeforeEach
    void setUp() {
        mockComment = Comment.builder()
                .id(1L)
                .content("The login button is broken and I cannot access my account!")
                .source("web-form")
                .createdAt(LocalDateTime.now())
                .analyzed(false)
                .convertedToTicket(false)
                .build();

        commentRequest = new CommentRequest(
                "The login button is broken and I cannot access my account!",
                "web-form"
        );
    }

    // -------------------------------------------------------
    // submitComment tests
    // -------------------------------------------------------

    @Test
    @DisplayName("Should create ticket when AI classifies comment as actionable")
    void submitComment_whenActionableComment_shouldCreateTicket() {
        TicketAnalysisResult actionableResult = TicketAnalysisResult.builder()
                .shouldCreateTicket(true)
                .title("Login button broken")
                .category(Category.BUG)
                .priority(Priority.HIGH)
                .summary("User unable to access account due to broken login button.")
                .build();

        Ticket mockTicket = Ticket.builder().id(10L).build();

        CommentResponse expectedResponse = CommentResponse.builder()
                .id(1L)
                .convertedToTicket(true)
                .ticketId(10L)
                .build();

        when(commentMapper.toEntity(commentRequest)).thenReturn(mockComment);
        when(commentRepository.save(any(Comment.class))).thenReturn(mockComment);
        when(aiAnalysisService.analyze(mockComment.getContent())).thenReturn(actionableResult);
        when(ticketMapper.toEntity(actionableResult, mockComment)).thenReturn(mockTicket);
        when(ticketRepository.save(any(Ticket.class))).thenReturn(mockTicket);
        when(commentMapper.toResponse(mockComment)).thenReturn(expectedResponse);

        CommentResponse result = commentService.submitComment(commentRequest);

        assertThat(result).isNotNull();
        assertThat(result.isConvertedToTicket()).isTrue();
        assertThat(result.getTicketId()).isEqualTo(10L);
        verify(ticketRepository, times(1)).save(any(Ticket.class));
        verify(commentRepository, times(2)).save(any(Comment.class));
    }

    @Test
    @DisplayName("Should NOT create ticket when AI classifies comment as compliment")
    void submitComment_whenCompliment_shouldNotCreateTicket() {
        TicketAnalysisResult nonActionableResult = TicketAnalysisResult.builder()
                .shouldCreateTicket(false)
                .build();

        CommentResponse expectedResponse = CommentResponse.builder()
                .id(1L)
                .convertedToTicket(false)
                .ticketId(null)
                .build();

        when(commentMapper.toEntity(commentRequest)).thenReturn(mockComment);
        when(commentRepository.save(any(Comment.class))).thenReturn(mockComment);
        when(aiAnalysisService.analyze(mockComment.getContent())).thenReturn(nonActionableResult);
        when(commentMapper.toResponse(mockComment)).thenReturn(expectedResponse);

        CommentResponse result = commentService.submitComment(commentRequest);

        assertThat(result.isConvertedToTicket()).isFalse();
        assertThat(result.getTicketId()).isNull();
        verify(ticketRepository, never()).save(any(Ticket.class));
    }

    @Test
    @DisplayName("Should create ticket for feature request category")
    void submitComment_whenFeatureRequest_shouldCreateFeatureTicket() {
        CommentRequest featureRequest = new CommentRequest(
                "It would be great if we could export data to CSV format.",
                "web-form"
        );

        Comment featureComment = Comment.builder()
                .id(2L)
                .content("It would be great if we could export data to CSV format.")
                .source("web-form")
                .build();

        TicketAnalysisResult featureResult = TicketAnalysisResult.builder()
                .shouldCreateTicket(true)
                .title("CSV export feature request")
                .category(Category.FEATURE)
                .priority(Priority.MEDIUM)
                .summary("User requests CSV data export functionality.")
                .build();

        Ticket featureTicket = Ticket.builder().id(11L).build();

        CommentResponse expectedResponse = CommentResponse.builder()
                .id(2L)
                .convertedToTicket(true)
                .ticketId(11L)
                .build();

        when(commentMapper.toEntity(featureRequest)).thenReturn(featureComment);
        when(commentRepository.save(any(Comment.class))).thenReturn(featureComment);
        when(aiAnalysisService.analyze(featureComment.getContent())).thenReturn(featureResult);
        when(ticketMapper.toEntity(featureResult, featureComment)).thenReturn(featureTicket);
        when(ticketRepository.save(any(Ticket.class))).thenReturn(featureTicket);
        when(commentMapper.toResponse(featureComment)).thenReturn(expectedResponse);

        CommentResponse result = commentService.submitComment(featureRequest);

        assertThat(result.isConvertedToTicket()).isTrue();
        assertThat(result.getTicketId()).isEqualTo(11L);
        verify(ticketRepository, times(1)).save(any(Ticket.class));
    }

    @Test
    @DisplayName("Should always save comment before AI analysis")
    void submitComment_shouldPersistCommentBeforeAnalysis() {
        TicketAnalysisResult result = TicketAnalysisResult.builder()
                .shouldCreateTicket(false)
                .build();

        when(commentMapper.toEntity(commentRequest)).thenReturn(mockComment);
        when(commentRepository.save(any(Comment.class))).thenReturn(mockComment);
        when(aiAnalysisService.analyze(any())).thenReturn(result);
        when(commentMapper.toResponse(mockComment)).thenReturn(CommentResponse.builder().build());

        commentService.submitComment(commentRequest);

        // Verify comment was saved BEFORE AI analysis was called
        var inOrder = inOrder(commentRepository, aiAnalysisService);
        inOrder.verify(commentRepository).save(any(Comment.class));
        inOrder.verify(aiAnalysisService).analyze(any());
    }

    @Test
    @DisplayName("Should mark comment as analyzed after AI processing")
    void submitComment_shouldMarkCommentAsAnalyzed() {
        TicketAnalysisResult result = TicketAnalysisResult.builder()
                .shouldCreateTicket(false)
                .build();

        when(commentMapper.toEntity(commentRequest)).thenReturn(mockComment);
        when(commentRepository.save(any(Comment.class))).thenReturn(mockComment);
        when(aiAnalysisService.analyze(any())).thenReturn(result);
        when(commentMapper.toResponse(mockComment)).thenReturn(CommentResponse.builder().build());

        commentService.submitComment(commentRequest);

        assertThat(mockComment.isAnalyzed()).isTrue();
    }

    // -------------------------------------------------------
    // getAllComments tests
    // -------------------------------------------------------

    @Test
    @DisplayName("Should return all comments ordered by date")
    void getAllComments_shouldReturnAllComments() {
        Comment comment2 = Comment.builder().id(2L).content("Another comment").build();

        when(commentRepository.findAllByOrderByCreatedAtDesc())
                .thenReturn(List.of(mockComment, comment2));
        when(commentMapper.toResponse(mockComment))
                .thenReturn(CommentResponse.builder().id(1L).build());
        when(commentMapper.toResponse(comment2))
                .thenReturn(CommentResponse.builder().id(2L).build());

        List<CommentResponse> results = commentService.getAllComments();

        assertThat(results).hasSize(2);
        assertThat(results.get(0).getId()).isEqualTo(1L);
        assertThat(results.get(1).getId()).isEqualTo(2L);
        verify(commentRepository, times(1)).findAllByOrderByCreatedAtDesc();
    }

    @Test
    @DisplayName("Should return empty list when no comments exist")
    void getAllComments_whenNoComments_shouldReturnEmptyList() {
        when(commentRepository.findAllByOrderByCreatedAtDesc()).thenReturn(List.of());

        List<CommentResponse> results = commentService.getAllComments();

        assertThat(results).isEmpty();
    }

    // -------------------------------------------------------
    // getCommentById tests
    // -------------------------------------------------------

    @Test
    @DisplayName("Should return comment when found by ID")
    void getCommentById_whenExists_shouldReturnComment() {
        CommentResponse expectedResponse = CommentResponse.builder()
                .id(1L)
                .content("The login button is broken!")
                .build();

        when(commentRepository.findById(1L)).thenReturn(Optional.of(mockComment));
        when(commentMapper.toResponse(mockComment)).thenReturn(expectedResponse);

        CommentResponse result = commentService.getCommentById(1L);

        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(1L);
        assertThat(result.getContent()).isEqualTo("The login button is broken!");
    }

    @Test
    @DisplayName("Should throw ResourceNotFoundException when comment not found")
    void getCommentById_whenNotFound_shouldThrowResourceNotFoundException() {
        when(commentRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> commentService.getCommentById(99L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Comment")
                .hasMessageContaining("99");
    }
}