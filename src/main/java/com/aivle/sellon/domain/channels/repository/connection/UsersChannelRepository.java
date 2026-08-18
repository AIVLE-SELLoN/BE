package com.aivle.sellon.domain.channels.repository.connection;

import com.aivle.sellon.domain.channels.entity.connection.UsersChannel;
import com.aivle.sellon.domain.channels.enums.ConnectionStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface UsersChannelRepository extends JpaRepository<UsersChannel, Long> {
    Optional<UsersChannel> findByCompany_IdAndChannelType(Long companyId, String channelType);

    List<UsersChannel> findByCompany_Id(Long companyId);

    List<UsersChannel> findByConnectionStatus(ConnectionStatus connectionStatus);

    List<UsersChannel> findByCompany_IdAndConnectionStatus(Long companyId, ConnectionStatus connectionStatus);

    /**
     * connect/naverCallback의 "조회 후 없으면 생성"을 PostgreSQL의 INSERT ... ON CONFLICT로 원자적으로 처리한다.
     */
    @Modifying
    @Query(value = """
            INSERT INTO users_channel (company_id, channel_type, channel_code, connection_status, created_date, updated_at)
            VALUES (:companyId, :channelType, :channelCode, 'CONNECTED', now(), now())
            ON CONFLICT (company_id, channel_type)
            DO UPDATE SET channel_code = EXCLUDED.channel_code,
                          connection_status = 'CONNECTED',
                          updated_at = now()
            """, nativeQuery = true)
    void upsertConnected(@Param("companyId") Long companyId, @Param("channelType") String channelType, @Param("channelCode") String channelCode);
}
