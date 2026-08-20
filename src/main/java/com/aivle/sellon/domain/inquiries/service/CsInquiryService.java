package com.aivle.sellon.domain.inquiries.service;

import com.aivle.sellon.domain.inquiries.dto.request.CsAnswerRequest;
import com.aivle.sellon.domain.inquiries.dto.request.CsInquiryRequest;
import com.aivle.sellon.domain.inquiries.dto.request.CsInquiryUpdateRequest;
import com.aivle.sellon.domain.inquiries.dto.response.CsInquiryResponse;
import com.aivle.sellon.domain.inquiries.entity.CsInquiry;
import com.aivle.sellon.domain.inquiries.enums.InquiryStatus;
import com.aivle.sellon.domain.inquiries.exception.CsInquiryAccessDeniedException;
import com.aivle.sellon.domain.inquiries.exception.CsInquiryAlreadyAnsweredException;
import com.aivle.sellon.domain.inquiries.exception.CsInquiryNotFoundException;
import com.aivle.sellon.domain.inquiries.repository.CsInquiryRepository;
import com.aivle.sellon.domain.user.entity.User;
import com.aivle.sellon.domain.user.enums.Role;
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

    @Transactional
    public CsInquiryResponse createInquiry(UserPrincipal principal, CsInquiryRequest request) {
        User user = userRepository.findById(principal.getId())
                .orElseThrow(UserNotFoundException::new);

        CsInquiry inquiry = CsInquiry.of(
            user,
            request.inquireTitle(), request.inquireContent(), request.inquireType(), request.attachmentUrl()
        );
        csInquiryRepository.save(inquiry);
        return toResponse(inquiry);
    }

    @Transactional(readOnly = true)
    public List<CsInquiryResponse> getMyInquiries(UserPrincipal principal) {
        return csInquiryRepository.findByUser_IdAndDeletedAtIsNull(principal.getId()).stream()
            .map(this::toResponse)
            .toList();
    }

    @Transactional
    public CsInquiryResponse updateInquiry(UserPrincipal principal, Long inquireKey, CsInquiryUpdateRequest request) {
        CsInquiry inquiry = getOwnedInquiry(principal, inquireKey);
        if (inquiry.isAnswered())
            throw new CsInquiryAlreadyAnsweredException();

        inquiry.update(request.inquireTitle(), request.inquireContent(), request.inquireType(), request.attachmentUrl());
        return toResponse(inquiry);
    }

    @Transactional
    public void deleteInquiry(UserPrincipal principal, Long inquireKey) {
        CsInquiry inquiry = getOwnedInquiry(principal, inquireKey);
        if (inquiry.isAnswered())
            throw new CsInquiryAlreadyAnsweredException();

        inquiry.remove();
    }

    private CsInquiry getOwnedInquiry(UserPrincipal principal, Long inquireKey) {
        CsInquiry inquiry = csInquiryRepository.findById(inquireKey)
                .filter(i -> i.getDeletedAt() == null)
                .orElseThrow(CsInquiryNotFoundException::new);

        if (!inquiry.isOwnedBy(principal.getId()))
            throw new CsInquiryAccessDeniedException();

        return inquiry;
    }

    @Transactional
    public CsInquiryResponse answerInquiry(Long inquireKey, CsAnswerRequest request) {
        CsInquiry inquiry = csInquiryRepository.findById(inquireKey)
                .filter(i -> i.getDeletedAt() == null)
                .orElseThrow(CsInquiryNotFoundException::new);

        if (inquiry.isAnswered())
            throw new CsInquiryAlreadyAnsweredException();

        inquiry.answer(request.inquireAnswer());
        return toResponse(inquiry);
    }

    @Transactional(readOnly = true)
    public CsInquiryResponse getInquiryDetail(UserPrincipal principal, Long inquireKey) {
        CsInquiry inquiry = csInquiryRepository.findById(inquireKey)
                .filter(i -> i.getDeletedAt() == null)
                .orElseThrow(CsInquiryNotFoundException::new);

        boolean isOwner = inquiry.isOwnedBy(principal.getId());
        boolean isAdmin = principal.getRole() == Role.ADMIN;
        if (!isOwner && !isAdmin)
            throw new CsInquiryAccessDeniedException();

        return toResponse(inquiry);
    }

    @Transactional(readOnly = true)
    public List<CsInquiryResponse> getAllInquiries(InquiryStatus status) {
        List<CsInquiry> inquiries = status == null
                ? csInquiryRepository.findByDeletedAtIsNull()
                : csInquiryRepository.findByInquiryStatusAndDeletedAtIsNull(status);
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
