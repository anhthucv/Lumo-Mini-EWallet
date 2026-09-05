package com.chethu.paymentledgerservice.payment.provider;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.RecordComponent;
import java.util.Arrays;

import org.junit.jupiter.api.Test;

import com.chethu.paymentledgerservice.payment.payout.PayoutProvider;
import com.chethu.paymentledgerservice.payment.payout.ProviderPayoutLookupResult;
import com.chethu.paymentledgerservice.payment.payout.ProviderPayoutRequest;
import com.chethu.paymentledgerservice.payment.payout.ProviderPayoutResult;

class PayoutProviderContractTest {
    @Test
    void payoutBoundaryUsesProviderNeutralTypes() {
        assertNeutral(ProviderPayoutRequest.class.getRecordComponents());
        assertNeutral(ProviderPayoutResult.class.getRecordComponents());
        assertNeutral(ProviderPayoutLookupResult.class.getRecordComponents());
        assertTrue(Arrays.stream(PayoutProvider.class.getDeclaredMethods())
                .flatMap(method -> Arrays.stream(method.getParameterTypes()))
                .map(Class::getName)
                .noneMatch(name -> name.startsWith("vn.payos")));
    }

    private void assertNeutral(RecordComponent[] components) {
        assertTrue(Arrays.stream(components).map(RecordComponent::getType).map(Class::getName)
                .noneMatch(name -> name.startsWith("vn.payos")));
    }
}
