package com.aivle.sellon.domain.inquiries.service;

import com.aivle.sellon.domain.inquiries.dto.*;
import com.aivle.sellon.domain.inquiries.entity.CsInquiry;
import com.aivle.sellon.domain.inquiries.enums.InquiryStatus;
import com.aivle.sellon.domain.inquiries.exception.CsInquiryNotFoundException;
import com.aivle.sellon.domain.inquiries.repository.CsInquiryRepository;
import com.aivle.sellon.domain.channels.entity.UsersChannel;
import com.aivle.sellon.domain.channels.exception.UsersChannelNotFoundException;
import com.aivle.sellon.domain.channels.repository.UsersChannelRepository;
import com.aivle.sellon.domain.user.entity.User;
import com.aivle.sellon.domain.user.exception.UserNotFoundException;
import com.aivle.sellon.domain.user.repository.UserRepository;
import com.aivle.sellon.global.security.principal.UserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CsInquiryService {

    private final CsInquiryRepository csInquiryRepository;
    private final UserRepository userRepository;
    private final UsersChannelRepository usersChannelRepository;

    @Transactional
    public CsInquiryResponse createInquiry(UserPrincipal principal, CsInquiryRequest request) {
        User user = userRepository.findById(principal.getId())
                .orElseThrow(UserNotFoundException::new);
        // TODO: 채널이 여러 개일 경우 선택 로직 필요 - 현재는 같은 회사의 첫 번째 연결 채널로 임시 처리
        // 채널은 루트 사용자만 연결하므로, 일반 사용자도 문의할 수 있도록 회사(companyId) 기준으로 조회
        UsersChannel usersChannel = usersChannelRepository.findByRootUser_Company_Id(principal.getCompanyId()).stream()
                .findFirst()
                .orElseThrow(UsersChannelNotFoundException::new);

        CsInquiry inquiry = CsInquiry.of(
            user, usersChannel,
            request.inquireTitle(), request.inquireContent(), request.inquireType(), request.attachmentUrl()
        );
        csInquiryRepository.save(inquiry);
        return toResponse(inquiry);
    }

    @Transactional(readOnly = true)
    public List<CsInquiryResponse> getMyInquiries(UserPrincipal principal) {
        return csInquiryRepository.findByUser_Id(principal.getId()).stream()
            .map(this::toResponse)
            .toList();
    }

    @Transactional
    public CsInquiryResponse answerInquiry(Long inquireKey, CsAnswerRequest request) {
        CsInquiry inquiry = csInquiryRepository.findById(inquireKey)
            .orElseThrow(CsInquiryNotFoundException::new);
        inquiry.answer(request.inquireAnswer());
        return toResponse(inquiry);
    }

    @Transactional(readOnly = true)
    public CsInquiryResponse getInquiryDetail(Long inquireKey) {
        CsInquiry inquiry = csInquiryRepository.findById(inquireKey)
                .orElseThrow(CsInquiryNotFoundException::new);
        return toResponse(inquiry);
    }

    @Transactional(readOnly = true)
    public List<CsInquiryResponse> getAllInquiries(InquiryStatus status) {
        List<CsInquiry> inquiries = status == null
                ? csInquiryRepository.findAll()
                : csInquiryRepository.findByInquiryStatus(status);
        return inquiries.stream().map(this::toResponse).toList();
    }

    private CsInquiryResponse toResponse(CsInquiry inquiry) {
        return new CsInquiryResponse(
            inquiry.getInquireKey(),
            inquiry.getInquireTitle(),
            inquiry.getInquireContent(),
            inquiry.getInquireType(),
            inquiry.getAttachmentUrl(),
            inquiry.getInquireAnswer(),
            inquiry.getInquiryStatus()
        );
    }
}
