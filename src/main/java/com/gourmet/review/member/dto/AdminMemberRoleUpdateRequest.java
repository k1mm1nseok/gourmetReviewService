package com.gourmet.review.member.dto;

import com.gourmet.review.domain.enums.MemberRole;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class AdminMemberRoleUpdateRequest {

    @NotNull
    private MemberRole role;
}

