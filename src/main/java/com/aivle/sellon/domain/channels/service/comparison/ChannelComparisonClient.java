package com.aivle.sellon.domain.channels.service.comparison;

import com.aivle.sellon.domain.channels.enums.InquiryType;

import java.util.List;

/**
 * 채널 비교분석 결과(Agent2)를 백엔드가 직접 호출해서 받아오는 경계.
 * TODO: 실제 Agent2 API 스펙 확정되면 RestClient 기반 구현으로 교체.
 * 지금 이 인터페이스의 필드 구성이 곧 "우리가 Agent2에 제안하는 응답 포맷"이다.
 */
public interface ChannelComparisonClient {

    ComparisonResult fetchComparisonResult(Long usersChannelKey, String channelType);

    record ComparisonResult(
            Integer totalInquiryCount,
            Double totalInquiryChangeRate,
            String totalInquiryComment,
            Double inquiryRatePerOrder,
            String inquiryRateComment,
            Double avgRating,
            String avgRatingComment,
            Double reviewRatePerOrder,
            String reviewRateComment,
            Double positiveRatio,
            Double neutralRatio,
            Double negativeRatio,
            String sentimentComment,
            String aspectComment,
            List<InquiryTypeCount> inquiryTypeCounts,
            List<MonthlyCount> monthlyCounts,
            List<String> insights
    ) {
    }

    record InquiryTypeCount(InquiryType inquireType, Integer count) {
    }

    record MonthlyCount(String yearMonth, Integer count) {
    }
}
