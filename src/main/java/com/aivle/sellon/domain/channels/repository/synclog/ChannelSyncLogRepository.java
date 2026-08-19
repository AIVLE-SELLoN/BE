package com.aivle.sellon.domain.channels.repository.synclog;

import com.aivle.sellon.domain.channels.entity.synclog.ChannelSyncLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ChannelSyncLogRepository extends JpaRepository<ChannelSyncLog, Long> {
    Page<ChannelSyncLog> findByUsersChannel_Company_IdOrderByCreatedDateDesc(Long companyId, Pageable pageable);

    Page<ChannelSyncLog> findByUsersChannel_Company_IdAndUsersChannel_ChannelTypeOrderByCreatedDateDesc(
            Long companyId, String channelType, Pageable pageable);
}
