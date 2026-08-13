package com.aivle.sellon.domain.channels.repository.connection;

import com.aivle.sellon.domain.channels.entity.connection.UsersChannel;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface UsersChannelRepository extends JpaRepository<UsersChannel, Long> {
    Optional<UsersChannel> findByCompany_IdAndChannelType(Long companyId, String channelType);

    /**
     * connect/naverCallback의 "조회 후 없으면 생성"을 PostgreSQL의 INSERT ... ON CONFLICT로 원자적으로 처리한다.
     * 애플리케이션 레벨에서 "조회 -> 락 -> 실패 시 재조회"를 시도하면, INSERT가 유니크 제약(
     * uk_users_channel_company_channel_type)에 걸려 실패하는 순간 PostgreSQL이 해당 트랜잭션 전체를
     * abort 상태로 만들어버려서 같은 트랜잭션 안의 재조회도 함께 실패한다(UnexpectedRollbackException으로
     * 이어질 수 있음) - 그래서 애초에 실패할 일이 없는 단일 SQL 문으로 처리한다.
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
