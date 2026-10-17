package com.nexushr.controller;

import com.nexushr.entity.PerformanceGoal;
import com.nexushr.service.PerformanceGoalService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/performance-goals")
public class PerformanceGoalController {
    private final PerformanceGoalService goalService;
    public PerformanceGoalController(PerformanceGoalService goalService){this.goalService=goalService;}

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN','HR','MANAGER')")
    public ResponseEntity<List<PerformanceGoal>> getAll(){return ResponseEntity.ok(goalService.getAll());}

    @GetMapping("/employee/{employeeId}")
    @PreAuthorize("hasAnyRole('ADMIN','HR','MANAGER')")
    public ResponseEntity<List<PerformanceGoal>> getByEmployee(@PathVariable Long employeeId){return ResponseEntity.ok(goalService.getByEmployee(employeeId));}

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN','HR','MANAGER')")
    public ResponseEntity<PerformanceGoal> create(@RequestBody PerformanceGoal goal){return ResponseEntity.ok(goalService.create(goal));}

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','HR','MANAGER')")
    public ResponseEntity<PerformanceGoal> update(@PathVariable Long id,@RequestBody PerformanceGoal goal){return ResponseEntity.ok(goalService.update(id,goal));}

    @PatchMapping("/{id}/progress")
    @PreAuthorize("hasAnyRole('ADMIN','HR','MANAGER')")
    public ResponseEntity<PerformanceGoal> progress(@PathVariable Long id,@RequestBody ProgressRequest request){return ResponseEntity.ok(goalService.updateProgress(id,request.progress(),request.employeeComment()));}

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','HR')")
    public ResponseEntity<Void> delete(@PathVariable Long id){goalService.delete(id);return ResponseEntity.noContent().build();}

    public record ProgressRequest(int progress,String employeeComment){}
}
