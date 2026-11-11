package com.hoakhanh.rental.service;

import com.hoakhanh.rental.dto.ContactRequestResponse;
import com.hoakhanh.rental.dto.CreateContactRequest;
import com.hoakhanh.rental.dto.UpdateContactRequestStatusRequest;
import com.hoakhanh.rental.entity.ContactRequest;
import com.hoakhanh.rental.entity.ContactRequestStatus;
import com.hoakhanh.rental.entity.ModerationStatus;
import com.hoakhanh.rental.entity.OccupancyStatus;
import com.hoakhanh.rental.entity.Role;
import com.hoakhanh.rental.entity.Room;
import com.hoakhanh.rental.entity.User;
import com.hoakhanh.rental.entity.VisibilityStatus;
import com.hoakhanh.rental.repository.ContactRequestRepository;
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
class ContactRequestServiceTest {

    @Mock
    private ContactRequestRepository contactRequestRepository;

    @Mock
    private RoomRepository roomRepository;

    @Mock
    private UserRepository userRepository;

    private ContactRequestService contactRequestService;

    @BeforeEach
    void setUp() {
        contactRequestService = new ContactRequestService(
                contactRequestRepository,
                roomRepository,
                userRepository);
    }

    private Jwt jwt(String subject) {
        Jwt jwt = mock(Jwt.class);
        when(jwt.getSubject()).thenReturn(subject);
        return jwt;
    }

    private User user(Long id, Role role) {
        User user = new User();
        ReflectionTestUtils.setField(user, "id", id);
        user.setFullName(
                role == Role.LANDLORD ? "Chu tro" : "Sinh vien");
        user.setEmail("test@example.com");
        user.setPhone("0900000000");
        user.setRole(role);
        user.setActive(true);
        return user;
    }

    private Room publicRoom(User landlord) {
        Room room = new Room();
        ReflectionTestUtils.setField(room, "id", 10L);
        room.setLandlord(landlord);
        room.setTitle("Phong tro Hoa Khanh");
        room.setModerationStatus(ModerationStatus.APPROVED);
        room.setOccupancyStatus(OccupancyStatus.AVAILABLE);
        room.setVisibilityStatus(VisibilityStatus.VISIBLE);
        return room;
    }

    private ContactRequest contactRequest(Room room, User student) {
        ContactRequest request = new ContactRequest();
        ReflectionTestUtils.setField(request, "id", 100L);
        request.setStudent(student);
        request.setRoom(room);
        request.setMessage("Toi muon xem phong");
        request.setStatus(ContactRequestStatus.PENDING);
        return request;
    }

    @Test
    void create_byStudentToPublicRoom_createsPendingRequest() {
        User student = user(3L, Role.STUDENT);
        User landlord = user(1L, Role.LANDLORD);
        Room room = publicRoom(landlord);

        when(userRepository.findById(3L))
                .thenReturn(Optional.of(student));
        when(roomRepository.findById(10L))
                .thenReturn(Optional.of(room));
        when(contactRequestRepository
                .existsByStudent_IdAndRoom_IdAndStatus(
                        3L, 10L, ContactRequestStatus.PENDING))
                .thenReturn(false);
        when(contactRequestRepository.save(
                any(ContactRequest.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        ContactRequestResponse response =
                contactRequestService.createContactRequest(
                        10L,
                        new CreateContactRequest(
                                "  Toi muon xem phong  "),
                        jwt("3"));

        assertThat(response.status())
                .isEqualTo(ContactRequestStatus.PENDING);
        assertThat(response.message())
                .isEqualTo("Toi muon xem phong");
        assertThat(response.roomId()).isEqualTo(10L);
    }

    @Test
    void create_withBlankMessage_storesNull() {
        User student = user(3L, Role.STUDENT);
        User landlord = user(1L, Role.LANDLORD);
        Room room = publicRoom(landlord);

        when(userRepository.findById(3L))
                .thenReturn(Optional.of(student));
        when(roomRepository.findById(10L))
                .thenReturn(Optional.of(room));
        when(contactRequestRepository
                .existsByStudent_IdAndRoom_IdAndStatus(
                        3L, 10L, ContactRequestStatus.PENDING))
                .thenReturn(false);
        when(contactRequestRepository.save(
                any(ContactRequest.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        ContactRequestResponse response =
                contactRequestService.createContactRequest(
                        10L,
                        new CreateContactRequest("   "),
                        jwt("3"));

        assertThat(response.message()).isNull();
        assertThat(response.status())
                .isEqualTo(ContactRequestStatus.PENDING);
    }

    @Test
    void create_duplicatePendingRequest_throwsConflict() {
        User student = user(3L, Role.STUDENT);
        User landlord = user(1L, Role.LANDLORD);

        when(userRepository.findById(3L))
                .thenReturn(Optional.of(student));
        when(roomRepository.findById(10L))
                .thenReturn(Optional.of(publicRoom(landlord)));
        when(contactRequestRepository
                .existsByStudent_IdAndRoom_IdAndStatus(
                        3L, 10L, ContactRequestStatus.PENDING))
                .thenReturn(true);

        assertThatThrownBy(
                () -> contactRequestService.createContactRequest(
                        10L,
                        new CreateContactRequest("Xin chao"),
                        jwt("3")))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> assertThat(
                        ((ResponseStatusException) ex)
                                .getStatusCode().value())
                        .isEqualTo(HttpStatus.CONFLICT.value()));
    }

    @Test
    void create_toUnapprovedRoom_throwsNotFound() {
        User student = user(3L, Role.STUDENT);
        User landlord = user(1L, Role.LANDLORD);
        Room room = publicRoom(landlord);
        room.setModerationStatus(ModerationStatus.PENDING);

        when(userRepository.findById(3L))
                .thenReturn(Optional.of(student));
        when(roomRepository.findById(10L))
                .thenReturn(Optional.of(room));

        assertThatThrownBy(
                () -> contactRequestService.createContactRequest(
                        10L,
                        new CreateContactRequest("Xin chao"),
                        jwt("3")))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> assertThat(
                        ((ResponseStatusException) ex)
                                .getStatusCode().value())
                        .isEqualTo(HttpStatus.NOT_FOUND.value()));
    }

    @Test
    void create_byLandlord_throwsForbidden() {
        User landlord = user(1L, Role.LANDLORD);

        when(userRepository.findById(1L))
                .thenReturn(Optional.of(landlord));

        assertThatThrownBy(
                () -> contactRequestService.createContactRequest(
                        10L,
                        new CreateContactRequest("Xin chao"),
                        jwt("1")))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> assertThat(
                        ((ResponseStatusException) ex)
                                .getStatusCode().value())
                        .isEqualTo(HttpStatus.FORBIDDEN.value()));
    }

    @Test
    void updateStatus_byOwningLandlord_pendingToResponded_succeeds() {
        User landlord = user(1L, Role.LANDLORD);
        User student = user(3L, Role.STUDENT);
        ContactRequest request =
                contactRequest(publicRoom(landlord), student);

        when(userRepository.findById(1L))
                .thenReturn(Optional.of(landlord));
        when(contactRequestRepository
                .findByIdAndRoom_Landlord_Id(100L, 1L))
                .thenReturn(Optional.of(request));
        when(contactRequestRepository.save(
                any(ContactRequest.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        ContactRequestResponse response =
                contactRequestService.updateContactRequestStatus(
                        100L,
                        new UpdateContactRequestStatusRequest(
                                ContactRequestStatus.RESPONDED),
                        jwt("1"));

        assertThat(response.status())
                .isEqualTo(ContactRequestStatus.RESPONDED);
    }

    @Test
    void updateStatus_byDifferentLandlord_throwsNotFound() {
        User otherLandlord = user(2L, Role.LANDLORD);

        when(userRepository.findById(2L))
                .thenReturn(Optional.of(otherLandlord));
        when(contactRequestRepository
                .findByIdAndRoom_Landlord_Id(100L, 2L))
                .thenReturn(Optional.empty());

        assertThatThrownBy(
                () -> contactRequestService.updateContactRequestStatus(
                        100L,
                        new UpdateContactRequestStatusRequest(
                                ContactRequestStatus.RESPONDED),
                        jwt("2")))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> assertThat(
                        ((ResponseStatusException) ex)
                                .getStatusCode().value())
                        .isEqualTo(HttpStatus.NOT_FOUND.value()));
    }

    @Test
    void updateStatus_closedToResponded_throwsConflict() {
        User landlord = user(1L, Role.LANDLORD);
        User student = user(3L, Role.STUDENT);
        ContactRequest request =
                contactRequest(publicRoom(landlord), student);
        request.setStatus(ContactRequestStatus.CLOSED);

        when(userRepository.findById(1L))
                .thenReturn(Optional.of(landlord));
        when(contactRequestRepository
                .findByIdAndRoom_Landlord_Id(100L, 1L))
                .thenReturn(Optional.of(request));

        assertThatThrownBy(
                () -> contactRequestService.updateContactRequestStatus(
                        100L,
                        new UpdateContactRequestStatusRequest(
                                ContactRequestStatus.RESPONDED),
                        jwt("1")))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> assertThat(
                        ((ResponseStatusException) ex)
                                .getStatusCode().value())
                        .isEqualTo(HttpStatus.CONFLICT.value()));
    }
}
