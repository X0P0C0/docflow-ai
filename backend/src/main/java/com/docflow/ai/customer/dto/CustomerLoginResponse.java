package com.docflow.ai.customer.dto;

import lombok.Data;

@Data
public class CustomerLoginResponse {
    private String token;
    private long expireSeconds;
    private CustomerInfo customer;

    @Data
    public static class CustomerInfo {
        private Long id;
        private String username;
        private String email;
        private String company;
        private String realName;
    }
}
