
package com.hoakhanh.rental.repository;

import com.hoakhanh.rental.entity.Room;
import com.hoakhanh.rental.entity.ModerationStatus;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.List;

public interface RoomRepository
        extends JpaRepository<Room, Long>,
        JpaSpecificationExecutor<Room> {

    // Danh sach phong cua chu tro
    List<Room>
    findByLandlord_IdAndDeletedAtIsNullOrderByCreatedAtDesc(
            Long landlordId
    );

    // Danh sach phong theo trang thai kiem duyet
    List<Room>
    findByModerationStatusAndDeletedAtIsNullOrderByCreatedAtAsc(
            ModerationStatus status
    );
}
