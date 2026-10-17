package com.nexushr.repository;

import com.nexushr.entity.Job;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface JobRepository extends JpaRepository<Job, Long> {

    List<Job> findByStatus(Job.JobStatus status);

    List<Job> findByDepartmentIgnoreCase(String department);

    List<Job> findByTitleContainingIgnoreCase(String title);
}