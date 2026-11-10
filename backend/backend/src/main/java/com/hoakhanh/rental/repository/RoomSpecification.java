
package com.hoakhanh.rental.repository;

import com.hoakhanh.rental.entity.Room;
import com.hoakhanh.rental.entity.ModerationStatus;
import com.hoakhanh.rental.entity.OccupancyStatus;
import com.hoakhanh.rental.entity.VisibilityStatus;
import org.springframework.data.jpa.domain.Specification;

import java.math.BigDecimal;
import java.util.Locale;

public class RoomSpecification {

    private RoomSpecification() {
    }

    public static Specification<Room> search(
            String keyword,
            String area,
            BigDecimal minPrice,
            BigDecimal maxPrice,
            BigDecimal minArea,
            BigDecimal maxArea
    ) {
        return (root, query, cb) -> {

            var conditions = cb.conjunction();

            // Chi hien thi phong cong khai
            conditions = cb.and(conditions,
                    cb.equal(root.get("moderationStatus"),
                            ModerationStatus.APPROVED));

            conditions = cb.and(conditions,
                    cb.equal(root.get("occupancyStatus"),
                            OccupancyStatus.AVAILABLE));

            conditions = cb.and(conditions,
                    cb.equal(root.get("visibilityStatus"),
                            VisibilityStatus.VISIBLE));

            conditions = cb.and(conditions,
                    cb.isNull(root.get("deletedAt")));

            // Tim theo tu khoa
            if (keyword != null && !keyword.isBlank()) {
                String pattern = "%" +
                        keyword.trim().toLowerCase(Locale.ROOT) +
                        "%";

                var keywordCondition = cb.or(
                        cb.like(cb.lower(root.get("title")), pattern),
                        cb.like(cb.lower(root.get("description")), pattern),
                        cb.like(cb.lower(root.get("address")), pattern)
                );

                conditions = cb.and(
                        conditions, keywordCondition);
            }

            // Loc theo khu vuc
            if (area != null && !area.isBlank()) {
                String pattern = "%" +
                        area.trim().toLowerCase(Locale.ROOT) +
                        "%";

                conditions = cb.and(conditions,
                        cb.like(cb.lower(root.get("areaName")),
                                pattern));
            }

            // Loc gia
            if (minPrice != null) {
                conditions = cb.and(conditions,
                        cb.greaterThanOrEqualTo(
                                root.get("price"), minPrice));
            }

            if (maxPrice != null) {
                conditions = cb.and(conditions,
                        cb.lessThanOrEqualTo(
                                root.get("price"), maxPrice));
            }

            // Loc dien tich
            if (minArea != null) {
                conditions = cb.and(conditions,
                        cb.greaterThanOrEqualTo(
                                root.get("areaSqm"), minArea));
            }

            if (maxArea != null) {
                conditions = cb.and(conditions,
                        cb.lessThanOrEqualTo(
                                root.get("areaSqm"), maxArea));
            }

            return conditions;
        };
    }
}
