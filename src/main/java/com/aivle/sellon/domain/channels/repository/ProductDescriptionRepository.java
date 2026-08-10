package com.aivle.sellon.domain.channels.repository;

import com.aivle.sellon.domain.channels.entity.ProductDescription;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ProductDescriptionRepository extends JpaRepository<ProductDescription, Long> {
    Optional<ProductDescription> findByProductGroupIdAndChannelAndRootUser_Company_Id(
        String productGroupId, String channel, Long companyId
    );
}
