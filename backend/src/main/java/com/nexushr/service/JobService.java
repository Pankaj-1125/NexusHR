package com.nexushr.service;

import com.nexushr.entity.Job;
import com.nexushr.repository.JobRepository;

import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class JobService {

    private final JobRepository jobRepository;

    public JobService(JobRepository jobRepository) {
        this.jobRepository = jobRepository;
    }

    public List<Job> getAllJobs() {
        return jobRepository.findAll();
    }

    public Optional<Job> getJobById(Long id) {
        return jobRepository.findById(id);
    }

    public List<Job> getJobsByStatus(Job.JobStatus status) {
        return jobRepository.findByStatus(status);
    }

    public List<Job> getJobsByDepartment(String department) {
        return jobRepository.findByDepartmentIgnoreCase(department);
    }

    public List<Job> searchJobsByTitle(String title) {
        return jobRepository.findByTitleContainingIgnoreCase(title);
    }

    public Job createJob(Job job) {
        validateJob(job);
        return jobRepository.save(job);
    }

    public Job updateJob(Long id, Job updatedJob) {
        validateJob(updatedJob);

        return jobRepository.findById(id)
                .map(existingJob -> {
                    existingJob.setTitle(updatedJob.getTitle());
                    existingJob.setDepartment(updatedJob.getDepartment());
                    existingJob.setLocation(updatedJob.getLocation());
                    existingJob.setEmploymentType(updatedJob.getEmploymentType());
                    existingJob.setDescription(updatedJob.getDescription());
                    existingJob.setRequirements(updatedJob.getRequirements());
                    existingJob.setSalaryMin(updatedJob.getSalaryMin());
                    existingJob.setSalaryMax(updatedJob.getSalaryMax());
                    existingJob.setStatus(updatedJob.getStatus());
                    existingJob.setPostedDate(updatedJob.getPostedDate());
                    existingJob.setClosingDate(updatedJob.getClosingDate());

                    return jobRepository.save(existingJob);
                })
                .orElseThrow(() ->
                        new RuntimeException(
                                "Job not found with id: " + id
                        )
                );
    }

    public void deleteJob(Long id) {
        jobRepository.deleteById(id);
    }

    private void validateJob(Job job) {

        if (job.getTitle() == null || job.getTitle().isBlank()) {
            throw new IllegalArgumentException("Job title is required.");
        }

        if (job.getDepartment() == null || job.getDepartment().isBlank()) {
            throw new IllegalArgumentException("Department is required.");
        }

        if (job.getLocation() == null || job.getLocation().isBlank()) {
            throw new IllegalArgumentException("Location is required.");
        }

        if (job.getEmploymentType() == null) {
            throw new IllegalArgumentException(
                    "Employment type is required."
            );
        }

        if (job.getStatus() == null) {
            throw new IllegalArgumentException(
                    "Job status is required."
            );
        }

        if (job.getPostedDate() == null) {
            throw new IllegalArgumentException(
                    "Posted date is required."
            );
        }

        if (job.getSalaryMin() != null
                && job.getSalaryMax() != null
                && job.getSalaryMin().compareTo(job.getSalaryMax()) > 0) {

            throw new IllegalArgumentException(
                    "Minimum salary cannot be greater than maximum salary."
            );
        }

        if (job.getClosingDate() != null
                && job.getClosingDate().isBefore(job.getPostedDate())) {

            throw new IllegalArgumentException(
                    "Closing date cannot be before posted date."
            );
        }
    }
}