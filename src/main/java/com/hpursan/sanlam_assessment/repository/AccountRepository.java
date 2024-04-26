package com.hpursan.sanlam_assessment.repository;

import java.math.BigDecimal;

public interface AccountRepository {
    BigDecimal getCurrentBalance(Long accountId);
    void updateBalance(Long accountId, BigDecimal amt);
}
