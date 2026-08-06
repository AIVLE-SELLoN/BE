package com.aivle.sellon.domain.mypage.dto.response;

import com.aivle.sellon.domain.mypage.entity.MonthlyReportRecipient;

public record RecipientResponse(
        Long recipientId,
        String department,
        String email
) {
    public static RecipientResponse of(MonthlyReportRecipient recipient) {
        return new RecipientResponse(
                recipient.getId(),
                recipient.getDepartment(),
                recipient.getEmail()
        );
    }
}
