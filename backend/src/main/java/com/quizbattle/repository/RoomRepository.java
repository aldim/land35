package com.quizbattle.repository;

import com.quizbattle.model.entity.RoomEntity;
import com.quizbattle.model.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface RoomRepository extends JpaRepository<RoomEntity, Long> {
    Optional<RoomEntity> findByCode(String code);
    List<RoomEntity> findByHostUser(User hostUser);
    boolean existsByCode(String code);
}

