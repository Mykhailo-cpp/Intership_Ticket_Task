package ticket.exercise.pulsedesk.service;

import ticket.exercise.pulsedesk.model.dto.response.TicketResponse;

import java.util.List;

public interface TicketService {

    List<TicketResponse> getAllTickets();

    TicketResponse getTicketById(Long id);
}
