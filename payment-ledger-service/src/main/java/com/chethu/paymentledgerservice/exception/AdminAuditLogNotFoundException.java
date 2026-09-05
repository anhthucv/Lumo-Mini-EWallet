package com.chethu.paymentledgerservice.exception;
public class AdminAuditLogNotFoundException extends RuntimeException {
    public AdminAuditLogNotFoundException(Long id) { super("Audit log with " + id + " not found"); }
}
