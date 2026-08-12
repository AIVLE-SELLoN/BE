package com.aivle.sellon.domain.channels.service.productmapping;

import com.aivle.sellon.domain.channels.dto.response.ProductMappingReviewItemResponse;
import com.aivle.sellon.domain.channels.entity.productmapping.ProductMappingReviewItem;
import com.aivle.sellon.domain.channels.exception.ChannelAccessDeniedException;
import com.aivle.sellon.domain.channels.exception.productmapping.ProductMappingReviewItemNotFoundException;
import com.aivle.sellon.domain.channels.repository.productmapping.ProductMappingReviewItemRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ProductMappingReviewService {

    private final ProductMappingReviewItemRepository productMappingReviewItemRepository;

    @Transactional(readOnly = true)
    public List<ProductMappingReviewItemResponse> getReviewQueue(Long companyId, boolean resolved) {
        return productMappingReviewItemRepository.findByCompany_IdAndResolved(companyId, resolved).stream()
                .map(ProductMappingReviewItemResponse::from)
                .toList();
    }

    @Transactional
    public ProductMappingReviewItemResponse resolve(Long companyId, Long reviewItemId) {
        ProductMappingReviewItem item = productMappingReviewItemRepository.findById(reviewItemId)
                .orElseThrow(ProductMappingReviewItemNotFoundException::new);
        if (!item.getCompany().getId().equals(companyId)) {
            throw new ChannelAccessDeniedException();
        }
        item.resolve();
        return ProductMappingReviewItemResponse.from(item);
    }
}
