package com.aivle.sellon.domain.channels.repository.productmapping;

import com.aivle.sellon.domain.channels.entity.productmapping.ChannelProduct;
import com.aivle.sellon.domain.channels.enums.MappingStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ChannelProductRepository extends JpaRepository<ChannelProduct, Long> {
    List<ChannelProduct> findByUsersChannel_UsersChannelKey(Long usersChannelKey);

    List<ChannelProduct> findByUsersChannel_UsersChannelKeyAndMappingStatus(Long usersChannelKey, MappingStatus status);

    List<ChannelProduct> findByUsersChannel_UsersChannelKeyAndMappingStatusNot(Long usersChannelKey, MappingStatus status);

    /**
     * 같은 상품(channelItemId)의 나머지 미매칭 옵션들 — 하나가 확정되면 이들도 같이 매칭 처리(cascade)하기 위해 조회.
     */
    List<ChannelProduct> findByUsersChannel_UsersChannelKeyAndChannelItemIdAndMappingStatus(
            Long usersChannelKey, String channelItemId, MappingStatus status);

    /**
     * 매칭 툴 배치 결과(mapping_result.csv) import 시, 같은 channelItemId를 공유하는 옵션들을 한번에 찾기 위한 조회.
     */
    List<ChannelProduct> findByUsersChannel_UsersChannelKeyAndChannelItemId(Long usersChannelKey, String channelItemId);

    @Query("SELECT cp FROM ChannelProduct cp " +
            "WHERE cp.usersChannel.usersChannelKey = :usersChannelKey " +
            "AND (LOWER(cp.productName) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
            "OR LOWER(cp.sourceSku) LIKE LOWER(CONCAT('%', :keyword, '%')))")
    List<ChannelProduct> searchByKeyword(@Param("usersChannelKey") Long usersChannelKey, @Param("keyword") String keyword);

    long countByUsersChannel_UsersChannelKeyAndMappingStatus(Long usersChannelKey, MappingStatus status);

    long countByUsersChannel_UsersChannelKeyAndMappingStatusNot(Long usersChannelKey, MappingStatus status);
}
