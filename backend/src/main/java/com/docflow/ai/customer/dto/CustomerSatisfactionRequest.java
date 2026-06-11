package com.docflow.ai.customer.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class CustomerSatisfactionRequest {
    @NotNull
    @Min(1) @Max(5)
    private Integer score;
    private String comment;
}
