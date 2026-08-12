package com.aivle.sellon.domain.channels.repository.comparison;

import com.aivle.sellon.domain.channels.entity.comparison.ChannelInquiryTypeStat;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ChannelInquiryTypeStatRepository extends JpaRepository<ChannelInquiryTypeStat, Long> {
    List<ChannelInquiryTypeStat> findByUsersChannel_UsersChannelKey(Long usersChannelKey);

    void deleteByUsersChannel_UsersChannelKey(Long usersChannelKey);
}
