package com.aivle.sellon.domain.proposal.repository;

import com.aivle.sellon.domain.proposal.entity.Proposal;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface ProposalRepository extends JpaRepository<Proposal, Long> {
    // 같은 회사 소속이면 루트/일반 계정 상관없이 조회 가능하도록 company 기준으로 스코핑
    List<Proposal> findByRootUser_Company_Id(Long companyId);

    // alert_id는 회사별로 겹칠 수 있어(product_group_id가 회사별 시퀀스) company_id와 함께 조회한다.
    Optional<Proposal> findByAlertIdAndCompanyId(String alertId, Long companyId);
}
