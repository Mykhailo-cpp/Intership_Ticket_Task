package ticket.exercise.pulsedesk.service.impl;

import ticket.exercise.pulsedesk.exception.ResourceNotFoundException;
import ticket.exercise.pulsedesk.mapper.TicketMapper;
import ticket.exercise.pulsedesk.model.dto.response.TicketResponse;
import ticket.exercise.pulsedesk.model.entity.Ticket;
import ticket.exercise.pulsedesk.repository.TicketRepository;
import ticket.exercise.pulsedesk.service.TicketService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class TicketServiceImpl implements TicketService {

    private final TicketRepository ticketRepository;
    private final TicketMapper ticketMapper;

    @Override
    @Transactional(readOnly = true)
    public List<TicketResponse> getAllTickets() {
        return ticketRepository.findAllByOrderByCreatedAtDesc()
                .stream()
                .map(ticketMapper::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public TicketResponse getTicketById(Long id) {
        Ticket ticket = ticketRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Ticket", id));
        return ticketMapper.toResponse(ticket);
    }
}
