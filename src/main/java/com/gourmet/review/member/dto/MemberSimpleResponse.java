package com.gourmet.review.member.dto;

import com.gourmet.review.domain.enums.MemberTier;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MemberSimpleResponse {

    private Long id;
    private String nickname;
    private MemberTier tier;
}
