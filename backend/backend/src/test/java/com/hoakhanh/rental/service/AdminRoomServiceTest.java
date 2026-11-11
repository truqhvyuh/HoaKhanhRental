package com.hoakhanh.rental.service;

import com.hoakhanh.rental.dto.AdminRoomResponse;
import com.hoakhanh.rental.entity.ModerationStatus;
import com.hoakhanh.rental.entity.OccupancyStatus;
import com.hoakhanh.rental.entity.Role;
import com.hoakhanh.rental.entity.Room;
import com.hoakhanh.rental.entity.User;
import com.hoakhanh.rental.entity.VisibilityStatus;
import com.hoakhanh.rental.repository.RoomRepository;
import com.hoakhanh.rental.repository.UserRepository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminRoomServiceTest {

    @Mock
    private RoomRepository roomRepository;

    @Mock
    private UserRepository userRepository;

    private AdminRoomService adminRoomService;

    @BeforeEach
    void setUp() {
        adminRoomService = new AdminRoomService(
                roomRepository, userRepository);
    }

    private Jwt jwt(String subject) {
        Jwt jwt = mock(Jwt.class);
        when(jwt.getSubject()).thenReturn(subject);
        return jwt;
    }

    private User user(Long id, Role role) {
        User user = new User();
        ReflectionTestUtils.setField(user, "id", id);
        user.setFullName("Test User");
        user.setEmail("test@example.com");
        user.setRole(role);
        user.setActive(true);
        return user;
    }

    private Room pendingRoom() {
        Room room = new Room();
        ReflectionTestUtils.setField(room, "id", 10L);
        room.setLandlord(user(7L, Role.LANDLORD));
        room.setTitle("Phong tro Hoa Khanh");
        room.setModerationStatus(ModerationStatus.PENDING);
        room.setRejectionReason("Anh chua ro");
        room.setOccupancyStatus(OccupancyStatus.AVAILABLE);
        room.setVisibilityStatus(VisibilityStatus.VISIBLE);
        return room;
    }

    @Test
    void approve_byStudent_throwsForbidden() {
        when(userRepository.findById(2L))
                .thenReturn(Optional.of(user(2L, Role.STUDENT)));

        assertThatThrownBy(
                () -> adminRoomService.approveRoom(10L, jwt("2")))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> assertThat(
                        ((ResponseStatusException) ex)
                                .getStatusCode().value())
                        .isEqualTo(HttpStatus.FORBIDDEN.value()));
    }

    @Test
    void approve_pendingRoom_approvesAndClearsRejectionReason() {
        when(userRepository.findById(1L))
                .thenReturn(Optional.of(user(1L, Role.ADMIN)));
        when(roomRepository.findById(10L))
                .thenReturn(Optional.of(pendingRoom()));
        when(roomRepository.save(any(Room.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        AdminRoomResponse response =
                adminRoomService.approveRoom(10L, jwt("1"));

        assertThat(response.moderationStatus())
                .isEqualTo("APPROVED");
        assertThat(response.rejectionReason()).isNull();
    }

    @Test
    void reject_withReason_setsRejectedAndTrimsReason() {
        when(userRepository.findById(1L))
                .thenReturn(Optional.of(user(1L, Role.ADMIN)));
        when(roomRepository.findById(10L))
                .thenReturn(Optional.of(pendingRoom()));
        when(roomRepository.save(any(Room.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        AdminRoomResponse response = adminRoomService.rejectRoom(
                10L,
                "  Anh mo, thieu thong tin dien nuoc  ",
                jwt("1"));

        assertThat(response.moderationStatus())
                .isEqualTo("REJECTED");
        assertThat(response.rejectionReason())
                .isEqualTo("Anh mo, thieu thong tin dien nuoc");
    }

    @Test
    void reject_nonPendingRoom_throwsConflict() {
        Room room = pendingRoom();
        room.setModerationStatus(ModerationStatus.APPROVED);

        when(userRepository.findById(1L))
                .thenReturn(Optional.of(user(1L, Role.ADMIN)));
        when(roomRepository.findById(10L))
                .thenReturn(Optional.of(room));

        assertThatThrownBy(() -> adminRoomService.rejectRoom(
                        10L, "Ly do", jwt("1")))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> assertThat(
                        ((ResponseStatusException) ex)
                                .getStatusCode().value())
                        .isEqualTo(HttpStatus.CONFLICT.value()));
    }

    @Test
    void reject_missingRoom_throwsNotFound() {
        when(userRepository.findById(1L))
                .thenReturn(Optional.of(user(1L, Role.ADMIN)));
        when(roomRepository.findById(99L))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> adminRoomService.rejectRoom(
                        99L, "Ly do", jwt("1")))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> assertThat(
                        ((ResponseStatusException) ex)
                                .getStatusCode().value())
                        .isEqualTo(HttpStatus.NOT_FOUND.value()));
    }
}
