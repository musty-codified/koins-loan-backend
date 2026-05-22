package com.koins.loanbackend.dto.webhook;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class PaystackWebhookPayload {

    private String event;
    private Data data;

    public String getEvent() { return event; }
    public void setEvent(String event) { this.event = event; }

    public Data getData() { return data; }
    public void setData(Data data) { this.data = data; }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Data {
        private String reference;
        private long amount; // in kobo
        private String status;
        private Customer customer;

        public String getReference() { return reference; }
        public void setReference(String reference) { this.reference = reference; }

        public long getAmount() { return amount; }
        public void setAmount(long amount) { this.amount = amount; }

        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }

        public Customer getCustomer() { return customer; }
        public void setCustomer(Customer customer) { this.customer = customer; }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Customer {
        private String email;

        public String getEmail() { return email; }
        public void setEmail(String email) { this.email = email; }
    }
}