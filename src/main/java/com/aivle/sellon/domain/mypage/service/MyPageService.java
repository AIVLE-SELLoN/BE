package com.aivle.sellon.domain.mypage.service;

import com.aivle.sellon.domain.company.entity.Company;
import com.aivle.sellon.domain.company.exception.CompanyNotFoundException;
import com.aivle.sellon.domain.company.repository.CompanyRepository;
import com.aivle.sellon.domain.company.util.CompanyKeyGenerator;
import com.aivle.sellon.domain.mypage.dto.request.MyPageUpdateRequest;
import com.aivle.sellon.domain.mypage.dto.request.ProfileImageCompleteRequest;
import com.aivle.sellon.domain.mypage.dto.request.ProfileImagePresignedUrlRequest;
import com.aivle.sellon.domain.mypage.dto.request.RecipientRequest;
import com.aivle.sellon.domain.mypage.dto.request.ReportSettingRequest;
import com.aivle.sellon.domain.mypage.dto.response.MyPageResponse;
import com.aivle.sellon.domain.mypage.dto.response.ProfileImagePresignedUrlResponse;
import com.aivle.sellon.domain.mypage.dto.response.RecipientResponse;
import com.aivle.sellon.domain.mypage.dto.response.ReportSettingResponse;
import com.aivle.sellon.domain.mypage.entity.MonthlyReportRecipient;
import com.aivle.sellon.domain.mypage.entity.MonthlyReportSetting;
import com.aivle.sellon.domain.mypage.exception.CompanyKeyNotIssuedException;
import com.aivle.sellon.domain.mypage.exception.DuplicateRecipientEmailException;
import com.aivle.sellon.domain.mypage.exception.FieldNotEditableException;
import com.aivle.sellon.domain.mypage.exception.FileSizeExceededException;
import com.aivle.sellon.domain.mypage.exception.InvalidObjectKeyException;
import com.aivle.sellon.domain.mypage.exception.InvalidSendDayException;
import com.aivle.sellon.domain.mypage.exception.NotCompanyOwnerException;
import com.aivle.sellon.domain.mypage.exception.ProfileImageNotUploadedException;
import com.aivle.sellon.domain.mypage.exception.RecipientNotOwnedException;
import com.aivle.sellon.domain.mypage.exception.TooManyRecipientsException;
import com.aivle.sellon.domain.mypage.repository.MonthlyReportRecipientRepository;
import com.aivle.sellon.domain.mypage.repository.MonthlyReportSettingRepository;
import com.aivle.sellon.domain.mypage.util.CompanyKeyMasker;
import com.aivle.sellon.domain.user.entity.User;
import com.aivle.sellon.domain.user.enums.Role;
import com.aivle.sellon.domain.user.exception.DuplicateEmailException;
import com.aivle.sellon.domain.user.exception.UserNotFoundException;
import com.aivle.sellon.domain.user.repository.UserRepository;
import com.aivle.sellon.domain.verification.service.EmailVerificationService;
import com.aivle.sellon.global.file.enums.AcceptableFileType;
import com.aivle.sellon.global.file.enums.FileDirectory;
import com.aivle.sellon.global.file.exception.InvalidExtensionException;
import com.aivle.sellon.global.file.utils.GlobalFileValidator;
import com.aivle.sellon.global.security.principal.UserPrincipal;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.HeadObjectRequest;
import software.amazon.awssdk.services.s3.model.HeadObjectResponse;
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.S3Exception;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;
import software.amazon.awssdk.services.s3.presigner.model.PutObjectPresignRequest;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Pattern;

@Slf4j
@Service
@RequiredArgsConstructor
public class MyPageService {

    private static final int MAX_RECIPIENTS = 20;
    private static final int MIN_SEND_DAY = 1;
    private static final int MAX_SEND_DAY = 28;
    private static final String DEFAULT_IMAGE_CONTENT_TYPE = "image/jpeg";
    private static final Pattern PROFILE_IMAGE_FILE_NAME =
            Pattern.compile("\\d+-[0-9a-f]{8}(-[0-9a-f]{4}){3}-[0-9a-f]{12}\\.[A-Za-z]+");

    private final UserRepository userRepository;
    private final CompanyRepository companyRepository;
    private final CompanyKeyGenerator companyKeyGenerator;
    private final MonthlyReportSettingRepository monthlyReportSettingRepository;
    private final MonthlyReportRecipientRepository monthlyReportRecipientRepository;
    private final CompanyKeyMasker companyKeyMasker;
    private final EmailVerificationService emailVerificationService;
    private final GlobalFileValidator globalFileValidator;
    private final S3Client imageS3Client;
    private final S3Presigner imageS3Presigner;

    @Value("${cloud.aws.s3.image.bucket}")
    private String bucket;

    @Transactional
    public String issueCompanyKey(UserPrincipal principal) {
        if (principal.getRole() != Role.ROOT)
            throw new NotCompanyOwnerException();

        Company company = companyRepository.findById(principal.getCompanyId())
                .orElseThrow(CompanyNotFoundException::new);

        String newKey = companyKeyGenerator.generate();
        company.issueJoinKey(newKey);

        return newKey;
    }

    public String getCompanyKey(UserPrincipal principal) {
        if (principal.getRole() != Role.ROOT)
            throw new NotCompanyOwnerException();

        Company company = companyRepository.findById(principal.getCompanyId())
                .orElseThrow(CompanyNotFoundException::new);

        String joinKey = company.getJoinKey();
        if (joinKey == null)
            throw new CompanyKeyNotIssuedException();

        return joinKey;
    }

    public MyPageResponse getMyPage(UserPrincipal principal) {
        User user = userRepository.findByIdAndDeletedAtIsNull(principal.getId())
                .orElseThrow(UserNotFoundException::new);
        Company company = user.getCompany();

        ReportSettingResponse reportSetting = findReportSetting(company.getId());

        return buildResponse(user, company, reportSetting);
    }

    @Transactional
    public MyPageResponse updateMyPage(UserPrincipal principal, MyPageUpdateRequest request) {
        User user = userRepository.findByIdAndDeletedAtIsNull(principal.getId())
                .orElseThrow(UserNotFoundException::new);
        Role role = user.getRole();
        Company company = user.getCompany();

        updateEmailIfChanged(request, user, role);
        updateBrandNameIfChanged(request, company, role);

        ReportSettingResponse reportSetting = request.reportSetting() != null
                ? applyReportSetting(company, request.reportSetting())
                : findReportSetting(company.getId());

        return buildResponse(user, company, reportSetting);
    }

    private void updateEmailIfChanged(MyPageUpdateRequest request, User user, Role role) {
        if (request.email() == null || request.email().equals(user.getEmail()))
            return;

        if (role == Role.ROOT)
            throw new FieldNotEditableException();

        if (userRepository.existsByEmailAndDeletedAtIsNull(request.email()))
            throw new DuplicateEmailException();

        emailVerificationService.validateVerificationToken(request.email(), request.verificationToken());

        user.changeEmail(request.email());
    }

    private void updateBrandNameIfChanged(MyPageUpdateRequest request, Company company, Role role) {
        if (request.brandName() == null || request.brandName().equals(company.getName()))
            return;

        if (role != Role.ROOT)
            throw new FieldNotEditableException();

        company.rename(request.brandName());
    }

    private ReportSettingResponse applyReportSetting(Company company, ReportSettingRequest request) {
        validateSendDay(request.sendDay());
        validateRecipients(request.recipients());

        MonthlyReportSetting setting;
        var existingSetting = monthlyReportSettingRepository.findByCompanyIdAndDeletedAtIsNull(company.getId());
        if (existingSetting.isPresent()) {
            setting = existingSetting.get();
            setting.update(request.enabled(), request.sendDay(), request.sendTime());
        } else {
            setting = monthlyReportSettingRepository.save(
                    MonthlyReportSetting.create(company, request.enabled(), request.sendDay(), request.sendTime())
            );
        }

        List<RecipientResponse> recipients = replaceRecipients(company, request.recipients());

        return ReportSettingResponse.of(setting, recipients);
    }

    private void validateSendDay(int sendDay) {
        if (sendDay < MIN_SEND_DAY || sendDay > MAX_SEND_DAY)
            throw new InvalidSendDayException();
    }

    private void validateRecipients(List<RecipientRequest> recipients) {
        if (recipients.size() > MAX_RECIPIENTS)
            throw new TooManyRecipientsException();

        long distinctEmailCount = recipients.stream()
                .map(RecipientRequest::email)
                .distinct()
                .count();
        if (distinctEmailCount != recipients.size())
            throw new DuplicateRecipientEmailException();
    }

    private List<RecipientResponse> replaceRecipients(Company company, List<RecipientRequest> requests) {
        List<MonthlyReportRecipient> existing =
                monthlyReportRecipientRepository.findAllByCompanyIdAndDeletedAtIsNullOrderByIdAsc(company.getId());

        Map<Long, MonthlyReportRecipient> existingById = new HashMap<>();
        for (MonthlyReportRecipient recipient : existing)
            existingById.put(recipient.getId(), recipient);

        List<MonthlyReportRecipient> toDelete = new ArrayList<>();
        for (MonthlyReportRecipient recipient : existing) {
            boolean stillRequested = requests.stream()
                    .anyMatch(r -> recipient.getId().equals(r.recipientId()));
            if (!stillRequested)
                toDelete.add(recipient);
        }
        if (!toDelete.isEmpty())
            monthlyReportRecipientRepository.deleteAll(toDelete);

        List<MonthlyReportRecipient> result = new ArrayList<>();
        for (RecipientRequest request : requests) {
            if (request.recipientId() == null) {
                result.add(monthlyReportRecipientRepository.save(
                        MonthlyReportRecipient.create(company, request.department(), request.email())
                ));
                continue;
            }

            MonthlyReportRecipient recipient = existingById.get(request.recipientId());
            if (recipient == null)
                throw new RecipientNotOwnedException();

            recipient.update(request.department(), request.email());
            result.add(recipient);
        }

        return result.stream()
                .sorted(Comparator.comparing(MonthlyReportRecipient::getId))
                .map(RecipientResponse::of)
                .toList();
    }

    private ReportSettingResponse findReportSetting(Long companyId) {
        return monthlyReportSettingRepository.findByCompanyIdAndDeletedAtIsNull(companyId)
                .map(setting -> ReportSettingResponse.of(setting, findRecipients(companyId)))
                .orElseGet(ReportSettingResponse::defaultValue);
    }

    private List<RecipientResponse> findRecipients(Long companyId) {
        return monthlyReportRecipientRepository.findAllByCompanyIdAndDeletedAtIsNullOrderByIdAsc(companyId).stream()
                .map(RecipientResponse::of)
                .toList();
    }

    private MyPageResponse buildResponse(User user, Company company, ReportSettingResponse reportSetting) {
        String companyKey = null;
        boolean companyKeyIssued = false;
        if (user.getRole() == Role.ROOT) {
            String joinKey = company.getJoinKey();
            companyKeyIssued = joinKey != null;
            companyKey = companyKeyMasker.mask(joinKey);
        }

        String profileImageUrl = resolveProfileImageUrl(user.getProfileImageKey());

        return MyPageResponse.of(user, companyKey, companyKeyIssued, reportSetting, profileImageUrl);
    }

    public ProfileImagePresignedUrlResponse issueProfileImagePresignedUrl(
            UserPrincipal principal,
            ProfileImagePresignedUrlRequest request
    ) {
        User user = userRepository.findByIdAndDeletedAtIsNull(principal.getId())
                .orElseThrow(UserNotFoundException::new);

        AcceptableFileType fileType = globalFileValidator.validateAndGetExtension(request.fileName());
        if (!FileDirectory.PROFILE.allows(fileType))
            throw new InvalidExtensionException();

        if (request.fileSize() <= 0 || request.fileSize() > FileDirectory.PROFILE.getFileSize())
            throw new FileSizeExceededException();

        String objectKey = buildProfileImageObjectKey(user.getId(), fileType.getExtension());

        PutObjectRequest objectRequest = PutObjectRequest.builder()
                .bucket(bucket)
                .key(objectKey)
                .contentType(fileType.getMimeType())
                .contentLength(request.fileSize())
                .build();

        PutObjectPresignRequest presignRequest = PutObjectPresignRequest.builder()
                .signatureDuration(FileDirectory.PROFILE.getUploadTime())
                .putObjectRequest(objectRequest)
                .build();

        String presignedUrl = imageS3Presigner.presignPutObject(presignRequest).url().toString();

        return ProfileImagePresignedUrlResponse.of(
                presignedUrl,
                objectKey,
                fileType.getMimeType(),
                FileDirectory.PROFILE.getUploadTime().toSeconds()
        );
    }

    @Transactional
    public MyPageResponse completeProfileImageUpload(UserPrincipal principal, ProfileImageCompleteRequest request) {
        User user = userRepository.findByIdAndDeletedAtIsNull(principal.getId())
                .orElseThrow(UserNotFoundException::new);
        Company company = user.getCompany();

        validateProfileImageObjectKey(user, request.objectKey());

        HeadObjectResponse headObjectResponse;
        try {
            headObjectResponse = imageS3Client.headObject(HeadObjectRequest.builder()
                    .bucket(bucket)
                    .key(request.objectKey())
                    .build());
        } catch (NoSuchKeyException e) {
            throw new ProfileImageNotUploadedException();
        }

        if (headObjectResponse.contentLength() > FileDirectory.PROFILE.getFileSize()) {
            imageS3Client.deleteObject(DeleteObjectRequest.builder().bucket(bucket).key(request.objectKey()).build());
            throw new FileSizeExceededException();
        }

        String previousKey = user.getProfileImageKey();
        if (previousKey != null && !previousKey.equals(request.objectKey())) {
            imageS3Client.deleteObject(DeleteObjectRequest.builder().bucket(bucket).key(previousKey).build());
        }

        user.changeProfileImage(request.objectKey());

        ReportSettingResponse reportSetting = findReportSetting(company.getId());
        return buildResponse(user, company, reportSetting);
    }

    @Transactional
    public MyPageResponse removeProfileImage(UserPrincipal principal) {
        User user = userRepository.findByIdAndDeletedAtIsNull(principal.getId())
                .orElseThrow(UserNotFoundException::new);
        Company company = user.getCompany();

        String profileImageKey = user.getProfileImageKey();
        if (profileImageKey != null) {
            try {
                imageS3Client.deleteObject(DeleteObjectRequest.builder().bucket(bucket).key(profileImageKey).build());
            } catch (S3Exception e) {
                log.warn("프로필 이미지 S3 객체 삭제 실패. key={}", profileImageKey, e);
            }
            user.removeProfileImage();
        }

        ReportSettingResponse reportSetting = findReportSetting(company.getId());
        return buildResponse(user, company, reportSetting);
    }

    private String buildProfileImageObjectKey(Long userId, String extension) {
        return "%s%d-%s.%s".formatted(
                profileImageKeyPrefix(userId), System.currentTimeMillis(), UUID.randomUUID(), extension);
    }

    private String profileImageKeyPrefix(Long userId) {
        return "%s/%d/original/".formatted(FileDirectory.PROFILE.getPrefix(), userId);
    }

    private void validateProfileImageObjectKey(User user, String objectKey) {
        String prefix = profileImageKeyPrefix(user.getId());
        if (!objectKey.startsWith(prefix))
            throw new InvalidObjectKeyException();

        // 서버가 발급하는 파일명은 {timestamp}-{uuid}.{ext} 로 고정이다.
        // 전체 일치 검사이므로 '/' 나 '..' 이 포함된 키는 여기서 전부 걸러진다.
        String fileName = objectKey.substring(prefix.length());
        if (!PROFILE_IMAGE_FILE_NAME.matcher(fileName).matches())
            throw new InvalidObjectKeyException();

        AcceptableFileType fileType = extractFileType(fileName);
        if (fileType == null || !FileDirectory.PROFILE.allows(fileType))
            throw new InvalidObjectKeyException();
    }

    private String resolveProfileImageUrl(String profileImageKey) {
        if (profileImageKey == null)
            return null;

        GetObjectRequest objectRequest = GetObjectRequest.builder()
                .bucket(bucket)
                .key(profileImageKey)
                .responseContentType(resolveImageContentType(profileImageKey))
                .responseContentDisposition("inline")
                .build();

        GetObjectPresignRequest presignRequest = GetObjectPresignRequest.builder()
                .signatureDuration(FileDirectory.PROFILE.getDownloadTime())
                .getObjectRequest(objectRequest)
                .build();

        return imageS3Presigner.presignGetObject(presignRequest).url().toString();
    }

    private String resolveImageContentType(String profileImageKey) {
        AcceptableFileType fileType = extractFileType(profileImageKey);
        return fileType != null ? fileType.getMimeType() : DEFAULT_IMAGE_CONTENT_TYPE;
    }

    private AcceptableFileType extractFileType(String objectKey) {
        String extension = extractExtension(objectKey);
        return extension == null ? null : AcceptableFileType.fromExtension(extension);
    }

    private String extractExtension(String key) {
        int lastDotIndex = key.lastIndexOf('.');
        return lastDotIndex == -1 ? null : key.substring(lastDotIndex + 1);
    }
}
