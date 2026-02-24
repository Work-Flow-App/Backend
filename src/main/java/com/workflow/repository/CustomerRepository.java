package com.workflow.repository;

import com.workflow.entity.Customer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CustomerRepository extends JpaRepository<Customer, Long> {
    List<Customer> findByCompanyId(Long companyId);

    boolean existsByCompanyIdAndName(Long companyId, String name);
    boolean existsByCompanyIdAndNameAndIdNot(Long companyId, String name, Long id);
    boolean existsByCompanyIdAndEmail(Long companyId, String email);
    boolean existsByCompanyIdAndEmailAndIdNot(Long companyId, String email, Long id);
}