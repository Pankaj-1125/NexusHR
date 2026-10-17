package com.nexushr.service;

import com.nexushr.entity.Employee;
import com.nexushr.entity.PerformanceGoal;
import com.nexushr.repository.EmployeeRepository;
import com.nexushr.repository.PerformanceGoalRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
public class PerformanceGoalService {
    private final PerformanceGoalRepository goalRepository;
    private final EmployeeRepository employeeRepository;

    public PerformanceGoalService(PerformanceGoalRepository goalRepository, EmployeeRepository employeeRepository) {
        this.goalRepository = goalRepository;
        this.employeeRepository = employeeRepository;
    }

    public List<PerformanceGoal> getAll() { return goalRepository.findAll(); }
    public List<PerformanceGoal> getByEmployee(Long employeeId) { return goalRepository.findByEmployeeIdOrderByTargetDateAsc(employeeId); }

    @Transactional
    public PerformanceGoal create(PerformanceGoal goal) {
        validate(goal);
        Employee employee = employeeRepository.findById(goal.getEmployee().getId())
                .orElseThrow(() -> new RuntimeException("Employee not found."));
        goal.setEmployee(employee);
        return goalRepository.save(goal);
    }

    @Transactional
    public PerformanceGoal update(Long id, PerformanceGoal updated) {
        validate(updated);
        PerformanceGoal existing = goalRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Performance goal not found with id: " + id));
        Employee employee = employeeRepository.findById(updated.getEmployee().getId())
                .orElseThrow(() -> new RuntimeException("Employee not found."));
        existing.setEmployee(employee);
        existing.setTitle(updated.getTitle());
        existing.setDescription(updated.getDescription());
        existing.setSuccessMetric(updated.getSuccessMetric());
        existing.setStartDate(updated.getStartDate());
        existing.setTargetDate(updated.getTargetDate());
        existing.setProgress(updated.getProgress());
        existing.setWeight(updated.getWeight());
        existing.setStatus(updated.getStatus());
        existing.setManagerComment(updated.getManagerComment());
        existing.setEmployeeComment(updated.getEmployeeComment());
        return goalRepository.save(existing);
    }

    @Transactional
    public PerformanceGoal updateProgress(Long id, int progress, String employeeComment) {
        PerformanceGoal goal = goalRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Performance goal not found with id: " + id));
        if (progress < 0 || progress > 100) throw new IllegalArgumentException("Progress must be between 0 and 100.");
        goal.setProgress(progress);
        if (employeeComment != null) goal.setEmployeeComment(employeeComment.trim());
        if (progress == 100) goal.setStatus(PerformanceGoal.GoalStatus.COMPLETED);
        else if (goal.getStatus() == PerformanceGoal.GoalStatus.DRAFT) goal.setStatus(PerformanceGoal.GoalStatus.ACTIVE);
        return goalRepository.save(goal);
    }

    @Transactional
    public void delete(Long id) {
        if (!goalRepository.existsById(id)) throw new RuntimeException("Performance goal not found with id: " + id);
        goalRepository.deleteById(id);
    }

    private void validate(PerformanceGoal goal) {
        if (goal == null) throw new IllegalArgumentException("Performance goal is required.");
        if (goal.getEmployee() == null || goal.getEmployee().getId() == null) throw new IllegalArgumentException("Employee is required.");
        if (goal.getTitle() == null || goal.getTitle().isBlank()) throw new IllegalArgumentException("Goal title is required.");
        if (goal.getStartDate() == null || goal.getTargetDate() == null) throw new IllegalArgumentException("Goal start and target dates are required.");
        if (goal.getTargetDate().isBefore(goal.getStartDate())) throw new IllegalArgumentException("Target date cannot be before start date.");
        int progress = goal.getProgress() == null ? 0 : goal.getProgress();
        int weight = goal.getWeight() == null ? 100 : goal.getWeight();
        if (progress < 0 || progress > 100) throw new IllegalArgumentException("Progress must be between 0 and 100.");
        if (weight < 1 || weight > 100) throw new IllegalArgumentException("Weight must be between 1 and 100.");
        goal.setProgress(progress);
        goal.setWeight(weight);
        if (goal.getStatus() == null) goal.setStatus(PerformanceGoal.GoalStatus.DRAFT);
    }
}
