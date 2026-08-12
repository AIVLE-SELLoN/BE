package com.aivle.sellon.domain.channels.repository.comparison;

import com.aivle.sellon.domain.channels.entity.comparison.ChannelInsight;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ChannelInsightRepository extends JpaRepository<ChannelInsight, Long> {
    List<ChannelInsight> findByUsersChannel_UsersChannelKeyOrderByDisplayOrderAsc(Long usersChannelKey);

    void deleteByUsersChannel_UsersChannelKey(Long usersChannelKey);
}
