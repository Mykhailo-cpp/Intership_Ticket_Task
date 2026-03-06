package ticket.exercise.pulsedesk.controller;

import ticket.exercise.pulsedesk.model.dto.response.ApiResponse;
import ticket.exercise.pulsedesk.model.dto.response.TicketResponse;
import ticket.exercise.pulsedesk.service.TicketService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/tickets")
@RequiredArgsConstructor
public class TicketController {

    private final TicketService ticketService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<TicketResponse>>> getAllTickets() {
        log.info("GET /tickets");
        List<TicketResponse> tickets = ticketService.getAllTickets();
        return ResponseEntity.ok(ApiResponse.success(tickets));
    }

    @GetMapping("/{ticketId}")
    public ResponseEntity<ApiResponse<TicketResponse>> getTicketById(@PathVariable Long ticketId) {
        log.info("GET /tickets/{}", ticketId);
        TicketResponse ticket = ticketService.getTicketById(ticketId);
        return ResponseEntity.ok(ApiResponse.success(ticket));
    }
}
