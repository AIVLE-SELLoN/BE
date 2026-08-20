package com.aivle.sellon.domain.channels.repository.comparison;

import com.aivle.sellon.domain.channels.entity.comparison.ChannelComparison;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ChannelComparisonRepository extends JpaRepository<ChannelComparison, Long> {
    List<ChannelComparison> findByUsersChannel_Company_Id(Long companyId);

    Optional<ChannelComparison> findByUsersChannel_UsersChannelKey(Long usersChannelKey);
}
