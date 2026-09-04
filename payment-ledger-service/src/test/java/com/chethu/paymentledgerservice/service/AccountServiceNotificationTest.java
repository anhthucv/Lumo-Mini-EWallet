package com.chethu.paymentledgerservice.service;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;

import com.chethu.paymentledgerservice.domain.AccountClass;
import com.chethu.paymentledgerservice.domain.AccountStatus;
import com.chethu.paymentledgerservice.domain.LedgerAccountType;
import com.chethu.paymentledgerservice.domain.RiskDecision;
import com.chethu.paymentledgerservice.domain.TransactionType;
import com.chethu.paymentledgerservice.dto.MoneyOperationRequest;
import com.chethu.paymentledgerservice.dto.TransferRequest;
import com.chethu.paymentledgerservice.entity.AccountEntity;
import com.chethu.paymentledgerservice.entity.IdempotencyRecordEntity;
import com.chethu.paymentledgerservice.entity.JournalEntity;
import com.chethu.paymentledgerservice.entity.LedgerAccountEntity;
import com.chethu.paymentledgerservice.exception.RiskRejectedException;
import com.chethu.paymentledgerservice.repository.AccountRepository;
import com.chethu.paymentledgerservice.repository.JournalRepository;
import com.chethu.paymentledgerservice.repository.LedgerAccountRepository;

class AccountServiceNotificationTest {
    @Test
    void successfulDepositPublishesOneEventAndWithdrawPublishesNone() {
        Fixture fixture = fixture();
        AccountEntity account = account("ACC-OWNER", 1L, "300000.00");
        when(fixture.accounts.findByUserId(42L)).thenReturn(Optional.of(account));
        when(fixture.accounts.findByIdForUpdate(1L)).thenReturn(Optional.of(account));

        fixture.service.depositForCurrentUser(42L, money("100000.00"), null);
        fixture.service.withdrawForCurrentUser(42L, money("100000.00"), null);

        verify(fixture.notifications).publishDepositSuccess(eq(account), eq(new BigDecimal("100000.00")), any());
        verify(fixture.notifications, never()).publishWithdrawSuccess(any(), any(), any());
    }

    @Test
    void successfulTransferPublishesRecipientEventOnly() {
        Fixture fixture = fixture();
        AccountEntity sender = account("ACC-SENDER", 1L, "300000.00");
        AccountEntity recipient = account("ACC-RECIPIENT", 2L, "50000.00");
        when(fixture.accounts.findByUserId(42L)).thenReturn(Optional.of(sender));
        when(fixture.accounts.findByAccountNumber("ACC-RECIPIENT")).thenReturn(Optional.of(recipient));
        when(fixture.accounts.findByIdForUpdate(1L)).thenReturn(Optional.of(sender));
        when(fixture.accounts.findByIdForUpdate(2L)).thenReturn(Optional.of(recipient));

        fixture.service.transferForCurrentUser(42L, transfer("ACC-RECIPIENT", "100000.00"), null);

        verify(fixture.notifications).publishTransferReceived(eq(recipient), eq(sender),
                eq(new BigDecimal("100000.00")), any());
        verify(fixture.notifications, never()).publishTransferSent(any(), any(), any(), any());
    }

    @Test
    void rejectedTransferPublishesNoSuccessEvent() {
        Fixture fixture = fixture();
        AccountEntity sender = account("ACC-SENDER", 1L, "300000.00");
        AccountEntity recipient = account("ACC-RECIPIENT", 2L, "50000.00");
        when(fixture.accounts.findByUserId(42L)).thenReturn(Optional.of(sender));
        when(fixture.accounts.findByAccountNumber("ACC-RECIPIENT")).thenReturn(Optional.of(recipient));
        when(fixture.accounts.findByIdForUpdate(1L)).thenReturn(Optional.of(sender));
        when(fixture.accounts.findByIdForUpdate(2L)).thenReturn(Optional.of(recipient));
        when(fixture.risk.evaluate(any(), any(), any()))
                .thenReturn(new RiskEvaluationResult(RiskDecision.REJECT, List.of()));

        org.junit.jupiter.api.Assertions.assertThrows(RiskRejectedException.class,
                () -> fixture.service.transferForCurrentUser(42L, transfer("ACC-RECIPIENT", "100000.00"), null));

        verify(fixture.notifications, never()).publishTransferSent(any(), any(), any(), any());
        verify(fixture.notifications, never()).publishTransferReceived(any(), any(), any(), any());
    }

    @Test
    void completedDepositReplayDoesNotPublishAnotherEvent() {
        Fixture fixture = fixture();
        AccountEntity account = account("ACC-OWNER", 1L, "300000.00");
        IdempotencyRecordEntity replay = mock(IdempotencyRecordEntity.class);
        when(fixture.accounts.findByUserId(42L)).thenReturn(Optional.of(account));
        when(fixture.accounts.findByIdForUpdate(1L)).thenReturn(Optional.of(account));
        when(fixture.idempotency.findExisting(any(), any(), any(), any(), any()))
                .thenReturn(null, replay);
        when(replay.getResultBalance()).thenReturn(new BigDecimal("400000.00"));

        fixture.service.depositForCurrentUser(42L, money("100000.00"), "deposit-key");
        fixture.service.depositForCurrentUser(42L, money("100000.00"), "deposit-key");

        verify(fixture.notifications).publishDepositSuccess(eq(account), eq(new BigDecimal("100000.00")), any());
    }

    private Fixture fixture() {
        AccountRepository accounts = mock(AccountRepository.class);
        LedgerAccountRepository ledgers = mock(LedgerAccountRepository.class);
        JournalRepository journals = mock(JournalRepository.class);
        IdempotencyService idempotency = mock(IdempotencyService.class);
        TransactionLimitService limits = mock(TransactionLimitService.class);
        RiskEvaluationService risk = mock(RiskEvaluationService.class);
        RiskAuditService audit = mock(RiskAuditService.class);
        NotificationEventService notifications = mock(NotificationEventService.class);
        when(idempotency.findExisting(any(), any(), any(), any(), any())).thenReturn(null);
        when(ledgers.findByWalletAccount(any())).thenAnswer(invocation ->
                Optional.of(new LedgerAccountEntity("WALLET", LedgerAccountType.WALLET,
                        AccountClass.LIABILITY, invocation.getArgument(0))));
        when(ledgers.findByCode("SYSTEM_CLEARING")).thenReturn(Optional.of(
                new LedgerAccountEntity("SYSTEM_CLEARING", LedgerAccountType.SYSTEM_CLEARING,
                        AccountClass.ASSET, null)));
        when(journals.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(accounts.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(risk.evaluate(any(), any(), any()))
                .thenReturn(new RiskEvaluationResult(RiskDecision.ALLOW, List.of()));
        AccountService service = new AccountService(accounts, mock(TransactionService.class),
                mock(AccountNumberGenerator.class), ledgers, journals, idempotency, limits, risk, audit,
                notifications);
        return new Fixture(accounts, idempotency, risk, notifications, service);
    }

    private AccountEntity account(String number, Long id, String balance) {
        AccountEntity account = new AccountEntity(number, "Owner");
        account.deposit(new BigDecimal(balance));
        try {
            java.lang.reflect.Field field = AccountEntity.class.getDeclaredField("id");
            field.setAccessible(true);
            field.set(account, id);
        } catch (ReflectiveOperationException ex) {
            throw new IllegalStateException(ex);
        }
        return account;
    }

    private MoneyOperationRequest money(String amount) {
        MoneyOperationRequest request = new MoneyOperationRequest();
        request.setAmount(new BigDecimal(amount));
        return request;
    }

    private TransferRequest transfer(String accountNumber, String amount) {
        TransferRequest request = new TransferRequest();
        request.setRecipientAccountNumber(accountNumber);
        request.setAmount(new BigDecimal(amount));
        return request;
    }

    private record Fixture(AccountRepository accounts, IdempotencyService idempotency, RiskEvaluationService risk,
            NotificationEventService notifications, AccountService service) {
    }
}
