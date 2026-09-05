package com.chethu.paymentledgerservice.payment.provider;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.RecordComponent;
import java.util.Arrays;
import java.util.EnumSet;

import org.junit.jupiter.api.Test;

class PaymentProviderContractTest {

    @Test
    void genericProviderTypesDoNotDependOnPayOsSdkClasses() {
        assertTrue(allTypesAreProviderNeutral(PaymentCheckoutRequest.class.getRecordComponents()));
        assertTrue(allTypesAreProviderNeutral(PaymentCheckoutResult.class.getRecordComponents()));
        assertTrue(Arrays.stream(PaymentProvider.class.getDeclaredMethods())
                .flatMap(method -> Arrays.stream(method.getParameterTypes()))
                .map(Class::getName)
                .noneMatch(name -> name.startsWith("vn.payos")));
    }

    @Test
    void providerTypeContainsOnlyPayOs() {
        assertEquals(EnumSet.of(PaymentProviderType.PAYOS), EnumSet.allOf(PaymentProviderType.class));
    }

    private boolean allTypesAreProviderNeutral(RecordComponent[] components) {
        return Arrays.stream(components)
                .map(RecordComponent::getType)
                .map(Class::getName)
                .noneMatch(name -> name.startsWith("vn.payos"));
    }
}
