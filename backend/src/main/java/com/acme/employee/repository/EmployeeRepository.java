package com.acme.employee.repository;

import org.springframework.data.mongodb.repository.ReactiveMongoRepository;
import org.springframework.stereotype.Repository;

import com.acme.employee.model.EmployeeDocument;

@Repository
public interface EmployeeRepository extends ReactiveMongoRepository<EmployeeDocument, String> {
}

