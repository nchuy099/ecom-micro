package com.nchuy099.ecommerce.notification.repository;

import java.util.List;

import com.nchuy099.ecommerce.notification.entity.NotificationDlqEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface NotificationDlqRepository extends JpaRepository<NotificationDlqEntity, Long> {
    List<NotificationDlqEntity> findAllByOrderByCreatedAtDesc();
}
