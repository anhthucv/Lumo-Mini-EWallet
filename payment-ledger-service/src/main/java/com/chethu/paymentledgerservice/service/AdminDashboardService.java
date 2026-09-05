package com.chethu.paymentledgerservice.service;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.chethu.paymentledgerservice.domain.TransactionStatus;
import com.chethu.paymentledgerservice.domain.TransactionType;
import com.chethu.paymentledgerservice.dto.AdminDashboardResponse;
import com.chethu.paymentledgerservice.repository.AccountRepository;
import com.chethu.paymentledgerservice.repository.TransactionRepository;
import com.chethu.paymentledgerservice.repository.UserRepository;

@Service
public class AdminDashboardService {
    private final UserRepository users;
    private final AccountRepository accounts;
    private final TransactionRepository transactions;
    public AdminDashboardService(UserRepository users, AccountRepository accounts, TransactionRepository transactions) {
        this.users = users; this.accounts = accounts; this.transactions = transactions;
    }
    @Transactional(readOnly = true)
    public AdminDashboardResponse getDashboard() {
        long totalUsers = users.count();
        long active = users.countByStatus(com.chethu.paymentledgerservice.domain.UserStatus.ACTIVE);
        long locked = users.countByStatus(com.chethu.paymentledgerservice.domain.UserStatus.LOCKED);
        long totalTransactions = transactions.count();
        long successful = transactions.countByStatus(TransactionStatus.SUCCESS);
        return new AdminDashboardResponse(new AdminDashboardResponse.UserMetrics(totalUsers, active, locked),
                new AdminDashboardResponse.WalletMetrics(accounts.count()),
                new AdminDashboardResponse.TransactionMetrics(totalTransactions, successful,
                        transactions.sumByTypeAndStatus(TransactionType.DEPOSIT, TransactionStatus.SUCCESS),
                        transactions.sumByTypeAndStatus(TransactionType.TRANSFER_OUT, TransactionStatus.SUCCESS)),
                transactions.findAll(PageRequest.of(0, 5, Sort.by(Sort.Direction.DESC, "createdAt")))
                        .map(com.chethu.paymentledgerservice.dto.AdminTransactionResponse::from).getContent());
    }
}
