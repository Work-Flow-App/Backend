package com.workflow.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.workflow.entity.Address;

public interface AddressRepository extends JpaRepository<Address, Long> {
}
