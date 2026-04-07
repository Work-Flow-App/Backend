package com.workflow.repository.common;

import org.springframework.data.jpa.repository.JpaRepository;

import com.workflow.entity.common.Address;

public interface AddressRepository extends JpaRepository<Address, Long> {
}
