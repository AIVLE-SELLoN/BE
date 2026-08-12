package com.aivle.sellon.domain.channels.repository.connection;

import com.aivle.sellon.domain.channels.entity.connection.UsersChannel;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UsersChannelRepository extends JpaRepository<UsersChannel, Long> {
    List<UsersChannel> findByCompany_Id(Long companyId);

    Optional<UsersChannel> findByCompany_IdAndChannelType(Long companyId, String channelType);
}
