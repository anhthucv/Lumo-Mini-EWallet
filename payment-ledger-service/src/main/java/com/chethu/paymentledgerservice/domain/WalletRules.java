package com.chethu.paymentledgerservice.domain;

import java.math.BigDecimal;

public final class WalletRules {
    public static final BigDecimal MINIMUM_OPERATION_AMOUNT = new BigDecimal("1.00");
    public static final BigDecimal MINIMUM_BALANCE = new BigDecimal("50000.00") ;
    private WalletRules(){}


}
