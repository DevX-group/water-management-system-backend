package com.backend.water_management_system.dto;

import java.math.BigDecimal;

public class SMSGatewayResponseDTO {

    private String status;
    private String message;
    private Data data;

    public SMSGatewayResponseDTO() {
    }

    public static class Data {
        
        private String to;
        private String from;
        private String message;
        private String status;
        private BigDecimal cost;
        private Integer smsCount;
        
        public Data() {
        }

        public String getTo() {
            return to;
        }
        public void setTo(String to) {
            this.to = to;
        }
        public String getFrom() {
            return from;
        }
        public void setFrom(String from) {
            this.from = from;
        }
        public String getMessage() {
            return message;
        }
        public void setMessage(String message) {
            this.message = message;
        }
        public String getStatus() {
            return status;
        }
        public void setStatus(String status) {
            this.status = status;
        }
        public BigDecimal getCost() {
            return cost;
        }
        public void setCost(BigDecimal cost) {
            this.cost = cost;
        }
        public Integer getSmsCount() {
            return smsCount;
        }
        public void setSmsCount(Integer smsCount) {
            this.smsCount = smsCount;
        }
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public Data getData() {
        return data;
    }

    public void setData(Data data) {
        this.data = data;
    }
}

