package com.gourmet.review.member.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gourmet.review.domain.enums.MemberRole;
import com.gourmet.review.domain.enums.MemberTier;
import com.gourmet.review.member.service.MemberService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class AdminMemberControllerTest {

    MockMvc mockMvc;

    MemberService memberService;

    ObjectMapper objectMapper;

    @BeforeEach
    public void setUp() {
        memberService = Mockito.mock(MemberService.class);
        objectMapper = new ObjectMapper();

        AdminMemberController controller = new AdminMemberController(memberService);
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .build();
    }

    @Test
    public void patch_tier_delegates_to_service() throws Exception {
        Long memberId = 1L;
        String body = "{\"tier\":\"BLACK\"}";

        when(memberService.adminUpdateMemberTier(eq(memberId), eq(MemberTier.BLACK)))
                .thenReturn(null);

        mockMvc.perform(patch("/admin/members/{memberId}/tier", memberId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk());

        verify(memberService).adminUpdateMemberTier(eq(memberId), eq(MemberTier.BLACK));
    }

    @Test
    public void patch_role_delegates_to_service() throws Exception {
        Long memberId = 1L;
        String body = "{\"role\":\"ADMIN\"}";

        when(memberService.adminUpdateMemberRole(eq(memberId), eq(MemberRole.ADMIN)))
                .thenReturn(null);

        mockMvc.perform(patch("/admin/members/{memberId}/role", memberId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk());

        verify(memberService).adminUpdateMemberRole(eq(memberId), eq(MemberRole.ADMIN));
    }

    @Test
    public void patch_tier_without_body_returns_400() throws Exception {
        Long memberId = 1L;

        mockMvc.perform(patch("/admin/members/{memberId}/tier", memberId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    public void patch_role_without_body_returns_400() throws Exception {
        Long memberId = 1L;

        mockMvc.perform(patch("/admin/members/{memberId}/role", memberId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());
    }
}
