package com.chethu.paymentledgerservice.service;

import java.util.List;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.chethu.paymentledgerservice.dto.BeneficiaryResponse;
import com.chethu.paymentledgerservice.dto.CreateBeneficiaryRequest;
import com.chethu.paymentledgerservice.dto.UpdateBeneficiaryRequest;
import com.chethu.paymentledgerservice.entity.AccountEntity;
import com.chethu.paymentledgerservice.entity.BeneficiaryEntity;
import com.chethu.paymentledgerservice.entity.UserEntity;
import com.chethu.paymentledgerservice.exception.AccountNotFoundException;
import com.chethu.paymentledgerservice.exception.BeneficiaryNotFoundException;
import com.chethu.paymentledgerservice.exception.DuplicateBeneficiaryException;
import com.chethu.paymentledgerservice.exception.UserNotFoundException;
import com.chethu.paymentledgerservice.repository.AccountRepository;
import com.chethu.paymentledgerservice.repository.BeneficiaryRepository;
import com.chethu.paymentledgerservice.repository.UserRepository;

@Service
public class BeneficiaryService {
    private final BeneficiaryRepository beneficiaryRepository;
    private final UserRepository userRepository;
    private final AccountRepository accountRepository;

    public BeneficiaryService(BeneficiaryRepository beneficiaryRepository, UserRepository userRepository,
            AccountRepository accountRepository) {
        this.beneficiaryRepository = beneficiaryRepository;
        this.userRepository = userRepository;
        this.accountRepository = accountRepository;
    }

    public List<BeneficiaryResponse> findForCurrentUser(Long userId) {
        UserEntity owner = findOwner(userId);
        return beneficiaryRepository.findAllByOwnerOrderByCreatedAtDesc(owner).stream()
                .map(BeneficiaryResponse::from)
                .toList();
    }

    @Transactional
    public BeneficiaryResponse createForCurrentUser(Long userId, CreateBeneficiaryRequest request) {
        UserEntity owner = findOwner(userId);
        AccountEntity account = accountRepository.findByAccountNumber(request.getAccountNumber())
                .orElseThrow(() -> new AccountNotFoundException(request.getAccountNumber()));
        if (beneficiaryRepository.existsByOwnerAndBeneficiaryAccount(owner, account)) {
            throw new DuplicateBeneficiaryException();
        }

        try {
            return BeneficiaryResponse.from(
                    beneficiaryRepository.save(new BeneficiaryEntity(owner, account, request.getNickname())));
        } catch (DataIntegrityViolationException ex) {
            throw new DuplicateBeneficiaryException();
        }
    }

    @Transactional
    public BeneficiaryResponse updateForCurrentUser(Long userId, Long beneficiaryId,
            UpdateBeneficiaryRequest request) {
        UserEntity owner = findOwner(userId);
        BeneficiaryEntity beneficiary = beneficiaryRepository.findByIdAndOwner(beneficiaryId, owner)
                .orElseThrow(() -> new BeneficiaryNotFoundException(beneficiaryId));
        beneficiary.changeNickname(request.getNickname());
        return BeneficiaryResponse.from(beneficiaryRepository.save(beneficiary));
    }

    @Transactional
    public void deleteForCurrentUser(Long userId, Long beneficiaryId) {
        UserEntity owner = findOwner(userId);
        BeneficiaryEntity beneficiary = beneficiaryRepository.findByIdAndOwner(beneficiaryId, owner)
                .orElseThrow(() -> new BeneficiaryNotFoundException(beneficiaryId));
        beneficiaryRepository.delete(beneficiary);
    }

    private UserEntity findOwner(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException(userId));
    }
}
