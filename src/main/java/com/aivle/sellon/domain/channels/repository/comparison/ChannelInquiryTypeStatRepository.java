package com.aivle.sellon.domain.channels.repository.comparison;

import com.aivle.sellon.domain.channels.entity.comparison.ChannelInquiryTypeStat;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ChannelInquiryTypeStatRepository extends JpaRepository<ChannelInquiryTypeStat, Long> {
    List<ChannelInquiryTypeStat> findByUsersChannel_UsersChannelKey(Long usersChannelKey);

    // 벌크 delete로 동시 refresh 요청 간 경합(StaleObjectStateException) 방지
    @Modifying(clearAutomatically = true)
    @Query("delete from ChannelInquiryTypeStat s where s.usersChannel.usersChannelKey = :usersChannelKey")
    void deleteByUsersChannel_UsersChannelKey(@Param("usersChannelKey") Long usersChannelKey);
}
