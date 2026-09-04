package com.chethu.paymentledgerservice.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.lang.reflect.Field;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.junit.jupiter.api.extension.ExtendWith;

import com.chethu.paymentledgerservice.domain.UserRole;
import com.chethu.paymentledgerservice.domain.UserStatus;
import com.chethu.paymentledgerservice.dto.CreateBeneficiaryRequest;
import com.chethu.paymentledgerservice.dto.UpdateBeneficiaryRequest;
import com.chethu.paymentledgerservice.entity.AccountEntity;
import com.chethu.paymentledgerservice.entity.BeneficiaryEntity;
import com.chethu.paymentledgerservice.entity.UserEntity;
import com.chethu.paymentledgerservice.exception.AccountNotFoundException;
import com.chethu.paymentledgerservice.exception.BeneficiaryNotFoundException;
import com.chethu.paymentledgerservice.exception.DuplicateBeneficiaryException;
import com.chethu.paymentledgerservice.repository.AccountRepository;
import com.chethu.paymentledgerservice.repository.BeneficiaryRepository;
import com.chethu.paymentledgerservice.repository.UserRepository;

@ExtendWith(MockitoExtension.class)
class BeneficiaryServiceTest {
    @Mock
    private BeneficiaryRepository beneficiaryRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private AccountRepository accountRepository;

    private BeneficiaryService service;
    private UserEntity owner;
    private UserEntity otherOwner;
    private AccountEntity target;

    @BeforeEach
    void setUp() {
        service = new BeneficiaryService(beneficiaryRepository, userRepository, accountRepository);
        owner = user(1L, "owner@example.com");
        otherOwner = user(2L, "other@example.com");
        target = account(20L, "ACC-TARGET");
    }

    @Test
    void create_shouldSaveExistingAccountForAuthenticatedOwner() {
        CreateBeneficiaryRequest request = createRequest("ACC-TARGET", "Main wallet");
        BeneficiaryEntity beneficiary = new BeneficiaryEntity(owner, target, "Main wallet");
        when(userRepository.findById(1L)).thenReturn(Optional.of(owner));
        when(accountRepository.findByAccountNumber("ACC-TARGET")).thenReturn(Optional.of(target));
        when(beneficiaryRepository.existsByOwnerAndBeneficiaryAccount(owner, target)).thenReturn(false);
        when(beneficiaryRepository.save(any(BeneficiaryEntity.class))).thenReturn(beneficiary);

        assertEquals("ACC-TARGET", service.createForCurrentUser(1L, request).accountNumber());
        verify(beneficiaryRepository).save(any(BeneficiaryEntity.class));
    }

    @Test
    void create_shouldRejectUnknownAccount() {
        CreateBeneficiaryRequest request = createRequest("ACC-MISSING", "Missing");
        when(userRepository.findById(1L)).thenReturn(Optional.of(owner));
        when(accountRepository.findByAccountNumber("ACC-MISSING")).thenReturn(Optional.empty());

        assertThrows(AccountNotFoundException.class, () -> service.createForCurrentUser(1L, request));
        verify(beneficiaryRepository, never()).save(any(BeneficiaryEntity.class));
    }

    @Test
    void sameOwner_cannotSaveSameAccountTwice() {
        CreateBeneficiaryRequest request = createRequest("ACC-TARGET", "Target");
        when(userRepository.findById(1L)).thenReturn(Optional.of(owner));
        when(accountRepository.findByAccountNumber("ACC-TARGET")).thenReturn(Optional.of(target));
        when(beneficiaryRepository.existsByOwnerAndBeneficiaryAccount(owner, target)).thenReturn(true);

        assertThrows(DuplicateBeneficiaryException.class, () -> service.createForCurrentUser(1L, request));
        verify(beneficiaryRepository, never()).save(any(BeneficiaryEntity.class));
    }

    @Test
    void differentOwners_maySaveSameAccount() {
        CreateBeneficiaryRequest request = createRequest("ACC-TARGET", "Target");
        when(userRepository.findById(1L)).thenReturn(Optional.of(owner));
        when(userRepository.findById(2L)).thenReturn(Optional.of(otherOwner));
        when(accountRepository.findByAccountNumber("ACC-TARGET")).thenReturn(Optional.of(target));
        when(beneficiaryRepository.existsByOwnerAndBeneficiaryAccount(any(UserEntity.class), any(AccountEntity.class)))
                .thenReturn(false);
        when(beneficiaryRepository.save(any(BeneficiaryEntity.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        service.createForCurrentUser(1L, request);
        service.createForCurrentUser(2L, request);

        verify(beneficiaryRepository, org.mockito.Mockito.times(2)).save(any(BeneficiaryEntity.class));
    }

    @Test
    void find_shouldReturnOnlyOwnerBeneficiaries() {
        BeneficiaryEntity beneficiary = new BeneficiaryEntity(owner, target, "Target");
        when(userRepository.findById(1L)).thenReturn(Optional.of(owner));
        when(beneficiaryRepository.findAllByOwnerOrderByCreatedAtDesc(owner)).thenReturn(List.of(beneficiary));

        assertEquals(List.of("ACC-TARGET"), service.findForCurrentUser(1L).stream()
                .map(response -> response.accountNumber()).toList());
        verify(beneficiaryRepository).findAllByOwnerOrderByCreatedAtDesc(owner);
    }

    @Test
    void update_shouldChangeNicknameForOwner() {
        BeneficiaryEntity beneficiary = new BeneficiaryEntity(owner, target, "Old");
        UpdateBeneficiaryRequest request = new UpdateBeneficiaryRequest();
        request.setNickname("New");
        when(userRepository.findById(1L)).thenReturn(Optional.of(owner));
        when(beneficiaryRepository.findByIdAndOwner(9L, owner)).thenReturn(Optional.of(beneficiary));
        when(beneficiaryRepository.save(beneficiary)).thenReturn(beneficiary);

        assertEquals("New", service.updateForCurrentUser(1L, 9L, request).nickname());
    }

    @Test
    void update_shouldHideBeneficiaryOwnedByAnotherUser() {
        UpdateBeneficiaryRequest request = new UpdateBeneficiaryRequest();
        request.setNickname("New");
        when(userRepository.findById(1L)).thenReturn(Optional.of(owner));
        when(beneficiaryRepository.findByIdAndOwner(9L, owner)).thenReturn(Optional.empty());

        assertThrows(BeneficiaryNotFoundException.class, () -> service.updateForCurrentUser(1L, 9L, request));
        verify(beneficiaryRepository, never()).save(any(BeneficiaryEntity.class));
    }

    @Test
    void delete_shouldRemoveOnlyOwnersBeneficiary() {
        BeneficiaryEntity beneficiary = new BeneficiaryEntity(owner, target, "Target");
        when(userRepository.findById(1L)).thenReturn(Optional.of(owner));
        when(beneficiaryRepository.findByIdAndOwner(9L, owner)).thenReturn(Optional.of(beneficiary));

        service.deleteForCurrentUser(1L, 9L);

        verify(beneficiaryRepository).delete(beneficiary);
        verify(accountRepository, never()).delete(any(AccountEntity.class));
        verify(userRepository, never()).delete(any(UserEntity.class));
    }

    @Test
    void delete_shouldHideBeneficiaryOwnedByAnotherUser() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(owner));
        when(beneficiaryRepository.findByIdAndOwner(9L, owner)).thenReturn(Optional.empty());

        assertThrows(BeneficiaryNotFoundException.class, () -> service.deleteForCurrentUser(1L, 9L));
        verify(beneficiaryRepository, never()).delete(any(BeneficiaryEntity.class));
    }

    @Test
    void entity_shouldDeclareOwnerAccountUniqueConstraint() {
        jakarta.persistence.Table table = BeneficiaryEntity.class.getAnnotation(jakarta.persistence.Table.class);

        assertEquals("beneficiaries", table.name());
        assertEquals("uk_beneficiaries_owner_account", table.uniqueConstraints()[0].name());
        assertEquals(List.of("owner_id", "beneficiary_account_id"),
                List.of(table.uniqueConstraints()[0].columnNames()));
    }

    private CreateBeneficiaryRequest createRequest(String accountNumber, String nickname) {
        CreateBeneficiaryRequest request = new CreateBeneficiaryRequest();
        request.setAccountNumber(accountNumber);
        request.setNickname(nickname);
        return request;
    }

    private UserEntity user(Long id, String email) {
        UserEntity user = new UserEntity(email, "hash", "Owner", UserRole.USER, UserStatus.ACTIVE);
        setId(user, id);
        return user;
    }

    private AccountEntity account(Long id, String number) {
        AccountEntity account = new AccountEntity(number, "Recipient");
        setId(account, id);
        return account;
    }

    private void setId(Object target, Long id) {
        try {
            Field field = target.getClass().getDeclaredField("id");
            field.setAccessible(true);
            field.set(target, id);
        } catch (ReflectiveOperationException ex) {
            throw new IllegalStateException(ex);
        }
    }
}
