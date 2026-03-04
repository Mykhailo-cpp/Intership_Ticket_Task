package ticket.exercise.pulsedesk.service.impl;

import ticket.exercise.pulsedesk.exception.ResourceNotFoundException;
import ticket.exercise.pulsedesk.mapper.CommentMapper;
import ticket.exercise.pulsedesk.mapper.TicketMapper;
import ticket.exercise.pulsedesk.model.dto.request.CommentRequest;
import ticket.exercise.pulsedesk.model.dto.response.CommentResponse;
import ticket.exercise.pulsedesk.model.dto.response.TicketAnalysisResult;
import ticket.exercise.pulsedesk.model.entity.Comment;
import ticket.exercise.pulsedesk.model.entity.Ticket;
import ticket.exercise.pulsedesk.repository.CommentRepository;
import ticket.exercise.pulsedesk.repository.TicketRepository;
import ticket.exercise.pulsedesk.service.AiAnalysisService;
import ticket.exercise.pulsedesk.service.CommentService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class CommentServiceImpl implements CommentService {

    private final CommentRepository commentRepository;
    private final TicketRepository ticketRepository;
    private final AiAnalysisService aiAnalysisService;
    private final CommentMapper commentMapper;
    private final TicketMapper ticketMapper;

    @Override
    @Transactional
    public CommentResponse submitComment(CommentRequest request) {
        log.info("Submitting new comment from source: {}", request.getSource());

        Comment comment = commentMapper.toEntity(request);
        comment = commentRepository.save(comment);

        TicketAnalysisResult analysisResult = aiAnalysisService.analyze(comment.getContent());

        comment.setAnalyzed(true);
        comment.setConvertedToTicket(analysisResult.isShouldCreateTicket());

        if (analysisResult.isShouldCreateTicket()) {
            log.info("Comment {} qualifies for ticket creation", comment.getId());
            Ticket ticket = ticketMapper.toEntity(analysisResult, comment);
            ticketRepository.save(ticket);
            comment.setTicket(ticket);
        }

        comment = commentRepository.save(comment);
        return commentMapper.toResponse(comment);
    }

    @Override
    @Transactional(readOnly = true)
    public List<CommentResponse> getAllComments() {
        return commentRepository.findAllByOrderByCreatedAtDesc()
                .stream()
                .map(commentMapper::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public CommentResponse getCommentById(Long id) {
        Comment comment = commentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Comment", id));
        return commentMapper.toResponse(comment);
    }
}
