package com.hpursan.sanlam_assessment.service;

import com.hpursan.sanlam_assessment.exception.InsufficientFundsException;
import com.hpursan.sanlam_assessment.exception.WithdrawalFailedException;
import com.hpursan.sanlam_assessment.model.WithdrawalEvent;
import com.hpursan.sanlam_assessment.repository.AccountRepository;
import lombok.AllArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.sns.SnsClient;
import software.amazon.awssdk.services.sns.model.PublishRequest;

import java.math.BigDecimal;
import java.util.concurrent.CompletableFuture;

@AllArgsConstructor
@Service
public class BankAccountService {

    @Value("${status.withdrawal.successful}")
    private String withdrawalSuccessful;

    @Value("${status.withdrawal.insufficient_funds}")
    private String insufficientFunds;

    @Value("${event.status.withdrawal.successful}")
    private String withdrawalSuccessfulEventStatus;

    @Value("${event.topic.withdrawal.successful}")
    private String withdrawalSuccessfulEventTopic;

    // the following will be initialised in the constructor provided by lombok
    // these are intentionally private and final to enforce immutability
    private final SnsClient snsClient;
    private final AccountRepository accountRepository;

    public CompletableFuture<String> withdraw(Long accountId, BigDecimal withdrawalAmt){
        return CompletableFuture.supplyAsync(() -> {

            try {
                // get the current balance
                BigDecimal currentBalance = accountRepository.getCurrentBalance(accountId);

                // attempt to update the balance if there's sufficient funds and if successful, publish the event
                if (currentBalance != null && currentBalance.compareTo(withdrawalAmt) >= 0) {
                    accountRepository.updateBalance(accountId, withdrawalAmt);
                    publishWithdrawalEvent(withdrawalAmt, accountId, withdrawalSuccessfulEventStatus);
                    return withdrawalSuccessful;
                } else {
                    throw new InsufficientFundsException("Insufficient funds for withdrawal");
                }

            } catch (Exception e) {
                throw new WithdrawalFailedException("Withdrawal failed", e);
            }

        });
    }

    private void publishWithdrawalEvent(BigDecimal amount, Long accountId, String status) {

        String snsTopicArn = withdrawalSuccessfulEventTopic;
        WithdrawalEvent event = new WithdrawalEvent(amount, accountId, status);
        String eventJson = event.toJson();
        PublishRequest publishRequest = PublishRequest.builder()
                .message(eventJson)
                .topicArn(snsTopicArn)
                .build();

        snsClient.publish(publishRequest);
    }
}
