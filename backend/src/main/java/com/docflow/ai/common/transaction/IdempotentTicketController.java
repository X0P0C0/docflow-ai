package com.docflow.ai.common.transaction;

import com.docflow.ai.auth.security.AuthUserPrincipal;
import com.docflow.ai.common.domain.ApiResponse;
import com.docflow.ai.ticket.dto.CreateTicketRequest;
import com.docflow.ai.ticket.dto.TicketDetailResponse;
import com.docflow.ai.ticket.service.TicketService;
import io.micrometer.core.annotation.Timed;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/tickets")
@RequiredArgsConstructor
@Tag(name = "Idempotent Ticket API", description = "Idempotent ticket creation with duplicate prevention")
public class IdempotentTicketController {

    private final TicketService ticketService;
    private final IdempotencyService idempotencyService;

    @PostMapping("/idempotent")
    @Timed(value = "tickets.write", histogram = true)
    @Operation(summary = "Create ticket with idempotency key (prevents duplicate submission)")
    public ApiResponse<TicketDetailResponse> createTicketIdempotent(
            @AuthenticationPrincipal AuthUserPrincipal principal,
            @RequestHeader("X-Idempotency-Key") String idempotencyKey,
            @Valid @RequestBody CreateTicketRequest request) {
        
        TicketDetailResponse result = idempotencyService.execute(idempotencyKey, 
                () -> ticketService.createTicket(principal.getUserId(), request));
        
        if (result == null) {
            return ApiResponse.success(null); // Duplicate request
        }
        return ApiResponse.success(result);
    }
}
