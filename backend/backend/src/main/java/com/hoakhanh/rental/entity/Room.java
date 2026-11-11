
package com.hoakhanh.rental.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.OffsetDateTime;

@Entity
@Table(name = "rooms")
public class Room {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "landlord_id", nullable = false)
    private User landlord;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(nullable = false, length = 255)
    private String address;

    @Column(name = "area_name", length = 150)
    private String areaName;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal price;

    @Column(name = "area_sqm", nullable = false,
            precision = 8, scale = 2)
    private BigDecimal areaSqm;

    @Column(name = "max_occupants", nullable = false)
    private Integer maxOccupants = 1;

    @Enumerated(EnumType.STRING)
    @Column(name = "moderation_status", nullable = false)
    private ModerationStatus moderationStatus =
            ModerationStatus.PENDING;

    @Column(name = "rejection_reason",
            columnDefinition = "TEXT",
            nullable = true)
    private String rejectionReason;

    @Enumerated(EnumType.STRING)
    @Column(name = "occupancy_status", nullable = false)
    private OccupancyStatus occupancyStatus =
            OccupancyStatus.AVAILABLE;

    @Enumerated(EnumType.STRING)
    @Column(name = "visibility_status", nullable = false)
    private VisibilityStatus visibilityStatus =
            VisibilityStatus.VISIBLE;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    @Column(name = "deleted_at")
    private OffsetDateTime deletedAt;

    public Room() {
    }

    @PrePersist
    public void onCreate() {
        OffsetDateTime now = OffsetDateTime.now();
        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    public void onUpdate() {
        updatedAt = OffsetDateTime.now();
    }

    public Long getId() {
        return id;
    }

    public User getLandlord() {
        return landlord;
    }

    public void setLandlord(User landlord) {
        this.landlord = landlord;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public String getAreaName() {
        return areaName;
    }

    public void setAreaName(String areaName) {
        this.areaName = areaName;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public void setPrice(BigDecimal price) {
        this.price = price;
    }

    public BigDecimal getAreaSqm() {
        return areaSqm;
    }

    public void setAreaSqm(BigDecimal areaSqm) {
        this.areaSqm = areaSqm;
    }

    public Integer getMaxOccupants() {
        return maxOccupants;
    }

    public void setMaxOccupants(Integer maxOccupants) {
        this.maxOccupants = maxOccupants;
    }

    public ModerationStatus getModerationStatus() {
        return moderationStatus;
    }

    public void setModerationStatus(
            ModerationStatus moderationStatus) {
        this.moderationStatus = moderationStatus;
    }

    public String getRejectionReason() {
        return rejectionReason;
    }

    public void setRejectionReason(String rejectionReason) {
        this.rejectionReason = rejectionReason;
    }

    public OccupancyStatus getOccupancyStatus() {
        return occupancyStatus;
    }

    public void setOccupancyStatus(
            OccupancyStatus occupancyStatus) {
        this.occupancyStatus = occupancyStatus;
    }

    public VisibilityStatus getVisibilityStatus() {
        return visibilityStatus;
    }

    public void setVisibilityStatus(
            VisibilityStatus visibilityStatus) {
        this.visibilityStatus = visibilityStatus;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public OffsetDateTime getUpdatedAt() {
        return updatedAt;
    }

    public OffsetDateTime getDeletedAt() {
        return deletedAt;
    }

    public void setDeletedAt(OffsetDateTime deletedAt) {
        this.deletedAt = deletedAt;
    }
}
