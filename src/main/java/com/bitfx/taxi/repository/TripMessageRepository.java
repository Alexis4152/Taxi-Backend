package com.bitfx.taxi.repository;

import com.bitfx.taxi.model.TripMessage;
import com.bitfx.taxi.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface TripMessageRepository extends JpaRepository<TripMessage, Long> {
    List<TripMessage> findByTrip_IdOrderByCreatedAtAsc(Long tripId);

    List<TripMessage> findBySenderUserOrderByCreatedAtAsc(User sender);

    // Al anonimizar una cuenta (ver AccountAnonymizationService): el texto libre del chat puede
    // traer datos personales, asi que se reemplaza; el registro del mensaje se conserva.
    @Modifying
    @Query("update TripMessage m set m.body = :body where m.senderUser = :sender")
    int redactBySender(@Param("sender") User sender, @Param("body") String body);
}
