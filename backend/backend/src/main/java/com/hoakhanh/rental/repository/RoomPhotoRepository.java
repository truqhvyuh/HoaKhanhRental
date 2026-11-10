
package com.hoakhanh.rental.repository;

import com.hoakhanh.rental.entity.RoomPhoto;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.EntityGraph;

import java.util.List;
import java.util.Optional;

public interface RoomPhotoRepository
        extends JpaRepository<RoomPhoto, Long> {

    List<RoomPhoto> findByRoom_IdOrderByIdAsc(
            Long roomId
    );

    long countByRoom_Id(Long roomId);

    Optional<RoomPhoto> findByIdAndRoom_Id(
            Long photoId,
            Long roomId
    );

    @EntityGraph(attributePaths = "room")
    Optional<RoomPhoto> findByFilename(String filename);
}
