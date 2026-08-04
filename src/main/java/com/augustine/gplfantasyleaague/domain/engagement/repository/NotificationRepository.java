package com.augustine.gplfantasyleaague.domain.engagement.repository;

import com.augustine.gplfantasyleaague.domain.engagement.dtos.NotificationResponse;
import com.augustine.gplfantasyleaague.domain.engagement.entity.Notification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface NotificationRepository extends JpaRepository<Notification, Integer> {
    List<Notification> findByUserId(Integer id);

    List<Notification> findByUserIdAndIsRead(Integer userId, Boolean isRead);

    // Bulk-marks every unread notification for a user as read in one UPDATE -
    // powers the inbox's "Mark all read" button, which previously simulated
    // this client-side by firing one PATCH per unread notification since no
    // real bulk endpoint existed. Returns the number of rows actually
    // flipped, so the caller can tell "nothing to do" from "done".
    @Modifying
    @Query("UPDATE Notification n SET n.isRead = true WHERE n.user.id = :userId AND n.isRead = false")
    int markAllAsReadByUserId(@Param("userId") Integer userId);
}