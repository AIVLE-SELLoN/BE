package com.aivle.sellon.domain.channels.repository.comparison;

import com.aivle.sellon.domain.channels.entity.comparison.ChannelInsight;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ChannelInsightRepository extends JpaRepository<ChannelInsight, Long> {
    List<ChannelInsight> findByUsersChannel_UsersChannelKeyOrderByDisplayOrderAsc(Long usersChannelKey);

    // 벌크 delete로 동시 refresh 요청 간 경합(StaleObjectStateException) 방지
    @Modifying(clearAutomatically = true)
    @Query("delete from ChannelInsight i where i.usersChannel.usersChannelKey = :usersChannelKey")
    void deleteByUsersChannel_UsersChannelKey(@Param("usersChannelKey") Long usersChannelKey);
}
