package com.n.devopsmonitoringsaas.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.n.devopsmonitoringsaas.entity.UserRole;
import com.n.devopsmonitoringsaas.repository.UserRepository;
import com.n.devopsmonitoringsaas.security.JwtUtil;
import com.n.devopsmonitoringsaas.support.AbstractIntegrationTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@DisplayName("AuthController")
class AuthControllerTest extends AbstractIntegrationTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private UserRepository userRepository;

    @Test
    @DisplayName("POST /auth/register — returns 201 and JWT when registration succeeds")
    void register_success() throws Exception {
        mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "email": "newuser-register@example.com",
                                  "password": "SecretPass123!",
                                  "companyName": "Acme Inc"
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.token").isNotEmpty());
    }

    @Test
    @DisplayName("POST /auth/register — returns 401 when email is already registered")
    void register_duplicateEmail() throws Exception {
        String body = """
                {
                  "email": "dup-register@example.com",
                  "password": "SecretPass123!",
                  "companyName": "First Co"
                }
                """;
        mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body.replace("First Co", "Second Co")))
                .andExpect(status().isUnauthorized())
                .andExpect(result -> assertThat(result.getResponse().getContentAsString())
                        .contains("Email already registered"));
    }

    @Test
    @DisplayName("POST /auth/login — returns 200 and JWT when credentials are valid")
    void login_success() throws Exception {
        mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "email": "login-ok@example.com",
                                  "password": "CorrectPass123!",
                                  "companyName": "Login Co"
                                }
                                """))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "email": "login-ok@example.com",
                                  "password": "CorrectPass123!"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isNotEmpty());
    }

    @Test
    @DisplayName("POST /auth/login — returns 401 when password is wrong")
    void login_wrongPassword() throws Exception {
        mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "email": "login-wrong@example.com",
                                  "password": "RightPass123!",
                                  "companyName": "Co"
                                }
                                """))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "email": "login-wrong@example.com",
                                  "password": "WrongPass999!"
                                }
                                """))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("POST /auth/login — returns 401 when user does not exist")
    void login_userNotFound() throws Exception {
        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "email": "nobody-exists@example.com",
                                  "password": "Whatever123!"
                                }
                                """))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("POST /auth/invite — returns 201 when owner invites a user")
    void invite_success() throws Exception {
        mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "email": "owner-invite@example.com",
                                  "password": "SecretPass123!",
                                  "companyName": "Invite Co"
                                }
                                """))
                .andExpect(status().isCreated());

        var owner = userRepository.findByEmail("owner-invite@example.com").orElseThrow();
        String token = jwtUtil.generateToken(owner.getId(), owner.getTenant().getId(), UserRole.OWNER);

        mockMvc.perform(post("/auth/invite")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(String.format("""
                                {
                                  "tenantId": %d,
                                  "email": "invitee-success@example.com",
                                  "role": "MEMBER"
                                }
                                """, owner.getTenant().getId())))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.invitationToken").isNotEmpty());
    }

    @Test
    @DisplayName("POST /auth/invite — returns 403 when not authenticated")
    void invite_unauthorized() throws Exception {
        mockMvc.perform(post("/auth/invite")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "tenantId": 1,
                                  "email": "x@example.com",
                                  "role": "MEMBER"
                                }
                                """))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("POST /auth/invite — returns 403 when MEMBER tries to invite OWNER")
    void invite_invalidRole_memberCannotInviteOwner() throws Exception {
        mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "email": "owner-for-member@example.com",
                                  "password": "SecretPass123!",
                                  "companyName": "M Co"
                                }
                                """))
                .andExpect(status().isCreated());

        var owner = userRepository.findByEmail("owner-for-member@example.com").orElseThrow();
        String ownerToken = jwtUtil.generateToken(owner.getId(), owner.getTenant().getId(), UserRole.OWNER);

        mockMvc.perform(post("/auth/invite")
                        .header("Authorization", "Bearer " + ownerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(String.format("""
                                {
                                  "tenantId": %d,
                                  "email": "member-user@example.com",
                                  "role": "MEMBER"
                                }
                                """, owner.getTenant().getId())))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/auth/accept-invite")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new AcceptInviteBody(
                                userRepository.findByEmail("member-user@example.com")
                                        .orElseThrow()
                                        .getInvitationToken(),
                                "MemberPass123!"))))
                .andExpect(status().isOk());

        var member = userRepository.findByEmail("member-user@example.com").orElseThrow();
        assertThat(member.getIsActive()).isTrue();

        String memberToken = jwtUtil.generateToken(member.getId(), member.getTenant().getId(), UserRole.MEMBER);

        mockMvc.perform(post("/auth/invite")
                        .header("Authorization", "Bearer " + memberToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(String.format("""
                                {
                                  "tenantId": %d,
                                  "email": "should-fail-owner@example.com",
                                  "role": "OWNER"
                                }
                                """, member.getTenant().getId())))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("POST /auth/accept-invite — returns 200 and JWT when token is valid")
    void acceptInvite_success() throws Exception {
        mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "email": "owner-accept@example.com",
                                  "password": "SecretPass123!",
                                  "companyName": "Accept Co"
                                }
                                """))
                .andExpect(status().isCreated());

        var owner = userRepository.findByEmail("owner-accept@example.com").orElseThrow();
        String ownerToken = jwtUtil.generateToken(owner.getId(), owner.getTenant().getId(), UserRole.OWNER);

        mockMvc.perform(post("/auth/invite")
                        .header("Authorization", "Bearer " + ownerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(String.format("""
                                {
                                  "tenantId": %d,
                                  "email": "pending-invite@example.com",
                                  "role": "MEMBER"
                                }
                                """, owner.getTenant().getId())))
                .andExpect(status().isCreated());

        var pending = userRepository.findByEmail("pending-invite@example.com").orElseThrow();

        mockMvc.perform(post("/auth/accept-invite")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(String.format("""
                                {
                                  "token": "%s",
                                  "password": "NewMemberPass123!"
                                }
                                """, pending.getInvitationToken())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isNotEmpty());
    }

    @Test
    @DisplayName("POST /auth/accept-invite — returns 400 when token is invalid")
    void acceptInvite_invalidToken() throws Exception {
        mockMvc.perform(post("/auth/accept-invite")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "token": "not-a-real-invitation-token",
                                  "password": "SomePass123!"
                                }
                                """))
                .andExpect(status().isBadRequest());
    }

    private record AcceptInviteBody(String token, String password) {}
}
