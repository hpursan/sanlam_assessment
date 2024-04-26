package com.hpursan.sanlam_assessment.controller;


import com.hpursan.sanlam_assessment.service.BankAccountService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.beans.factory.annotation.Value;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.sns.SnsAsyncClient;
import java.math.BigDecimal;
import java.util.concurrent.CompletableFuture;


@RestController
@RequestMapping("/bank")
public class BankAccountController {

    private final BankAccountService bankAccountService;
    private final SnsAsyncClient snsClient;

    @Autowired
    // the controller will have the Region value injected from the properties file
    public BankAccountController(BankAccountService bankAccountService, @Value("${aws.region}") String region) {
        this.bankAccountService = bankAccountService;
        this.snsClient = SnsAsyncClient.builder().region(Region.of(region)).build();
    }

    @PostMapping("/withdraw")
    public CompletableFuture<ResponseEntity<String>> withdraw( @RequestParam("accountId") Long accountId,
                                                               @RequestParam("amount") BigDecimal amount) {
        return bankAccountService.withdraw(accountId, amount)
                .thenApply(response -> ResponseEntity.status(HttpStatus.OK).body(response))
                .exceptionally(ex -> ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(ex.getMessage()));

    }
}