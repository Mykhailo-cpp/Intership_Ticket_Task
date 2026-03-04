package ticket.exercise.pulsedesk.model.dto.response;

import ticket.exercise.pulsedesk.model.enums.Category;
import ticket.exercise.pulsedesk.model.enums.Priority;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.Builder;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TicketResponse {
    private Long id;
    private String title;
    private Category category;
    private Priority priority;
    private String summary;
    private LocalDateTime createdAt;
    private Long commentId;
    private String originalComment;
}
