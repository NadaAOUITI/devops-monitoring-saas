package com.n.devopsmonitoringsaas.service;

import com.n.devopsmonitoringsaas.entity.Tenant;
import com.n.devopsmonitoringsaas.entity.User;
import com.n.devopsmonitoringsaas.entity.UserRole;
import com.n.devopsmonitoringsaas.exception.OwnerOperationException;
import com.n.devopsmonitoringsaas.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private UserService userService;

    private Tenant tenant;
    private User owner;
    private User member;

    @BeforeEach
    void setUp() {
        tenant = Tenant.builder().id(1L).name("Acme").build();
        owner = User.builder()
                .id(10L)
                .email("owner@acme.com")
                .role(UserRole.OWNER)
                .tenantId(1L)
                .tenant(tenant)
                .build();
        member = User.builder()
                .id(20L)
                .email("member@acme.com")
                .role(UserRole.MEMBER)
                .tenantId(1L)
                .tenant(tenant)
                .build();
    }

    @Nested
    @DisplayName("findByTenantId")
    class FindByTenantId {

        @Test
        @DisplayName("returns all users for tenant")
        void returnsUsersForTenant() {
            when(userRepository.findByTenantId(1L)).thenReturn(List.of(owner, member));

            var result = userService.findByTenantId(1L);

            assertThat(result).hasSize(2).containsExactly(owner, member);
            verify(userRepository).findByTenantId(1L);
        }
    }

    @Nested
    @DisplayName("deleteUser")
    class DeleteUser {

        @Test
        @DisplayName("throws 400 when trying to delete OWNER")
        void throwsWhenDeletingOwner() {
            when(userRepository.findById(10L)).thenReturn(Optional.of(owner));

            assertThatThrownBy(() -> userService.deleteUser(1L, 10L))
                    .isInstanceOf(OwnerOperationException.class)
                    .hasMessage("Cannot delete the tenant owner");

            verify(userRepository).findById(10L);
            verify(userRepository, never()).delete(any());
        }

        @Test
        @DisplayName("throws 404 when user not found")
        void throwsWhenUserNotFound() {
            when(userRepository.findById(99L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> userService.deleteUser(1L, 99L))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("User not found: 99");

            verify(userRepository).findById(99L);
            verify(userRepository, never()).delete(any());
        }

        @Test
        @DisplayName("throws 404 when user belongs to different tenant")
        void throwsWhenUserDifferentTenant() {
            User otherTenantUser = User.builder().id(30L).tenantId(2L).role(UserRole.MEMBER).build();
            when(userRepository.findById(30L)).thenReturn(Optional.of(otherTenantUser));

            assertThatThrownBy(() -> userService.deleteUser(1L, 30L))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("User not found: 30");

            verify(userRepository, never()).delete(any());
        }

        @Test
        @DisplayName("deletes non-owner user successfully")
        void deletesMemberSuccessfully() {
            when(userRepository.findById(20L)).thenReturn(Optional.of(member));

            userService.deleteUser(1L, 20L);

            verify(userRepository).findById(20L);
            verify(userRepository).delete(member);
        }
    }

    @Nested
    @DisplayName("updateRole")
    class UpdateRole {

        @Test
        @DisplayName("throws 400 when trying to change OWNER role")
        void throwsWhenChangingOwnerRole() {
            when(userRepository.findById(10L)).thenReturn(Optional.of(owner));

            assertThatThrownBy(() -> userService.updateRole(1L, 10L, UserRole.ADMIN))
                    .isInstanceOf(OwnerOperationException.class)
                    .hasMessage("Cannot change the owner's role");

            verify(userRepository).findById(10L);
            verify(userRepository, never()).save(any());
        }

        @Test
        @DisplayName("updates role successfully for non-owner")
        void updatesRoleSuccessfully() {
            when(userRepository.findById(20L)).thenReturn(Optional.of(member));
            when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

            var result = userService.updateRole(1L, 20L, UserRole.ADMIN);

            assertThat(result.getRole()).isEqualTo(UserRole.ADMIN);
            verify(userRepository).findById(20L);
            verify(userRepository).save(member);
        }

        @Test
        @DisplayName("throws 404 when user not found")
        void throwsWhenUserNotFound() {
            when(userRepository.findById(99L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> userService.updateRole(1L, 99L, UserRole.ADMIN))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("User not found: 99");
        }
    }
}
