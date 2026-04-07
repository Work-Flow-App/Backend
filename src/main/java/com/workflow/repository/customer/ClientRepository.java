package com.workflow.repository.customer;

import com.workflow.entity.customer.Client;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ClientRepository extends JpaRepository<Client, Long> {
    List<Client> findByCompanyId(Long companyId);
}