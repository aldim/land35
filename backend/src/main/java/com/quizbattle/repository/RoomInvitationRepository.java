package com.quizbattle.repository;

import com.quizbattle.model.entity.RoomEntity;
import com.quizbattle.model.entity.RoomInvitation;
import com.quizbattle.model.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface RoomInvitationRepository extends JpaRepository<RoomInvitation, Long> {
    List<RoomInvitation> findByInvitedUserAndStatus(User invitedUser, RoomInvitation.InvitationStatus status);
    List<RoomInvitation> findByRoomAndStatus(RoomEntity room, RoomInvitation.InvitationStatus status);
    Optional<RoomInvitation> findByRoomAndInvitedUser(RoomEntity room, User invitedUser);
    List<RoomInvitation> findByRoom(RoomEntity room);
}

