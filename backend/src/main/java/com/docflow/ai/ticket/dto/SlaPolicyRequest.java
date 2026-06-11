package com.docflow.ai.ticket.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class SlaPolicyRequest {
    @NotBlank(message = "Policy name is required")
    private String name;
    private String ticketType;
    private Integer priority;
    @NotNull(message = "Response hours is required")
    private Integer responseHours;
    @NotNull(message = "Resolve hours is required")
    private Integer resolveHours;
    private Integer businessHoursOnly;
    private Integer status;
}
