package com.lodhi.auth.services;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.modelmapper.ModelMapper;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import com.lodhi.auth.model.User;
import com.lodhi.auth.respositories.RefreshTokenRepository;
import com.lodhi.auth.respositories.UserRepository;

@ExtendWith(MockitoExtension.class)
class UserServiceImplPostCommitPublishTest {

    @Mock private UserRepository userRepository;
    @Mock private ModelMapper modelMapper;
    @Mock private RoleServiceImpl roleService;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private BCryptService bcryptService;
    @Mock private RefreshTokenRepository refreshTokenRepository;
    @Mock private AccountEventProducer accountEventProducer;

    private UserServiceImpl userService;

    @BeforeEach
    void setUp() {
        userService = new UserServiceImpl(userRepository, modelMapper, roleService,
                passwordEncoder, bcryptService, refreshTokenRepository, accountEventProducer);
        User user = new User();
        user.setEmail("user@example.com");
        user.setEnabled(true);
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
    }

    @AfterEach
    void tearDown() {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.clearSynchronization();
        }
    }

    @Test
    void updateUserStatus_Disable_PublishesOnlyAfterCommit() {
        TransactionSynchronizationManager.initSynchronization();
        try {
            userService.updateUserStatus(1L, false);

            verifyNoInteractions(accountEventProducer);
            verify(refreshTokenRepository).revokeAllForUser(1L);

            commit();

            verify(accountEventProducer).publishAccountDisabled(
                    1L, "user@example.com",
                    "Your account has been disabled by an administrator. Please contact support.");
        } finally {
            TransactionSynchronizationManager.clearSynchronization();
        }
    }

    @Test
    void updateUserStatus_Enable_PublishesOnlyAfterCommit() {
        TransactionSynchronizationManager.initSynchronization();
        try {
            userService.updateUserStatus(1L, true);

            verifyNoInteractions(accountEventProducer);

            commit();

            verify(accountEventProducer).publishAccountEnabled(1L, "user@example.com");
        } finally {
            TransactionSynchronizationManager.clearSynchronization();
        }
    }

    @Test
    void updateUserStatus_Disable_RollbackDoesNotPublish() {
        TransactionSynchronizationManager.initSynchronization();
        try {
            userService.updateUserStatus(1L, false);

            rollback();
        } finally {
            TransactionSynchronizationManager.clearSynchronization();
        }

        verifyNoInteractions(accountEventProducer);
    }

    @Test
    void updateUserStatus_OutsideTransaction_PublishesImmediately() {
        userService.updateUserStatus(1L, true);

        verify(accountEventProducer).publishAccountEnabled(1L, "user@example.com");
    }

    private void commit() {
        TransactionSynchronizationManager.getSynchronizations()
                .forEach(TransactionSynchronization::afterCommit);
    }

    private void rollback() {
        TransactionSynchronizationManager.getSynchronizations()
                .forEach(sync -> sync.afterCompletion(TransactionSynchronization.STATUS_ROLLED_BACK));
    }
}
