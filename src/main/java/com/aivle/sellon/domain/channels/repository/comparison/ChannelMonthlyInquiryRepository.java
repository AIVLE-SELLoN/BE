package com.aivle.sellon.domain.channels.repository.comparison;

import com.aivle.sellon.domain.channels.entity.comparison.ChannelMonthlyInquiry;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ChannelMonthlyInquiryRepository extends JpaRepository<ChannelMonthlyInquiry, Long> {
    List<ChannelMonthlyInquiry> findByUsersChannel_UsersChannelKey(Long usersChannelKey);

    void deleteByUsersChannel_UsersChannelKey(Long usersChannelKey);
}
