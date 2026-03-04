package ticket.exercise.pulsedesk.model.dto.reponse;

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
public class CommentResponse {
    private Long id;
    private String content;
    private String source;
    private LocalDateTime createdAt;
    private boolean analyzed;
    private boolean convertedToTicket;
    private Long ticketId;
}
