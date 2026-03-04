package ticket.exercise.pulsedesk.mapper;

import ticket.exercise.pulsedesk.model.dto.request.CommentRequest;
import ticket.exercise.pulsedesk.model.dto.response.CommentResponse;
import ticket.exercise.pulsedesk.model.entity.Comment;

import org.springframework.stereotype.Component;

@Component
public class CommentMapper {

    public Comment toEntity(CommentRequest request) {
        return Comment.builder()
                .content(request.getContent())
                .source(request.getSource())
                .build();
    }

    public CommentResponse toResponse(Comment comment) {
        return CommentResponse.builder()
                .id(comment.getId())
                .content(comment.getContent())
                .source(comment.getSource())
                .createdAt(comment.getCreatedAt())
                .analyzed(comment.isAnalyzed())
                .convertedToTicket(comment.isConvertedToTicket())
                .ticketId(comment.getTicket() != null ? comment.getTicket().getId() : null)
                .build();
    }
}
