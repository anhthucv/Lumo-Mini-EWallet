package com.chethu.paymentledgerservice.entity;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Field;
import java.math.BigDecimal;

import org.junit.jupiter.api.Test;

import com.chethu.paymentledgerservice.domain.AccountClass;
import com.chethu.paymentledgerservice.domain.LedgerAccountType;
import com.chethu.paymentledgerservice.domain.LedgerEntryType;

import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;

class LedgerFoundationTest {
    private final AccountEntity wallet = new AccountEntity("ACC-WALLET", "Wallet Owner");

    @Test
    void ledgerEntry_shouldSupportPositiveBigDecimalDebitAndCredit() {
        JournalEntity journal = new JournalEntity("deposit-1");
        LedgerAccountEntity clearing = new LedgerAccountEntity("SYSTEM_CLEARING",
                LedgerAccountType.SYSTEM_CLEARING, AccountClass.ASSET, null);
        LedgerAccountEntity walletLedger = new LedgerAccountEntity("WALLET-1",
                LedgerAccountType.WALLET, AccountClass.LIABILITY, wallet);

        LedgerEntryEntity debit = new LedgerEntryEntity(journal, clearing, LedgerEntryType.DEBIT,
                new BigDecimal("100000.00"));
        LedgerEntryEntity credit = new LedgerEntryEntity(journal, walletLedger, LedgerEntryType.CREDIT,
                new BigDecimal("100000.00"));

        assertEquals(new BigDecimal("100000.00"), debit.getAmount());
        assertEquals(LedgerEntryType.DEBIT, debit.getEntryType());
        assertEquals(LedgerEntryType.CREDIT, credit.getEntryType());
        assertTrue(journal.isBalanced());
        assertEquals(2, journal.getEntries().size());
    }

    @Test
    void ledgerEntry_shouldRejectZeroAndNegativeAmounts() {
        JournalEntity journal = new JournalEntity("invalid-entries");
        LedgerAccountEntity clearing = new LedgerAccountEntity("SYSTEM_CLEARING",
                LedgerAccountType.SYSTEM_CLEARING, AccountClass.ASSET, null);

        assertThrows(IllegalArgumentException.class,
                () -> new LedgerEntryEntity(journal, clearing, LedgerEntryType.DEBIT, BigDecimal.ZERO));
        assertThrows(IllegalArgumentException.class,
                () -> new LedgerEntryEntity(journal, clearing, LedgerEntryType.CREDIT,
                        new BigDecimal("-1.00")));
    }

    @Test
    void ledgerAccount_shouldDistinguishWalletAndSystemAccounts() {
        LedgerAccountEntity walletLedger = new LedgerAccountEntity("WALLET-1",
                LedgerAccountType.WALLET, AccountClass.LIABILITY, wallet);
        LedgerAccountEntity systemLedger = new LedgerAccountEntity("SYSTEM_CLEARING",
                LedgerAccountType.SYSTEM_CLEARING, AccountClass.ASSET, null);
        LedgerAccountEntity providerLedger = new LedgerAccountEntity("PROVIDER_CLEARING",
                LedgerAccountType.PROVIDER_CLEARING, AccountClass.ASSET, null);

        assertEquals(LedgerAccountType.WALLET, walletLedger.getType());
        assertEquals(AccountClass.LIABILITY, walletLedger.getAccountClass());
        assertEquals(wallet, walletLedger.getWalletAccount());
        assertEquals(LedgerAccountType.SYSTEM_CLEARING, systemLedger.getType());
        assertEquals(LedgerAccountType.PROVIDER_CLEARING, providerLedger.getType());
        assertFalse(systemLedger.getWalletAccount() != null);
    }

    @Test
    void ledgerAccount_shouldRejectInvalidWalletAssociation() {
        assertThrows(IllegalArgumentException.class,
                () -> new LedgerAccountEntity("WALLET-MISSING", LedgerAccountType.WALLET,
                        AccountClass.LIABILITY, null));
        assertThrows(IllegalArgumentException.class,
                () -> new LedgerAccountEntity("SYSTEM-WALLET", LedgerAccountType.SYSTEM_CLEARING,
                        AccountClass.ASSET, wallet));
    }

    @Test
    void transactionEntity_shouldKeepJournalAssociationNullableAndControlled() {
        TransactionEntity transaction = new TransactionEntity(wallet, null,
                com.chethu.paymentledgerservice.domain.TransactionType.TRANSFER_OUT,
                new BigDecimal("100.00"), new BigDecimal("0.00"));
        JournalEntity journal = new JournalEntity("transfer-1");

        assertEquals(null, transaction.getJournal());
        transaction.associateJournal(journal);
        assertEquals(journal, transaction.getJournal());
        assertThrows(IllegalArgumentException.class, () -> transaction.associateJournal(new JournalEntity("other")));
    }

    @Test
    void enums_shouldUseStringPersistenceMapping() throws Exception {
        Field transactionStatus = TransactionEntity.class.getDeclaredField("status");
        Field entryType = LedgerEntryEntity.class.getDeclaredField("entryType");

        assertEquals(EnumType.STRING, transactionStatus.getAnnotation(Enumerated.class).value());
        assertEquals(EnumType.STRING, entryType.getAnnotation(Enumerated.class).value());
    }
}
