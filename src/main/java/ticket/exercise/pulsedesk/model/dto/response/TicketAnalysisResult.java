package ticket.exercise.pulsedesk.model.dto.response;

import ticket.exercise.pulsedesk.model.enums.Category;
import ticket.exercise.pulsedesk.model.enums.Priority;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.Builder;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TicketAnalysisResult {
    private boolean shouldCreateTicket;
    private String title;
    private Category category;
    private Priority priority;
    private String summary;
}
