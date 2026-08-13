package com.aivle.sellon.domain.channels.repository.connection;

import com.aivle.sellon.domain.channels.entity.connection.UsersChannel;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;

import java.util.List;
import java.util.Optional;

public interface UsersChannelRepository extends JpaRepository<UsersChannel, Long> {
    List<UsersChannel> findByCompany_Id(Long companyId);

    Optional<UsersChannel> findByCompany_IdAndChannelType(Long companyId, String channelType);

    /**
     * connect/naverCallback에서 조회 후 생성(read-then-write)하는 구간의 동시성 방어용.
     * 같은 (company, channelType) 조합에 대한 동시 요청이 각자 "없음"을 보고 중복 행을
     * 만들지 않도록, 트랜잭션이 끝날 때까지 해당 행(또는 행이 될 자리)에 대한 쓰기 락을 건다.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<UsersChannel> findWithLockByCompany_IdAndChannelType(Long companyId, String channelType);
}
