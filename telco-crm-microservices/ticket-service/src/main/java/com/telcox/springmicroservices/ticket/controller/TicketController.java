package com.telcox.springmicroservices.ticket.controller;

import com.telcox.springmicroservices.ticket.dto.TicketCreateRequest;
import com.telcox.springmicroservices.ticket.dto.TicketResponse;
import com.telcox.springmicroservices.ticket.service.TicketService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/tickets")
@RequiredArgsConstructor
public class TicketController {

    private final TicketService ticketService;

    @PostMapping
    public ResponseEntity<TicketResponse> createTicket(@RequestBody TicketCreateRequest request) {
        TicketResponse response = ticketService.createTicket(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
}
