package com.aivle.sellon.domain.channels.repository.comparison;

import com.aivle.sellon.domain.channels.entity.comparison.ChannelMonthlyInquiry;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ChannelMonthlyInquiryRepository extends JpaRepository<ChannelMonthlyInquiry, Long> {
    List<ChannelMonthlyInquiry> findByUsersChannel_UsersChannelKey(Long usersChannelKey);

    // 파생 delete는 행을 하나씩 지우며 삭제 건수를 검증해 동시 refresh 요청이 겹치면
    // StaleObjectStateException으로 터진다 - 벌크 delete로 바꿔 그 경합 자체를 없앤다.
    @Modifying(clearAutomatically = true)
    @Query("delete from ChannelMonthlyInquiry m where m.usersChannel.usersChannelKey = :usersChannelKey")
    void deleteByUsersChannel_UsersChannelKey(@Param("usersChannelKey") Long usersChannelKey);
}
