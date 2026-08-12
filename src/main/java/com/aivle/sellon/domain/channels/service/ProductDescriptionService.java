package com.aivle.sellon.domain.channels.service;

import com.aivle.sellon.domain.channels.entity.ProductDescription;
import com.aivle.sellon.domain.channels.repository.ProductDescriptionRepository;
import com.aivle.sellon.domain.user.entity.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

// 채널별 상품 설명 저장/갱신. 개선안 승인 시 실제 반영, 롤백 시 되돌리는 데 쓰인다(proposal 도메인에서 호출).
@Service
@RequiredArgsConstructor
public class ProductDescriptionService {

    private final ProductDescriptionRepository productDescriptionRepository;

    @Transactional(readOnly = true)
    public Optional<String> findDescription(Long companyId, String productGroupId, String channel) {
        return productDescriptionRepository
            .findByProductGroupIdAndChannelAndRootUser_Company_Id(productGroupId, channel, companyId)
            .map(ProductDescription::getDescription);
    }

    @Transactional
    public void apply(User rootUser, Long companyId, String productGroupId, String channel, String description) {
        ProductDescription productDescription = productDescriptionRepository
            .findByProductGroupIdAndChannelAndRootUser_Company_Id(productGroupId, channel, companyId)
            .map(existing -> {
                existing.update(description);
                return existing;
            })
            .orElseGet(() -> ProductDescription.of(rootUser, productGroupId, channel, description));
        productDescriptionRepository.save(productDescription);
    }
}
