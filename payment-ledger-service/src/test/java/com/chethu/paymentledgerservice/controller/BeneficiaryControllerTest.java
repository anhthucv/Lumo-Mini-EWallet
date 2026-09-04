package com.chethu.paymentledgerservice.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.http.HttpStatus;

import com.chethu.paymentledgerservice.domain.UserRole;
import com.chethu.paymentledgerservice.domain.UserStatus;
import com.chethu.paymentledgerservice.dto.BeneficiaryResponse;
import com.chethu.paymentledgerservice.dto.CreateBeneficiaryRequest;
import com.chethu.paymentledgerservice.dto.UpdateBeneficiaryRequest;
import com.chethu.paymentledgerservice.security.AuthenticatedUserPrincipal;
import com.chethu.paymentledgerservice.service.BeneficiaryService;

@ExtendWith(MockitoExtension.class)
class BeneficiaryControllerTest {
    @Mock
    private BeneficiaryService beneficiaryService;

    private BeneficiaryController controller;
    private AuthenticatedUserPrincipal principal;

    @BeforeEach
    void setUp() {
        controller = new BeneficiaryController(beneficiaryService);
        principal = new AuthenticatedUserPrincipal(42L, "user@example.com", "User", UserRole.USER,
                UserStatus.ACTIVE);
    }

    @Test
    void findAll_shouldUseAuthenticatedPrincipal() {
        when(beneficiaryService.findForCurrentUser(42L)).thenReturn(List.of());

        assertNotNull(controller.findAll(principal).getBody());
        verify(beneficiaryService).findForCurrentUser(42L);
    }

    @Test
    void create_shouldReturnCreatedAndUsePrincipalId() {
        CreateBeneficiaryRequest request = new CreateBeneficiaryRequest();
        BeneficiaryResponse response = new BeneficiaryResponse(9L, "ACC-TARGET", "Recipient", "Target", null, null);
        when(beneficiaryService.createForCurrentUser(42L, request)).thenReturn(response);

        assertEquals(HttpStatus.CREATED, controller.create(principal, request).getStatusCode());
        verify(beneficiaryService).createForCurrentUser(42L, request);
    }

    @Test
    void updateAndDelete_shouldUsePrincipalIdAndBeneficiaryId() {
        UpdateBeneficiaryRequest request = new UpdateBeneficiaryRequest();
        BeneficiaryResponse response = new BeneficiaryResponse(9L, "ACC-TARGET", "Recipient", "New", null, null);
        when(beneficiaryService.updateForCurrentUser(42L, 9L, request)).thenReturn(response);

        assertEquals(HttpStatus.OK, controller.update(principal, 9L, request).getStatusCode());
        assertEquals(HttpStatus.NO_CONTENT, controller.delete(principal, 9L).getStatusCode());
        verify(beneficiaryService).updateForCurrentUser(42L, 9L, request);
        verify(beneficiaryService).deleteForCurrentUser(42L, 9L);
    }
}
