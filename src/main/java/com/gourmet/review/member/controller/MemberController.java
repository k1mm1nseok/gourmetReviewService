package com.gourmet.review.member.controller;

import com.gourmet.review.common.dto.ApiResponse;
import com.gourmet.review.member.dto.MemberLoginRequest;
import com.gourmet.review.member.dto.MemberLoginResponse;
import com.gourmet.review.member.dto.MemberProfileResponse;
import com.gourmet.review.member.dto.MemberProfileUpdateRequest;
import com.gourmet.review.member.dto.MemberRegisterRequest;
import com.gourmet.review.member.dto.MemberResponse;
import com.gourmet.review.member.dto.MemberSimpleResponse;
import com.gourmet.review.member.service.MemberService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/members")
@RequiredArgsConstructor
public class MemberController {

    private final MemberService memberService;

    @PostMapping("/register")
    public ApiResponse<MemberResponse> register(@RequestBody @Valid MemberRegisterRequest request) {
        return ApiResponse.success(memberService.registerMember(request));
    }

    @PostMapping("/login")
    public ApiResponse<MemberLoginResponse> login(@RequestBody @Valid MemberLoginRequest request) {
        return ApiResponse.success(memberService.login(request));
    }

    @GetMapping("/me")
    public ApiResponse<MemberProfileResponse> getMyProfile() {
        return ApiResponse.success(memberService.getMyProfile());
    }

    @PatchMapping("/me")
    public ApiResponse<MemberResponse> updateMyProfile(@RequestBody @Valid MemberProfileUpdateRequest request) {
        return ApiResponse.success(memberService.updateMyProfile(request));
    }

    @PostMapping("/{memberId}/follow")
    public ApiResponse<Void> follow(@PathVariable Long memberId) {
        memberService.follow(memberId);
        return ApiResponse.success(null);
    }

    @DeleteMapping("/{memberId}/follow")
    public ApiResponse<Void> unfollow(@PathVariable Long memberId) {
        memberService.unfollow(memberId);
        return ApiResponse.success(null);
    }

    @GetMapping("/{memberId}/followers")
    public ApiResponse<Page<MemberSimpleResponse>> getFollowers(@PathVariable Long memberId,
                                                                @RequestParam(defaultValue = "0") int page,
                                                                @RequestParam(defaultValue = "20") int size) {
        Pageable pageable = PageRequest.of(page, size);
        return ApiResponse.success(memberService.getFollowers(memberId, pageable));
    }
}
