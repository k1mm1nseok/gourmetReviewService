package com.gourmet.review.member.controller;

import com.gourmet.review.common.dto.ApiResponse;
import com.gourmet.review.member.dto.AdminMemberRoleUpdateRequest;
import com.gourmet.review.member.dto.AdminMemberTierUpdateRequest;
import com.gourmet.review.member.dto.MemberResponse;
import com.gourmet.review.member.service.MemberService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/admin/members")
@RequiredArgsConstructor
public class AdminMemberController {

    private final MemberService memberService;

    @PatchMapping("/{memberId}/tier")
    public ApiResponse<MemberResponse> updateTier(@PathVariable Long memberId,
                                                  @RequestBody @Valid AdminMemberTierUpdateRequest request) {
        return ApiResponse.success(memberService.adminUpdateMemberTier(memberId, request.getTier()));
    }

    @PatchMapping("/{memberId}/role")
    public ApiResponse<MemberResponse> updateRole(@PathVariable Long memberId,
                                                  @RequestBody @Valid AdminMemberRoleUpdateRequest request) {
        return ApiResponse.success(memberService.adminUpdateMemberRole(memberId, request.getRole()));
    }
}

