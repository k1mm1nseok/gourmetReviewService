package com.gourmet.review.member.dto;

import com.gourmet.review.domain.enums.MemberTier;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class AdminMemberTierUpdateRequest {

    @NotNull
    private MemberTier tier;
}

