package com.chethu.paymentledgerservice.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.HashSet;

import org.junit.jupiter.api.Test;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.chethu.paymentledgerservice.domain.EmailVerificationStatus;
import com.chethu.paymentledgerservice.dto.VerificationCodeResponse;
import com.chethu.paymentledgerservice.entity.EmailVerificationEntity;
import com.chethu.paymentledgerservice.exception.DuplicateEmailException;
import com.chethu.paymentledgerservice.exception.VerificationCodeResendTooSoonException;
import com.chethu.paymentledgerservice.repository.EmailVerificationRepository;
import com.chethu.paymentledgerservice.repository.UserRepository;

public class EmailVerificationServiceTest {

    @Test
    void sendCode_success() {
        TestFixture fixture = TestFixture.create(false, List.of(), "004271");
        LocalDateTime before = LocalDateTime.now();

        VerificationCodeResponse response = fixture.service.sendRegistrationCode("  User@Example.com ");
        LocalDateTime after = LocalDateTime.now();

        assertEquals("Verification code sent", response.getMessage());
        assertEquals(1, fixture.verificationState.savedEntities.size());
        EmailVerificationEntity saved = fixture.verificationState.savedEntities.get(0);
        assertEquals("user@example.com", saved.getEmail());
        assertEquals(EmailVerificationStatus.ACTIVE, saved.getStatus());
        assertEquals(0, saved.getFailedAttempts());
        assertEquals("hash::004271", saved.getCodeHash());
        assertEquals("user@example.com", fixture.emailService.lastEmail);
        assertEquals("004271", fixture.emailService.lastCode);
        assertEquals(1, fixture.passwordEncoder.encodeCalls);
        assertTrue(saved.getExpiresAt().isAfter(before.plusMinutes(4)));
        assertTrue(saved.getExpiresAt().isBefore(after.plusMinutes(6)));
    }

    @Test
    void sendCode_duplicateEmail() {
        TestFixture fixture = TestFixture.create(true, List.of(), "004271");

        DuplicateEmailException ex = assertThrows(DuplicateEmailException.class,
                () -> fixture.service.sendRegistrationCode("user@example.com"));

        assertInstanceOf(DuplicateEmailException.class, ex);
        assertTrue(fixture.verificationState.savedEntities.isEmpty());
        assertEquals(0, fixture.emailService.sendCalls);
        assertEquals(0, fixture.passwordEncoder.encodeCalls);
    }

    @Test
    void sendCode_resendTooSoon() {
        TestFixture fixture = TestFixture.create(false, List.of(), "004271");
        EmailVerificationEntity newest = new EmailVerificationEntity(
                "user@example.com",
                "hash::old",
                EmailVerificationStatus.ACTIVE,
                0,
                LocalDateTime.now().plusMinutes(4));
        fixture.setCreatedAt(newest, LocalDateTime.now().minusSeconds(30));
        fixture.verificationState.records.add(newest);

        assertThrows(VerificationCodeResendTooSoonException.class,
                () -> fixture.service.sendRegistrationCode("user@example.com"));

        assertTrue(fixture.verificationState.savedEntities.isEmpty());
        assertEquals(0, fixture.emailService.sendCalls);
        assertEquals(0, fixture.passwordEncoder.encodeCalls);
        assertEquals(EmailVerificationStatus.ACTIVE, newest.getStatus());
    }

    @Test
    void sendCode_afterCooldown() {
        TestFixture fixture = TestFixture.create(false, List.of(), "004271");
        EmailVerificationEntity active = new EmailVerificationEntity(
                "user@example.com",
                "hash::old",
                EmailVerificationStatus.ACTIVE,
                0,
                LocalDateTime.now().plusMinutes(4));
        fixture.setCreatedAt(active, LocalDateTime.now().minusSeconds(61));
        fixture.verificationState.records.add(active);

        VerificationCodeResponse response = fixture.service.sendRegistrationCode("user@example.com");

        assertEquals("Verification code sent", response.getMessage());
        assertEquals(2, fixture.verificationState.savedEntities.size());
        assertEquals(EmailVerificationStatus.INVALIDATED, fixture.verificationState.savedEntities.get(0).getStatus());

        EmailVerificationEntity newRecord = fixture.verificationState.savedEntities.get(1);
        assertEquals("user@example.com", newRecord.getEmail());
        assertEquals(EmailVerificationStatus.ACTIVE, newRecord.getStatus());
        assertEquals("hash::004271", newRecord.getCodeHash());
        assertEquals("user@example.com", fixture.emailService.lastEmail);
        assertEquals("004271", fixture.emailService.lastCode);
        assertEquals(1, fixture.passwordEncoder.encodeCalls);
        assertEquals(EmailVerificationStatus.INVALIDATED, active.getStatus());
    }

    @Test
    void verificationCode_isNotStoredRaw() {
        TestFixture fixture = TestFixture.create(false, List.of(), "123456");

        fixture.service.sendRegistrationCode("user@example.com");

        EmailVerificationEntity saved = fixture.verificationState.savedEntities.get(0);
        assertFalse(saved.getCodeHash().equals("123456"));
        assertEquals("hash::123456", saved.getCodeHash());
        assertEquals(1, fixture.passwordEncoder.encodeCalls);
    }

    private static final class TestFixture {
        private final EmailVerificationService service;
        private final RecordingPasswordEncoder passwordEncoder;
        private final RecordingEmailService emailService;
        private final VerificationRepositoryState verificationState;

        private TestFixture(EmailVerificationService service, RecordingPasswordEncoder passwordEncoder,
                RecordingEmailService emailService, VerificationRepositoryState verificationState) {
            this.service = service;
            this.passwordEncoder = passwordEncoder;
            this.emailService = emailService;
            this.verificationState = verificationState;
        }

        private static TestFixture create(boolean userExists, List<EmailVerificationEntity> existingRecords,
                String generatedCode) {
            VerificationRepositoryState verificationState = new VerificationRepositoryState();
            verificationState.records.addAll(existingRecords);

            UserRepository userRepository = createUserRepository(userExists);
            EmailVerificationRepository emailVerificationRepository = createEmailVerificationRepository(verificationState);
            VerificationCodeGenerator verificationCodeGenerator = new VerificationCodeGenerator() {
                @Override
                public String generateCode() {
                    return generatedCode;
                }
            };
            RecordingPasswordEncoder passwordEncoder = new RecordingPasswordEncoder();
            RecordingEmailService emailService = new RecordingEmailService();

            EmailVerificationService service = new EmailVerificationService(userRepository, emailVerificationRepository,
                    verificationCodeGenerator, passwordEncoder, emailService);
            return new TestFixture(service, passwordEncoder, emailService, verificationState);
        }

        private void setCreatedAt(EmailVerificationEntity entity, LocalDateTime createdAt) {
            try {
                Field field = EmailVerificationEntity.class.getDeclaredField("createdAt");
                field.setAccessible(true);
                field.set(entity, createdAt);
            } catch (ReflectiveOperationException ex) {
                throw new IllegalStateException(ex);
            }
        }
    }

    private static final class VerificationRepositoryState {
        private final List<EmailVerificationEntity> records = new ArrayList<>();
        private final List<EmailVerificationEntity> savedEntities = new ArrayList<>();
    }

    private static final class RecordingPasswordEncoder implements PasswordEncoder {
        private int encodeCalls;

        @Override
        public String encode(CharSequence rawPassword) {
            encodeCalls++;
            return "hash::" + rawPassword;
        }

        @Override
        public boolean matches(CharSequence rawPassword, String encodedPassword) {
            return encodedPassword.equals("hash::" + rawPassword);
        }
    }

    private static final class RecordingEmailService extends EmailService {
        private String lastEmail;
        private String lastCode;
        private int sendCalls;

        private RecordingEmailService() {
            super(dummyMailSender());
        }

        @Override
        public void sendVerificationCode(String email, String code) {
            sendCalls++;
            lastEmail = email;
            lastCode = code;
        }
    }

    private static UserRepository createUserRepository(boolean userExists) {
        Set<String> existingEmails = new HashSet<>();
        if (userExists) {
            existingEmails.add("user@example.com");
        }
        InvocationHandler handler = (proxy, method, args) -> {
            String methodName = method.getName();
            if ("existsByEmailIgnoreCase".equals(methodName)) {
                return existingEmails.contains(normalize((String) args[0]));
            }
            if ("findByEmailIgnoreCase".equals(methodName)) {
                return Optional.empty();
            }
            return defaultValue(method.getReturnType());
        };
        return (UserRepository) Proxy.newProxyInstance(UserRepository.class.getClassLoader(),
                new Class<?>[] { UserRepository.class }, handler);
    }

    private static EmailVerificationRepository createEmailVerificationRepository(VerificationRepositoryState state) {
        InvocationHandler handler = (proxy, method, args) -> {
            String methodName = method.getName();
            if ("save".equals(methodName)) {
                EmailVerificationEntity entity = (EmailVerificationEntity) args[0];
                state.savedEntities.add(entity);
                if (!state.records.contains(entity)) {
                    state.records.add(entity);
                }
                return entity;
            }
            if ("findTopByEmailIgnoreCaseOrderByCreatedAtDesc".equals(methodName)) {
                String email = normalize((String) args[0]);
                return state.records.stream()
                        .filter(entity -> entity.getEmail() != null && entity.getEmail().equalsIgnoreCase(email))
                        .max(Comparator.comparing(EmailVerificationEntity::getCreatedAt,
                                Comparator.nullsLast(Comparator.naturalOrder())));
            }
            if ("findTopByEmailIgnoreCaseAndStatusOrderByCreatedAtDesc".equals(methodName)) {
                String email = normalize((String) args[0]);
                EmailVerificationStatus status = (EmailVerificationStatus) args[1];
                return state.records.stream()
                        .filter(entity -> entity.getEmail() != null && entity.getEmail().equalsIgnoreCase(email))
                        .filter(entity -> entity.getStatus() == status)
                        .max(Comparator.comparing(EmailVerificationEntity::getCreatedAt,
                                Comparator.nullsLast(Comparator.naturalOrder())));
            }
            return defaultValue(method.getReturnType());
        };
        return (EmailVerificationRepository) Proxy.newProxyInstance(EmailVerificationRepository.class.getClassLoader(),
                new Class<?>[] { EmailVerificationRepository.class }, handler);
    }

    private static JavaMailSender dummyMailSender() {
        InvocationHandler handler = (proxy, method, args) -> defaultValue(method.getReturnType());
        return (JavaMailSender) Proxy.newProxyInstance(JavaMailSender.class.getClassLoader(),
                new Class<?>[] { JavaMailSender.class }, handler);
    }

    private static Object defaultValue(Class<?> returnType) {
        if (returnType.equals(Boolean.TYPE)) {
            return false;
        }
        if (returnType.equals(Integer.TYPE)) {
            return 0;
        }
        if (returnType.equals(Long.TYPE)) {
            return 0L;
        }
        if (returnType.equals(Double.TYPE)) {
            return 0d;
        }
        if (returnType.equals(Float.TYPE)) {
            return 0f;
        }
        if (returnType.equals(Short.TYPE)) {
            return (short) 0;
        }
        if (returnType.equals(Byte.TYPE)) {
            return (byte) 0;
        }
        if (returnType.equals(Character.TYPE)) {
            return '\0';
        }
        if (Optional.class.isAssignableFrom(returnType)) {
            return Optional.empty();
        }
        if (List.class.isAssignableFrom(returnType)) {
            return List.of();
        }
        return null;
    }

    private static String normalize(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }
}
