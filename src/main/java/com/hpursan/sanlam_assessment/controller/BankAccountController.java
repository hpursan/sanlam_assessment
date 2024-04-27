package com.hpursan.sanlam_assessment.controller;


import com.hpursan.sanlam_assessment.service.BankAccountService;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.concurrent.CompletableFuture;

@Slf4j
@RestController
@AllArgsConstructor
@RequestMapping("/bank")
public class BankAccountController {

    private final BankAccountService bankAccountService;

    @PostMapping("/withdraw")
    public CompletableFuture<ResponseEntity<String>> withdraw( @RequestParam("accountId") Long accountId,
                                                               @RequestParam("amount") BigDecimal amount) {
        log.info("attempt withdrawal for account id {} for an amount of {}", accountId, amount);
        return bankAccountService.withdraw(accountId, amount)
                .thenApply(response -> ResponseEntity.status(HttpStatus.OK).body(response))
                .exceptionally(ex -> ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(ex.getMessage()));

    }
}