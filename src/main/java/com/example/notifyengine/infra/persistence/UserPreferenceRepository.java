package com.example.notifyengine.infra.persistence;

import com.example.notifyengine.domain.model.Channel;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface UserPreferenceRepository extends JpaRepository<UserPreference, UUID> {

    List<UserPreference> findByUserId(String userId);

    Optional<UserPreference> findByUserIdAndChannelAndEventType(String userId, Channel channel, String eventType);

    @Query("""
            select count(p) > 0 from UserPreference p
            where p.userId = :userId and p.channel = :channel and p.optedOut = true
              and (p.eventType is null or p.eventType = :eventType)
            """)
    boolean isOptedOut(@Param("userId") String userId,
                       @Param("channel") Channel channel,
                       @Param("eventType") String eventType);
}
