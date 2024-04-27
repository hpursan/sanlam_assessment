package com.hpursan.sanlam_assessment.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.sns.SnsClient;

@Configuration
public class AwsConfiguration {

    @Bean
    // this may need to be AsyncClient instead of SnsClient if we want to use the async version of the client
    public SnsClient snsClient(@Value("${aws.region}") String region) {
        return SnsClient.builder()
                .region(Region.of(region))
                .build();
    }
}

