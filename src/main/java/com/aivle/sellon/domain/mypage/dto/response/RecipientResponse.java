package com.aivle.sellon.domain.mypage.dto.response;

import com.aivle.sellon.domain.mypage.entity.MonthlyReportRecipient;
import com.aivle.sellon.domain.mypage.enums.RecipientDepartment;

public record RecipientResponse(
        Long recipientId,
        RecipientDepartment department,
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
