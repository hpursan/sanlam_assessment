package com.hpursan.sanlam_assessment.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.retry.annotation.EnableRetry;
import org.springframework.retry.backoff.FixedBackOffPolicy;
import org.springframework.retry.policy.SimpleRetryPolicy;
import org.springframework.retry.support.RetryTemplate;

@Configuration
@EnableRetry
public class RetryConfiguration {

    @Value("${retry.maxAttempts}")
    private int maxAttempts;

    @Value("${retry.backOffPeriod}")
    private long backOffPeriod;

    @Bean
    public RetryTemplate retryTemplate() {
        RetryTemplate retryTemplate = new RetryTemplate();

        // Configure retry policy
        SimpleRetryPolicy retryPolicy = new SimpleRetryPolicy();
        retryPolicy.setMaxAttempts(maxAttempts); // Maximum number of retry attempts
        retryTemplate.setRetryPolicy(retryPolicy);

        // Configure backoff policy
        FixedBackOffPolicy backOffPolicy = new FixedBackOffPolicy();
        backOffPolicy.setBackOffPeriod(backOffPeriod); // Backoff period in milliseconds between retries
        retryTemplate.setBackOffPolicy(backOffPolicy);

        return retryTemplate;
    }
}
