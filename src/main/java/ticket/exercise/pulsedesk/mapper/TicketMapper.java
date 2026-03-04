package ticket.exercise.pulsedesk.mapper;

import ticket.exercise.pulsedesk.model.dto.response.TicketAnalysisResult;
import ticket.exercise.pulsedesk.model.dto.response.TicketResponse;
import ticket.exercise.pulsedesk.model.entity.Comment;
import ticket.exercise.pulsedesk.model.entity.Ticket;
import org.springframework.stereotype.Component;

@Component
public class TicketMapper {

    public Ticket toEntity(TicketAnalysisResult result, Comment comment) {
        return Ticket.builder()
                .title(result.getTitle())
                .category(result.getCategory())
                .priority(result.getPriority())
                .summary(result.getSummary())
                .comment(comment)
                .build();
    }

    public TicketResponse toResponse(Ticket ticket) {
        return TicketResponse.builder()
                .id(ticket.getId())
                .title(ticket.getTitle())
                .category(ticket.getCategory())
                .priority(ticket.getPriority())
                .summary(ticket.getSummary())
                .createdAt(ticket.getCreatedAt())
                .commentId(ticket.getComment().getId())
                .originalComment(ticket.getComment().getContent())
                .build();
    }
}
