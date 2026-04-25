package com.cinema.ticket_booking.repository;

import com.cinema.ticket_booking.model.PricingRule;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface PricingRuleRepository extends JpaRepository<PricingRule, UUID> {
    Page<PricingRule> findAllByOrderByPriorityAsc(Pageable pageable);
    List<PricingRule> findByIsActiveTrueOrderByPriorityAsc();
}
