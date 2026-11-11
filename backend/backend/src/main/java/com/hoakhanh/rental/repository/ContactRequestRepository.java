package com.hoakhanh.rental.repository;

import com.hoakhanh.rental.entity.ContactRequest;
import com.hoakhanh.rental.entity.ContactRequestStatus;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ContactRequestRepository
        extends JpaRepository<ContactRequest, Long> {

    // Danh sach yeu cau lien he cua sinh vien
    List<ContactRequest> findByStudent_IdOrderByCreatedAtDesc(
            Long studentId
    );

    // Danh sach yeu cau lien he thuoc cac phong cua chu tro
    List<ContactRequest> findByRoom_Landlord_IdOrderByCreatedAtDesc(
            Long landlordId
    );

    // Kiem tra sinh vien da co yeu cau dang cho xu ly voi phong hay chua
    boolean existsByStudent_IdAndRoom_IdAndStatus(
            Long studentId,
            Long roomId,
            ContactRequestStatus status
    );

    // Tim yeu cau thuoc phong cua chu tro de kiem tra quyen so huu
    Optional<ContactRequest> findByIdAndRoom_Landlord_Id(
            Long id,
            Long landlordId
    );
}
