package com.docflow.ai.customer.service;

import com.docflow.ai.customer.dto.*;
import java.util.List;

public interface CustomerPortalService {
    CustomerLoginResponse register(CustomerRegisterRequest request);
    CustomerLoginResponse login(CustomerLoginRequest request);
    CustomerTicketResponse submitTicket(Long customerId, CustomerTicketRequest request);
    List<CustomerTicketResponse> listMyTickets(Long customerId);
    CustomerTicketResponse getTicketDetail(Long ticketId, Long customerId);
    CustomerTicketResponse rateSatisfaction(Long ticketId, Long customerId, CustomerSatisfactionRequest request);
}
