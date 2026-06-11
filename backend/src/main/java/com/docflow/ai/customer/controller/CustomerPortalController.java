package com.docflow.ai.customer.controller;

import com.docflow.ai.common.domain.ApiResponse;
import com.docflow.ai.customer.auth.CustomerId;
import com.docflow.ai.customer.dto.*;
import com.docflow.ai.customer.service.CustomerPortalService;
import io.micrometer.core.annotation.Timed;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/customer")
@RequiredArgsConstructor
@Tag(name = "Customer Portal", description = "Customer self-service portal")
public class CustomerPortalController {

    private final CustomerPortalService portalService;

    @PostMapping("/register")
    @Timed(value = "customer.register", histogram = true)
    @Operation(summary = "Customer registration")
    public ApiResponse<CustomerLoginResponse> register(@Valid @RequestBody CustomerRegisterRequest request) {
        return ApiResponse.success(portalService.register(request));
    }

    @PostMapping("/login")
    @Timed(value = "customer.login", histogram = true)
    @Operation(summary = "Customer login")
    public ApiResponse<CustomerLoginResponse> login(@Valid @RequestBody CustomerLoginRequest request) {
        return ApiResponse.success(portalService.login(request));
    }

    @PostMapping("/tickets")
    @Timed(value = "customer.tickets", histogram = true)
    @Operation(summary = "Submit a new ticket")
    public ApiResponse<CustomerTicketResponse> submitTicket(@CustomerId Long customerId,
                                                            @Valid @RequestBody CustomerTicketRequest request) {
        return ApiResponse.success(portalService.submitTicket(customerId, request));
    }

    @GetMapping("/tickets")
    @Timed(value = "customer.tickets", histogram = true)
    @Operation(summary = "List my tickets")
    public ApiResponse<List<CustomerTicketResponse>> listMyTickets(@CustomerId Long customerId) {
        return ApiResponse.success(portalService.listMyTickets(customerId));
    }

    @GetMapping("/tickets/{ticketId}")
    @Timed(value = "customer.tickets", histogram = true)
    @Operation(summary = "Get ticket detail with timeline")
    public ApiResponse<CustomerTicketResponse> getTicketDetail(@PathVariable Long ticketId,
                                                                @CustomerId Long customerId) {
        return ApiResponse.success(portalService.getTicketDetail(ticketId, customerId));
    }

    @PostMapping("/tickets/{ticketId}/satisfaction")
    @Timed(value = "customer.tickets", histogram = true)
    @Operation(summary = "Rate ticket satisfaction")
    public ApiResponse<CustomerTicketResponse> rateSatisfaction(@PathVariable Long ticketId,
                                                                 @CustomerId Long customerId,
                                                                 @Valid @RequestBody CustomerSatisfactionRequest request) {
        return ApiResponse.success(portalService.rateSatisfaction(ticketId, customerId, request));
    }
}
