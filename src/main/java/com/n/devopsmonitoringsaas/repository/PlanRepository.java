package com.n.devopsmonitoringsaas.repository;

import com.n.devopsmonitoringsaas.entity.Plan;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PlanRepository extends JpaRepository<Plan, Long> {

    Optional<Plan> findFirstByOrderByIdAsc();
}
