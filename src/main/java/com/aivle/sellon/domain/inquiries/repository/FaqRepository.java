package com.aivle.sellon.domain.inquiries.repository;

import com.aivle.sellon.domain.inquiries.entity.Faq;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FaqRepository extends JpaRepository<Faq, Long> {
}
