package com.hpursan.sanlam_assessment.model;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.math.BigDecimal;

@Slf4j
@Getter
public class WithdrawalEvent {
    private final BigDecimal amount;
    private final Long accountId;
    private final String status;

    public WithdrawalEvent(BigDecimal amount, Long accountId, String status) {
        this.amount = amount;
        this.accountId = accountId;
        this.status = status;
    }

    @Override
    public String toString() {
        return "WithdrawalEvent{" +
                "amount=" + amount +
                ", accountId=" + accountId +
                ", status='" + status + '\'' +
                '}';
    }

    public String toJson() {
        ObjectMapper mapper = new ObjectMapper();

        try {
            return mapper.writeValueAsString(this);
        } catch (JsonProcessingException jpe) {
            log.error("Unable to process the event json for {} due to error {}", this, jpe.getMessage());
            return ""; // this would imply that we have empty notifications being published.
        }
    }
}
