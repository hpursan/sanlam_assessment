package com.hpursan.sanlam_assessment.service;

import com.hpursan.sanlam_assessment.exception.InsufficientFundsException;
import com.hpursan.sanlam_assessment.exception.PublishingFailedException;
import com.hpursan.sanlam_assessment.exception.WithdrawalFailedException;
import com.hpursan.sanlam_assessment.model.WithdrawalEvent;
import com.hpursan.sanlam_assessment.repository.AccountRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.retry.support.RetryTemplate;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.core.async.BlockingOutputStreamAsyncRequestBody;
import software.amazon.awssdk.services.sns.SnsAsyncClient;
import software.amazon.awssdk.services.sns.SnsClient;
import software.amazon.awssdk.services.sns.model.PublishRequest;

import java.math.BigDecimal;
import java.util.concurrent.CompletableFuture;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@RequiredArgsConstructor
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
    private final RetryTemplate retryTemplate;

    public CompletableFuture<String> withdraw(Long accountId, BigDecimal withdrawalAmt) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                validateWithdrawalAmount(withdrawalAmt);

                BigDecimal currentBalance = accountRepository.getCurrentBalance(accountId);
                validateSufficientFunds(currentBalance, withdrawalAmt);

                updateAccountBalance(accountId, withdrawalAmt);

                retryTemplate.execute(context -> {
                    publishWithdrawalEvent(withdrawalAmt, accountId, withdrawalSuccessfulEventStatus);
                    return null; // to satisfy the lamba expression
                });

                return withdrawalSuccessful;
            } catch (InsufficientFundsException | WithdrawalFailedException e) {
                throw e; // Rethrow known exceptions
            } catch (Exception e) {
                String errorMessage = String.format("Failed to withdraw amount %s from account %d", withdrawalAmt, accountId);
                log.error(errorMessage, e);
                throw new WithdrawalFailedException(errorMessage, e);
            }
        });
    }

    private void validateWithdrawalAmount(BigDecimal withdrawalAmt) {
        if (withdrawalAmt == null || withdrawalAmt.compareTo(BigDecimal.ZERO) < 0) {
            log.error("Invalid withdrawal amount: {}", withdrawalAmt);
            throw new IllegalArgumentException("Withdrawal amount must be non-null and positive");
        }
    }

    private void validateSufficientFunds(BigDecimal currentBalance, BigDecimal withdrawalAmt) {
        if (currentBalance == null || currentBalance.compareTo(withdrawalAmt) < 0) {
            String errMsg = String.format("Insufficient funds in account to withdraw amount %s", withdrawalAmt);
            log.error(errMsg);
            throw new InsufficientFundsException(errMsg);
        }
    }

    private void updateAccountBalance(Long accountId, BigDecimal withdrawalAmt) {
        try {
            accountRepository.updateBalance(accountId, withdrawalAmt);
        } catch (Exception e) {
            String errMsg = String.format("Failed to update balance for account %d", accountId);
            log.error(errMsg, e);
            throw new WithdrawalFailedException(errMsg, e);
        }
    }

    private void publishWithdrawalEvent(BigDecimal amount, Long accountId, String status) {

        try {

            log.info("Publishing withdrawal event for account {} and an amount of {}({})", accountId, amount, status);
            String snsTopicArn = withdrawalSuccessfulEventTopic;
            WithdrawalEvent event = new WithdrawalEvent(amount, accountId, status);
            String eventJson = event.toJson();
            PublishRequest publishRequest = PublishRequest.builder()
                    .message(eventJson)
                    .topicArn(snsTopicArn)
                    .build();

            snsClient.publish(publishRequest);
            log.info("Successfully published withdrawal event {} for account {} and an amount of {}({})", eventJson, accountId, amount, status);
        } catch (Exception e) {
            // not sure what else to do if we cannot publish the event. so for now, will log it.
            log.error("An error occurred when publishing withdrawal event for account {} and an amount of {}({}): {}", accountId, amount, status, e.getMessage());
            throw new PublishingFailedException("Failed to publish withdrawal event", e);
        }
    }
}
