package com.chethu.paymentledgerservice.domain;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class AccountTest {
    @Test
     void testAccountId() {
        Account account = new Account((long) 1,"123","Thu");
        assertThat(account.getId()).isEqualTo(1);
        assertThat(account.getOwnerName()).isEqualTo("Thu");
        assertThat(account.getAccountNumber()).isEqualTo("123");
        assertThat(account.getBalance()).isEqualByComparingTo("0.00");
        assertThat(account.getStatus()).isEqualTo(AccountStatus.ACTIVE);
    }
    @Test
     void shouldRejectBlankOwnerName(){
        assertThatThrownBy(()-> new Account((long)1,"123",""))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("Owner name must not be blank");
        
    }
}
