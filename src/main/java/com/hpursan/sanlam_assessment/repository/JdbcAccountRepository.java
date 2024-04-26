package com.hpursan.sanlam_assessment.repository;

import lombok.AllArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;

@AllArgsConstructor
@Repository
public class JdbcAccountRepository implements AccountRepository{

    private final JdbcTemplate jdbcTemplate;

    @Override
    public BigDecimal getCurrentBalance(Long accountId) {
        String sql = "SELECT balance FROM accounts WHERE id = ?";
        return jdbcTemplate.queryForObject(sql, new Object[]{accountId}, BigDecimal.class);
    }

    @Override
    public void updateBalance(Long accountId, BigDecimal amt) {
        String sql = "UPDATE accounts SET balance = balance - ? WHERE id = ?";
        jdbcTemplate.update(sql, amt, accountId);
    }

}
