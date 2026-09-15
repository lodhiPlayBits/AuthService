package com.lodhi.auth.services;

import static org.mockito.Mockito.verify;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.lodhi.auth.respositories.RefreshTokenRepository;

@ExtendWith(MockitoExtension.class)
class RefreshTokenFamilyServiceTest {

    @Mock
    private RefreshTokenRepository refreshTokenRepository;

    @InjectMocks
    private RefreshTokenFamilyService refreshTokenFamilyService;

    @Test
    void testRevokeFamily() {
        String familyId = "test-family-123";

        refreshTokenFamilyService.revokeFamily(familyId);

        verify(refreshTokenRepository).revokeFamily(familyId);
    }
}
