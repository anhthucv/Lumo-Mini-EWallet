package com.chethu.paymentledgerservice.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.chethu.paymentledgerservice.domain.UserRole;
import com.chethu.paymentledgerservice.domain.AuditAction;
import com.chethu.paymentledgerservice.domain.UserStatus;
import com.chethu.paymentledgerservice.dto.AdminUserResponse;
import com.chethu.paymentledgerservice.entity.UserEntity;
import com.chethu.paymentledgerservice.exception.AdminUserOperationForbiddenException;
import com.chethu.paymentledgerservice.exception.UserNotFoundException;
import com.chethu.paymentledgerservice.repository.AccountRepository;
import com.chethu.paymentledgerservice.repository.UserRepository;

@Service
public class AdminUserService {
    private static final int MAX_PAGE_SIZE = 100;

    private final UserRepository userRepository;
    private final AccountRepository accountRepository;
    private final AuditLogService auditLogService;

    public AdminUserService(UserRepository userRepository, AccountRepository accountRepository, AuditLogService auditLogService) {
        this.userRepository = userRepository;
        this.accountRepository = accountRepository;
        this.auditLogService = auditLogService;
    }

    @Transactional(readOnly = true)
    public Page<AdminUserResponse> listUsers(String search, Pageable pageable) {
        Pageable safePageable = safePageable(pageable);
        Page<UserEntity> users = search == null || search.isBlank()
                ? userRepository.findAll(safePageable)
                : userRepository.findByEmailContainingIgnoreCaseOrFullNameContainingIgnoreCase(
                        search.trim(), search.trim(), safePageable);
        return users.map(user -> AdminUserResponse.from(user,
                accountRepository.findByUserId(user.getId()).orElse(null)));
    }

    @Transactional
    public AdminUserResponse lockUser(Long actorId, Long targetId, String reason) {
        UserEntity target = target(targetId);
        ensureCanChange(actorId, target, true);
        String trimmedReason = reason.trim();
        target.lock(trimmedReason);
        UserEntity saved = userRepository.save(target);
        auditLogService.recordUserStatusChange(actorId, targetId, AuditAction.ADMIN_USER_LOCK, trimmedReason, "ACTIVE -> LOCKED");
        return AdminUserResponse.from(saved,
                accountRepository.findByUserId(target.getId()).orElse(null));
    }

    @Transactional
    public AdminUserResponse unlockUser(Long actorId, Long targetId, String reason) {
        UserEntity target = target(targetId);
        ensureCanChange(actorId, target, false);
        String trimmedReason = reason.trim();
        target.unlock(trimmedReason);
        UserEntity saved = userRepository.save(target);
        auditLogService.recordUserStatusChange(actorId, targetId, AuditAction.ADMIN_USER_UNLOCK, trimmedReason, "LOCKED -> ACTIVE");
        return AdminUserResponse.from(saved,
                accountRepository.findByUserId(target.getId()).orElse(null));
    }

    private UserEntity target(Long targetId) {
        return userRepository.findById(targetId).orElseThrow(() -> new UserNotFoundException(targetId));
    }

    private void ensureCanChange(Long actorId, UserEntity target, boolean locking) {
        if (actorId != null && actorId.equals(target.getId())) {
            throw new AdminUserOperationForbiddenException("Administrators cannot change their own account status.");
        }
        if (locking && target.getRole() == UserRole.ADMIN) {
            throw new AdminUserOperationForbiddenException("Administrators cannot lock another administrator.");
        }
    }

    private Pageable safePageable(Pageable pageable) {
        int page = Math.max(0, pageable.getPageNumber());
        int size = Math.min(MAX_PAGE_SIZE, Math.max(1, pageable.getPageSize()));
        Sort sort = pageable.getSort().isSorted() ? pageable.getSort() : Sort.by(Sort.Direction.DESC, "createdAt");
        return PageRequest.of(page, size, sort);
    }
}
