package com.yusuf.jwt_demo.violation;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ViolationRepository extends JpaRepository<Violation, Long> {

    // Metot adından SQL üretilir: SELECT ... FROM violations WHERE plate = ?
    List<Violation> findByPlate(String plate);
}
