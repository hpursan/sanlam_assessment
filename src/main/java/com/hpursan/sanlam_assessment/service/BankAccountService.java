package com.hpursan.sanlam_assessment.service;

import com.hpursan.sanlam_assessment.exception.InsufficientFundsException;
import com.hpursan.sanlam_assessment.exception.WithdrawalFailedException;
import com.hpursan.sanlam_assessment.model.WithdrawalEvent;
import com.hpursan.sanlam_assessment.repository.AccountRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
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

    public CompletableFuture<String> withdraw(Long accountId, BigDecimal withdrawalAmt){
        return CompletableFuture.supplyAsync(() -> {

            try {
                log.info("Get the account balance for account id {}", accountId);
                BigDecimal currentBalance = accountRepository.getCurrentBalance(accountId);

                // attempt to update the balance if there's sufficient funds and if successful, publish the event
                if (currentBalance != null && currentBalance.compareTo(withdrawalAmt) >= 0) {
                    log.info("There is sufficient balance in account {} to withdraw an amount of {}", accountId, withdrawalAmt);
                    accountRepository.updateBalance(accountId, withdrawalAmt);
                    publishWithdrawalEvent(withdrawalAmt, accountId, withdrawalSuccessfulEventStatus);
                    return withdrawalSuccessful;
                } else {
                    String err_msg = String.format("Insufficient funds in account %s to withdraw %b", accountId, withdrawalAmt);
                    log.error(err_msg);
                    throw new InsufficientFundsException(err_msg);
                }

            } catch (InsufficientFundsException ife) {
                // nothing much to do here as we have already logged the error. so for now, let's rethrow
                throw ife;
            }
            catch (Exception e) {
                String errorMessage = String.format("Failed to withdraw amount %s from account %d", withdrawalAmt, accountId);
                log.error(errorMessage, e);
                throw new WithdrawalFailedException(errorMessage, e);
            }

        });
    }

    private void publishWithdrawalEvent(BigDecimal amount, Long accountId, String status) {

        try {

            log.info("Publishing withdrawal event for account {} and an amount of {}({})", accountId, amount, status);
            String snsTopicArn = withdrawalSuccessfulEventTopic;
            WithdrawalEvent event = new WithdrawalEvent(amount, accountId, status);
            String eventJson = event.toJson();
            PublishRequest publishRequest = PublishRequest.builder()
                    .message(eventJson) // how should we handle json processing exception ?
                    .topicArn(snsTopicArn)
                    .build();

            snsClient.publish(publishRequest);
            log.info("Successfully published withdrawal event {} for account {} and an amount of {}({})", eventJson, accountId, amount, status);
        } catch (Exception e) {
            // not sure what else to do if we cannot publish the event. so for now, will log it.
            log.error("An error occurred when publishing withdrawal event for account {} and an amount of {}({}): {}", accountId, amount, status, e.getMessage());
        }
    }
}
