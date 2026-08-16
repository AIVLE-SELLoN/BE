package com.aivle.sellon.rawdb.repository;

import com.aivle.sellon.rawdb.entity.ClassifiedItemAspect;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

public interface ClassifiedItemAspectRepository extends JpaRepository<ClassifiedItemAspect, Long> {
    // item_id 목록(cs.id/reviews.id 기준)에 대한 분류 결과 조회. TODO: IN 절 크기 커지면 페이징 필요
    @Transactional(transactionManager = "rawDbTransactionManager", readOnly = true)
    List<ClassifiedItemAspect> findByItemIdIn(List<String> itemIds);
}
