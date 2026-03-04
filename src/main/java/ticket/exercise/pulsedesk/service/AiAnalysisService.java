package ticket.exercise.pulsedesk.service;

import ticket.exercise.pulsedesk.model.dto.response.TicketAnalysisResult;

public interface AiAnalysisService {

    TicketAnalysisResult analyze(String commentContent);
}