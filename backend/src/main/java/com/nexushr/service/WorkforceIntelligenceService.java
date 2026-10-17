package com.nexushr.service;

import com.nexushr.dto.WorkforceIntelligenceDTO;
import com.nexushr.entity.Attendance;
import com.nexushr.entity.Employee;
import com.nexushr.entity.Leave;
import com.nexushr.entity.PerformanceReview;
import com.nexushr.repository.AttendanceRepository;
import com.nexushr.repository.EmployeeRepository;
import com.nexushr.repository.LeaveRepository;
import com.nexushr.repository.PerformanceReviewRepository;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
public class WorkforceIntelligenceService {

    private static final String MODEL_VERSION = "NexusHR Explainable Workforce Engine v2.0";

    private final EmployeeRepository employeeRepository;
    private final AttendanceRepository attendanceRepository;
    private final LeaveRepository leaveRepository;
    private final PerformanceReviewRepository performanceReviewRepository;
    private final AuditLogService auditLogService;

    public WorkforceIntelligenceService(
            EmployeeRepository employeeRepository,
            AttendanceRepository attendanceRepository,
            LeaveRepository leaveRepository,
            PerformanceReviewRepository performanceReviewRepository,
            AuditLogService auditLogService
    ) {
        this.employeeRepository = employeeRepository;
        this.attendanceRepository = attendanceRepository;
        this.leaveRepository = leaveRepository;
        this.performanceReviewRepository = performanceReviewRepository;
        this.auditLogService = auditLogService;
    }

    public WorkforceIntelligenceDTO analyzeEmployee(Long employeeId) {
        Employee employee = employeeRepository.findById(employeeId)
                .orElseThrow(() -> new RuntimeException("Employee not found with ID: " + employeeId));

        WorkforceIntelligenceDTO result = calculateIntelligence(
                employee,
                attendanceRepository.findByEmployeeId(employeeId),
                leaveRepository.findByEmployeeId(employeeId),
                performanceReviewRepository.findByEmployeeId(employeeId)
        );

        auditLogService.log(
                "WORKFORCE_INTELLIGENCE_ANALYZED",
                "EMPLOYEE",
                employeeId,
                "Workforce intelligence analysis generated"
        );

        return result;
    }

    public List<WorkforceIntelligenceDTO> analyzeAllEmployees() {
        List<WorkforceIntelligenceDTO> results = employeeRepository.findAll()
                .stream()
                .filter(this::isActiveEmployee)
                .map(employee -> calculateIntelligence(
                        employee,
                        attendanceRepository.findByEmployeeId(employee.getId()),
                        leaveRepository.findByEmployeeId(employee.getId()),
                        performanceReviewRepository.findByEmployeeId(employee.getId())
                ))
                .sorted(
                        Comparator.comparing(WorkforceIntelligenceDTO::getAttritionRiskScore)
                                .thenComparing(WorkforceIntelligenceDTO::getBurnoutRiskScore)
                                .reversed()
                )
                .toList();

        auditLogService.log(
                "WORKFORCE_INTELLIGENCE_ANALYZED_ALL",
                "WORKFORCE",
                0L,
                "Workforce intelligence analysis generated for " + results.size() + " active employee(s)"
        );

        return results;
    }

    public Map<String, Object> getWorkforceSummary() {
        List<WorkforceIntelligenceDTO> rows = analyzeAllEmployees();

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("totalEmployees", rows.size());
        response.put("highRisk", countLevel(rows, WorkforceIntelligenceDTO::getAttritionRiskLevel, "HIGH"));
        response.put("mediumRisk", countLevel(rows, WorkforceIntelligenceDTO::getAttritionRiskLevel, "MEDIUM"));
        response.put("lowRisk", countLevel(rows, WorkforceIntelligenceDTO::getAttritionRiskLevel, "LOW"));
        response.put("lowEngagement", countLevel(rows, WorkforceIntelligenceDTO::getEngagementLevel, "LOW"));
        response.put("highBurnoutRisk", countLevel(rows, WorkforceIntelligenceDTO::getBurnoutRiskLevel, "HIGH"));
        response.put("highPotential", countLevel(rows, WorkforceIntelligenceDTO::getGrowthPotentialLevel, "HIGH"));
        response.put("urgentActions", rows.stream().filter(row -> "URGENT".equalsIgnoreCase(row.getPriority())).count());
        response.put("averageEngagement", roundedAverage(rows.stream().mapToInt(WorkforceIntelligenceDTO::getEngagementScore).average().orElse(0)));
        response.put("averageRisk", roundedAverage(rows.stream().mapToInt(WorkforceIntelligenceDTO::getAttritionRiskScore).average().orElse(0)));
        response.put("averageBurnoutRisk", roundedAverage(rows.stream().mapToInt(WorkforceIntelligenceDTO::getBurnoutRiskScore).average().orElse(0)));
        response.put("averageGrowthPotential", roundedAverage(rows.stream().mapToInt(WorkforceIntelligenceDTO::getGrowthPotentialScore).average().orElse(0)));
        response.put("averageConfidence", roundedAverage(rows.stream().mapToInt(WorkforceIntelligenceDTO::getConfidenceScore).average().orElse(0)));
        response.put("averageAttendance", roundOne(rows.stream().mapToDouble(WorkforceIntelligenceDTO::getAttendanceRate).average().orElse(0)));
        response.put("averagePerformanceRating", roundOne(rows.stream().mapToDouble(WorkforceIntelligenceDTO::getAveragePerformanceRating).average().orElse(0)));
        response.put("averageSkillGap", roundedAverage(rows.stream().mapToInt(WorkforceIntelligenceDTO::getSkillGapScore).average().orElse(0)));
        response.put("employeesWithPerformanceData", rows.stream().filter(row -> row.getPerformanceReviewCount() > 0).count());
        response.put("employeesWithAttendanceData", rows.stream().filter(row -> row.getAttendanceRecords() > 0).count());
        int averageEngagement = roundedAverage(rows.stream().mapToInt(WorkforceIntelligenceDTO::getEngagementScore).average().orElse(0));
        int averageRisk = roundedAverage(rows.stream().mapToInt(WorkforceIntelligenceDTO::getAttritionRiskScore).average().orElse(0));
        int averageBurnout = roundedAverage(rows.stream().mapToInt(WorkforceIntelligenceDTO::getBurnoutRiskScore).average().orElse(0));
        int workforceHealth = clamp((averageEngagement + (100 - averageRisk) + (100 - averageBurnout)) / 3);
        response.put("workforceHealth", workforceHealth);

        Map<String, Long> departmentPopulation = rows.stream().collect(Collectors.groupingBy(
                row -> displayDepartment(row.getDepartment()),
                LinkedHashMap::new,
                Collectors.counting()
        ));
        response.put("departmentPopulation", departmentPopulation);

        Map<String, Long> riskDistribution = new LinkedHashMap<>();
        riskDistribution.put("HIGH", countLevel(rows, WorkforceIntelligenceDTO::getAttritionRiskLevel, "HIGH"));
        riskDistribution.put("MEDIUM", countLevel(rows, WorkforceIntelligenceDTO::getAttritionRiskLevel, "MEDIUM"));
        riskDistribution.put("LOW", countLevel(rows, WorkforceIntelligenceDTO::getAttritionRiskLevel, "LOW"));
        response.put("riskDistribution", riskDistribution);

        Map<String, Long> engagementDistribution = new LinkedHashMap<>();
        engagementDistribution.put("HIGH", countLevel(rows, WorkforceIntelligenceDTO::getEngagementLevel, "HIGH"));
        engagementDistribution.put("MEDIUM", countLevel(rows, WorkforceIntelligenceDTO::getEngagementLevel, "MEDIUM"));
        engagementDistribution.put("LOW", countLevel(rows, WorkforceIntelligenceDTO::getEngagementLevel, "LOW"));
        response.put("engagementDistribution", engagementDistribution);

        Map<String, Long> topRiskSignals = rows.stream()
                .flatMap(row -> safeList(row.getRiskFactors()).stream())
                .filter(signal -> !"No significant risk signals detected".equalsIgnoreCase(signal))
                .collect(Collectors.groupingBy(signal -> signal, LinkedHashMap::new, Collectors.counting()));
        response.put("topRiskSignals", topRiskSignals);

        response.put("modelVersion", MODEL_VERSION);
        response.put("model", "Explainable local predictive scoring using attendance, leave, performance and employee-profile signals");
        response.put("generatedAt", java.time.LocalDateTime.now().toString());
        response.put("methodology", "Decision-support scoring; not a disciplinary or employment-decision system. Human review is required.");

        return response;
    }

    public List<Map<String, Object>> getDepartmentInsights() {
        List<WorkforceIntelligenceDTO> rows = analyzeAllEmployees();

        return rows.stream()
                .collect(Collectors.groupingBy(
                        row -> displayDepartment(row.getDepartment()),
                        LinkedHashMap::new,
                        Collectors.toList()
                ))
                .entrySet()
                .stream()
                .map(entry -> buildDepartmentInsight(entry.getKey(), entry.getValue()))
                .sorted(Comparator.comparingInt(row -> -((Number) row.get("averageRisk")).intValue()))
                .toList();
    }

    private Map<String, Object> buildDepartmentInsight(String department, List<WorkforceIntelligenceDTO> rows) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("department", department);
        result.put("employees", rows.size());
        result.put("averageRisk", roundedAverage(rows.stream().mapToInt(WorkforceIntelligenceDTO::getAttritionRiskScore).average().orElse(0)));
        result.put("averageEngagement", roundedAverage(rows.stream().mapToInt(WorkforceIntelligenceDTO::getEngagementScore).average().orElse(0)));
        result.put("averageAttendance", roundOne(rows.stream().mapToDouble(WorkforceIntelligenceDTO::getAttendanceRate).average().orElse(0)));
        result.put("averageBurnoutRisk", roundedAverage(rows.stream().mapToInt(WorkforceIntelligenceDTO::getBurnoutRiskScore).average().orElse(0)));
        result.put("highRiskEmployees", countLevel(rows, WorkforceIntelligenceDTO::getAttritionRiskLevel, "HIGH"));
        result.put("lowEngagementEmployees", countLevel(rows, WorkforceIntelligenceDTO::getEngagementLevel, "LOW"));
        result.put("urgentActions", rows.stream().filter(row -> "URGENT".equalsIgnoreCase(row.getPriority())).count());
        result.put("highPotentialEmployees", countLevel(rows, WorkforceIntelligenceDTO::getGrowthPotentialLevel, "HIGH"));

        String health;
        int averageRisk = ((Number) result.get("averageRisk")).intValue();
        int averageEngagement = ((Number) result.get("averageEngagement")).intValue();
        if (averageRisk >= 60 || averageEngagement < 50) {
            health = "NEEDS_ATTENTION";
        } else if (averageRisk >= 35 || averageEngagement < 70) {
            health = "WATCH";
        } else {
            health = "HEALTHY";
        }
        result.put("health", health);
        return result;
    }

    private WorkforceIntelligenceDTO calculateIntelligence(
            Employee employee,
            List<Attendance> attendanceInput,
            List<Leave> leavesInput,
            List<PerformanceReview> reviewsInput
    ) {
        List<Attendance> attendance = attendanceInput == null ? List.of() : attendanceInput;
        List<Leave> leaves = leavesInput == null ? List.of() : leavesInput;
        List<PerformanceReview> reviews = reviewsInput == null ? List.of() : reviewsInput;

        List<String> riskFactors = new ArrayList<>();
        List<String> skillGapIndicators = new ArrayList<>();
        List<String> recommendations = new ArrayList<>();
        List<String> strengths = new ArrayList<>();

        long attendanceRecords = attendance.size();
        long absentDays = attendance.stream().filter(record -> "ABSENT".equalsIgnoreCase(record.getStatus())).count();
        long presentDays = attendance.stream().filter(record -> "PRESENT".equalsIgnoreCase(record.getStatus())).count();
        double attendanceRate = attendanceRecords == 0 ? 0.0 : (presentDays * 100.0) / attendanceRecords;

        double averageWorkingHours = attendance.stream()
                .filter(record -> record.getCheckIn() != null && record.getCheckOut() != null && record.getCheckOut().isAfter(record.getCheckIn()))
                .mapToLong(record -> Duration.between(record.getCheckIn(), record.getCheckOut()).toMinutes())
                .average()
                .orElse(0.0) / 60.0;

        long pendingLeaves = leaves.stream().filter(leave -> "PENDING".equalsIgnoreCase(leave.getStatus())).count();
        long approvedLeaveDays = leaves.stream()
                .filter(leave -> "APPROVED".equalsIgnoreCase(leave.getStatus()))
                .mapToLong(leave -> leave.getNumberOfDays() == null ? 0 : leave.getNumberOfDays())
                .sum();

        List<PerformanceReview> validReviews = reviews.stream()
                .filter(review -> review.getRating() != null)
                .toList();
        double averageRating = validReviews.stream().mapToInt(PerformanceReview::getRating).average().orElse(0.0);
        String performanceTrend = calculatePerformanceTrend(validReviews);

        long tenureMonths = employee.getJoiningDate() == null
                ? 0
                : Math.max(0, ChronoUnit.MONTHS.between(employee.getJoiningDate(), LocalDate.now()));

        int attritionRiskScore = 8;
        int engagementScore = 72;
        int burnoutRiskScore = 8;
        int growthPotentialScore = 45;
        int skillGapScore = 10;

        // Attendance signals
        if (attendanceRecords >= 5) {
            if (attendanceRate < 70) {
                attritionRiskScore += 25;
                engagementScore -= 28;
                skillGapScore += 5;
                riskFactors.add("Low attendance rate");
                recommendations.add("Meet with the employee to understand attendance barriers and agree on a support plan.");
            } else if (attendanceRate < 85) {
                attritionRiskScore += 14;
                engagementScore -= 15;
                riskFactors.add("Attendance is below the healthy range");
                recommendations.add("Review attendance patterns and remove avoidable work barriers.");
            } else if (attendanceRate >= 95) {
                engagementScore += 8;
                growthPotentialScore += 6;
                strengths.add("Consistent attendance");
            }
        }

        if (absentDays >= 5) {
            attritionRiskScore += 18;
            engagementScore -= 16;
            riskFactors.add("Frequent absence detected");
        } else if (absentDays >= 3) {
            attritionRiskScore += 9;
            engagementScore -= 8;
            riskFactors.add("Elevated absence frequency");
        }

        // Working-hours / burnout signals
        if (averageWorkingHours >= 10.0) {
            burnoutRiskScore += 55;
            attritionRiskScore += 12;
            riskFactors.add("Sustained long working days");
            recommendations.add("Review workload, overtime and recovery time to reduce burnout risk.");
        } else if (averageWorkingHours >= 9.0) {
            burnoutRiskScore += 35;
            attritionRiskScore += 6;
            riskFactors.add("Long average working day");
            recommendations.add("Check workload balance and encourage sustainable working hours.");
        } else if (averageWorkingHours >= 7.0 && averageWorkingHours <= 8.75) {
            burnoutRiskScore = Math.max(0, burnoutRiskScore - 3);
        }

        // Leave signals
        if (pendingLeaves >= 3) {
            attritionRiskScore += 12;
            engagementScore -= 10;
            burnoutRiskScore += 10;
            riskFactors.add("Multiple pending leave requests");
            recommendations.add("Resolve outstanding leave requests promptly and discuss workload concerns.");
        } else if (pendingLeaves >= 1) {
            burnoutRiskScore += 4;
        }

        if (approvedLeaveDays >= 20) {
            attritionRiskScore += 8;
            riskFactors.add("High approved leave utilization");
        } else if (approvedLeaveDays >= 12) {
            attritionRiskScore += 4;
        }

        // Performance signals
        if (!validReviews.isEmpty()) {
            if (averageRating < 2.5) {
                attritionRiskScore += 25;
                engagementScore -= 22;
                growthPotentialScore -= 18;
                skillGapScore += 45;
                riskFactors.add("Low recent performance rating");
                skillGapIndicators.add("Performance improvement required");
                recommendations.add("Create a structured performance-improvement and mentoring plan with measurable milestones.");
            } else if (averageRating < 3.5) {
                attritionRiskScore += 10;
                engagementScore -= 8;
                growthPotentialScore += 2;
                skillGapScore += 24;
                skillGapIndicators.add("Targeted development opportunity");
                recommendations.add("Provide focused coaching, learning resources and a 30-60-90 day development goal.");
            } else if (averageRating >= 4.5) {
                engagementScore += 10;
                growthPotentialScore += 30;
                strengths.add("High performance rating");
                recommendations.add("Consider recognition, stretch assignments and a documented career-growth conversation.");
            } else {
                engagementScore += 4;
                growthPotentialScore += 14;
            }

            if ("DECLINING".equals(performanceTrend)) {
                attritionRiskScore += 10;
                engagementScore -= 8;
                skillGapScore += 10;
                riskFactors.add("Performance trend is declining");
                recommendations.add("Review the causes of the declining performance trend before the next review cycle.");
            } else if ("IMPROVING".equals(performanceTrend)) {
                growthPotentialScore += 10;
                engagementScore += 4;
                strengths.add("Improving performance trend");
            }
        } else {
            skillGapIndicators.add("No performance review data available");
            recommendations.add("Schedule a performance review so skill and growth insights are based on evidence.");
        }

        List<String> developmentAreas = reviews.stream()
                .filter(review -> hasText(review.getAreasForImprovement()))
                .map(review -> review.getAreasForImprovement().trim())
                .distinct()
                .limit(4)
                .toList();

        for (String area : developmentAreas) {
            skillGapIndicators.add("Development area: " + area);
            skillGapScore += 8;
        }

        boolean hasGoals = reviews.stream().anyMatch(review -> hasText(review.getGoals()));
        if (!reviews.isEmpty() && !hasGoals) {
            skillGapIndicators.add("Performance goals are not defined");
            skillGapScore += 12;
            recommendations.add("Define measurable role and development goals for the next review period.");
        } else if (hasGoals) {
            growthPotentialScore += 5;
        }

        List<String> recordedStrengths = reviews.stream()
                .filter(review -> hasText(review.getStrengths()))
                .map(review -> review.getStrengths().trim())
                .distinct()
                .limit(3)
                .toList();
        strengths.addAll(recordedStrengths);

        // Profile / tenure signals
        if (!hasText(employee.getJobTitle())) {
            skillGapIndicators.add("Job title is not defined");
            recommendations.add("Assign a clear job title and role expectations.");
        }
        if (!hasText(employee.getDepartment())) {
            skillGapIndicators.add("Department is not defined");
        }
        if (employee.getJoiningDate() != null && tenureMonths <= 3) {
            recommendations.add("Run an onboarding check-in and confirm role clarity, manager support and early goals.");
            growthPotentialScore += 4;
        }

        // Cross-signal adjustments
        if (engagementScore < 50) {
            attritionRiskScore += 14;
        }
        if (burnoutRiskScore >= 60) {
            attritionRiskScore += 10;
        }
        if (growthPotentialScore >= 75 && attritionRiskScore >= 40) {
            riskFactors.add("High-potential employee with elevated retention risk");
            recommendations.add("Prioritize a retention and career-growth conversation with this employee.");
        }

        engagementScore = clamp(engagementScore);
        attritionRiskScore = clamp(attritionRiskScore);
        burnoutRiskScore = clamp(burnoutRiskScore);
        growthPotentialScore = clamp(growthPotentialScore);
        skillGapScore = clamp(skillGapScore);

        String attritionRiskLevel = level(attritionRiskScore, 65, 35, "HIGH", "MEDIUM", "LOW");
        String engagementLevel = level(engagementScore, 75, 50, "HIGH", "MEDIUM", "LOW");
        String burnoutRiskLevel = level(burnoutRiskScore, 65, 35, "HIGH", "MEDIUM", "LOW");
        String growthPotentialLevel = level(growthPotentialScore, 75, 50, "HIGH", "MEDIUM", "DEVELOPING");
        String skillGapLevel = level(skillGapScore, 65, 35, "HIGH", "MEDIUM", "LOW");

        int confidenceScore = calculateConfidence(employee, attendance, leaves, validReviews);
        String dataQuality = confidenceScore >= 80 ? "HIGH" : confidenceScore >= 55 ? "MEDIUM" : "LIMITED";

        String priority;
        if (attritionRiskScore >= 65 || burnoutRiskScore >= 70 || engagementScore < 40) {
            priority = "URGENT";
        } else if (attritionRiskScore >= 35 || burnoutRiskScore >= 35 || engagementScore < 60 || skillGapScore >= 50) {
            priority = "WATCH";
        } else {
            priority = "STABLE";
        }

        if (riskFactors.isEmpty()) {
            riskFactors.add("No significant risk signals detected");
        }
        if (skillGapIndicators.isEmpty()) {
            skillGapIndicators.add("No significant development gaps detected");
        }
        if (strengths.isEmpty()) {
            strengths.add("No strengths recorded in completed performance data yet");
        }
        if (recommendations.isEmpty()) {
            recommendations.add("Maintain regular manager check-ins and continue monitoring engagement and performance trends.");
        }

        riskFactors = distinctLimited(riskFactors, 6);
        skillGapIndicators = distinctLimited(skillGapIndicators, 6);
        strengths = distinctLimited(strengths, 5);
        recommendations = distinctLimited(recommendations, 7);

        WorkforceIntelligenceDTO dto = new WorkforceIntelligenceDTO();
        dto.setEmployeeId(employee.getId());
        dto.setEmployeeName((safe(employee.getFirstName()) + " " + safe(employee.getLastName())).trim());
        dto.setDepartment(employee.getDepartment());
        dto.setJobTitle(employee.getJobTitle());
        dto.setTenureMonths(tenureMonths);
        dto.setAttritionRiskScore(attritionRiskScore);
        dto.setAttritionRiskLevel(attritionRiskLevel);
        dto.setRetentionScore(100 - attritionRiskScore);
        dto.setEngagementScore(engagementScore);
        dto.setEngagementLevel(engagementLevel);
        dto.setBurnoutRiskScore(burnoutRiskScore);
        dto.setBurnoutRiskLevel(burnoutRiskLevel);
        dto.setGrowthPotentialScore(growthPotentialScore);
        dto.setGrowthPotentialLevel(growthPotentialLevel);
        dto.setSkillGapScore(skillGapScore);
        dto.setSkillGapLevel(skillGapLevel);
        dto.setAttendanceRate(roundOne(attendanceRate));
        dto.setAbsentDays(absentDays);
        dto.setAttendanceRecords(attendanceRecords);
        dto.setAverageWorkingHours(roundOne(averageWorkingHours));
        dto.setPendingLeaveRequests(pendingLeaves);
        dto.setApprovedLeaveDays(approvedLeaveDays);
        dto.setAveragePerformanceRating(roundOne(averageRating));
        dto.setPerformanceTrend(performanceTrend);
        dto.setPerformanceReviewCount(validReviews.size());
        dto.setConfidenceScore(confidenceScore);
        dto.setDataQuality(dataQuality);
        dto.setPriority(priority);
        dto.setModelVersion(MODEL_VERSION);
        dto.setRiskFactors(riskFactors);
        dto.setSkillGapIndicators(skillGapIndicators);
        dto.setRecommendations(recommendations);
        dto.setStrengths(strengths);
        dto.setInsightSummary(buildInsightSummary(dto));
        return dto;
    }

    private int calculateConfidence(
            Employee employee,
            List<Attendance> attendance,
            List<Leave> leaves,
            List<PerformanceReview> validReviews
    ) {
        int score = 10;
        if (attendance.size() >= 20) score += 30;
        else if (attendance.size() >= 5) score += 22;
        else if (!attendance.isEmpty()) score += 10;

        if (validReviews.size() >= 2) score += 30;
        else if (validReviews.size() == 1) score += 20;

        if (!leaves.isEmpty()) score += 12;
        if (hasText(employee.getDepartment())) score += 6;
        if (hasText(employee.getJobTitle())) score += 6;
        if (employee.getJoiningDate() != null) score += 6;
        return clamp(score);
    }

    private String calculatePerformanceTrend(List<PerformanceReview> validReviews) {
        if (validReviews.size() < 2) return "INSUFFICIENT_DATA";

        List<PerformanceReview> sorted = new ArrayList<>(validReviews);
        sorted.sort(Comparator.comparing(
                PerformanceReview::getReviewPeriodEnd,
                Comparator.nullsLast(Comparator.naturalOrder())
        ).thenComparing(PerformanceReview::getId, Comparator.nullsLast(Comparator.naturalOrder())));

        int previous = sorted.get(sorted.size() - 2).getRating();
        int latest = sorted.get(sorted.size() - 1).getRating();
        if (latest > previous) return "IMPROVING";
        if (latest < previous) return "DECLINING";
        return "STABLE";
    }

    private String buildInsightSummary(WorkforceIntelligenceDTO dto) {
        String riskText = dto.getAttritionRiskLevel().toLowerCase(Locale.ROOT);
        String engagementText = dto.getEngagementLevel().toLowerCase(Locale.ROOT);
        String burnoutText = dto.getBurnoutRiskLevel().toLowerCase(Locale.ROOT);
        String potentialText = dto.getGrowthPotentialLevel().toLowerCase(Locale.ROOT);

        return dto.getEmployeeName()
                + " has " + riskText + " predicted attrition risk (" + dto.getAttritionRiskScore() + "%), "
                + engagementText + " engagement (" + dto.getEngagementScore() + "%), "
                + burnoutText + " burnout risk and " + potentialText + " growth potential. "
                + "Priority: " + dto.getPriority() + ". Data confidence: " + dto.getConfidenceScore() + "% (" + dto.getDataQuality() + ").";
    }

    private boolean isActiveEmployee(Employee employee) {
        return employee.getEmploymentStatus() == null || !"INACTIVE".equalsIgnoreCase(employee.getEmploymentStatus());
    }

    private static int clamp(int value) {
        return Math.max(0, Math.min(100, value));
    }

    private static String level(int value, int highThreshold, int mediumThreshold, String high, String medium, String low) {
        if (value >= highThreshold) return high;
        if (value >= mediumThreshold) return medium;
        return low;
    }

    private static int roundedAverage(double value) {
        return (int) Math.round(value);
    }

    private static double roundOne(double value) {
        return Math.round(value * 10.0) / 10.0;
    }

    private static boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private static String safe(String value) {
        return value == null ? "" : value;
    }

    private static String displayDepartment(String department) {
        return hasText(department) ? department.trim() : "Unassigned";
    }

    private static <T> long countLevel(List<T> rows, java.util.function.Function<T, String> extractor, String level) {
        return rows.stream().filter(row -> level.equalsIgnoreCase(extractor.apply(row))).count();
    }

    private static List<String> safeList(List<String> values) {
        return values == null ? List.of() : values;
    }

    private static List<String> distinctLimited(List<String> values, int limit) {
        return values.stream().filter(Objects::nonNull).map(String::trim).filter(value -> !value.isBlank()).distinct().limit(limit).toList();
    }
}
