package com.nexushr.service;

import com.nexushr.dto.AttendanceDTO;
import com.nexushr.dto.AttendanceDashboardDTO;
import com.nexushr.entity.Attendance;
import com.nexushr.entity.Employee;
import com.nexushr.repository.AttendanceRepository;
import com.nexushr.repository.EmployeeRepository;

import jakarta.transaction.Transactional;

import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class AttendanceService {

    private final AttendanceRepository attendanceRepository;
    private final EmployeeRepository employeeRepository;

    public AttendanceService(
            AttendanceRepository attendanceRepository,
            EmployeeRepository employeeRepository) {

        this.attendanceRepository = attendanceRepository;
        this.employeeRepository = employeeRepository;
    }

    // =========================================================
    // CREATE ATTENDANCE
    // =========================================================

    @Transactional
    public AttendanceDTO createAttendance(AttendanceDTO dto) {

        if (dto.getEmployeeId() == null) {
            throw new RuntimeException("Employee ID is required");
        }

        if (dto.getAttendanceDate() == null) {
            throw new RuntimeException("Attendance date is required");
        }

        Employee employee = employeeRepository
                .findById(dto.getEmployeeId())
                .orElseThrow(() ->
                        new RuntimeException(
                                "Employee not found with ID: "
                                        + dto.getEmployeeId()
                        )
                );

        boolean alreadyExists =
                attendanceRepository
                        .findByEmployeeIdAndAttendanceDate(
                                dto.getEmployeeId(),
                                dto.getAttendanceDate()
                        )
                        .isPresent();

        if (alreadyExists) {
            throw new RuntimeException(
                    "Attendance already exists for this employee on this date"
            );
        }

        Attendance attendance = new Attendance();

        attendance.setEmployee(employee);
        attendance.setAttendanceDate(dto.getAttendanceDate());
        attendance.setCheckIn(dto.getCheckIn());
        attendance.setCheckOut(dto.getCheckOut());

        attendance.setStatus(
                dto.getStatus() != null
                        ? dto.getStatus().toUpperCase()
                        : "PRESENT"
        );

        attendance.setRemarks(dto.getRemarks());

        Attendance saved =
                attendanceRepository.save(attendance);

        return convertToDTO(saved);
    }

    // =========================================================
    // CHECK IN
    // =========================================================

    @Transactional
    public AttendanceDTO checkIn(Long employeeId) {

        Employee employee = employeeRepository
                .findById(employeeId)
                .orElseThrow(() ->
                        new RuntimeException(
                                "Employee not found with ID: "
                                        + employeeId
                        )
                );

        LocalDate today = LocalDate.now();

        var existing =
                attendanceRepository
                        .findByEmployeeIdAndAttendanceDate(
                                employeeId,
                                today
                        );

        // If attendance already exists, do not create duplicate
        if (existing.isPresent()) {

            Attendance existingAttendance = existing.get();

            if (existingAttendance.getCheckIn() != null) {
                throw new RuntimeException(
                        "Employee has already checked in today"
                );
            }

            existingAttendance.setCheckIn(
                    LocalDateTime.now()
            );

            existingAttendance.setStatus("PRESENT");

            existingAttendance.setRemarks(
                    "Employee checked in"
            );

            Attendance saved =
                    attendanceRepository.save(
                            existingAttendance
                    );

            return convertToDTO(saved);
        }

        Attendance attendance = new Attendance();

        attendance.setEmployee(employee);
        attendance.setAttendanceDate(today);

        attendance.setCheckIn(
                LocalDateTime.now()
        );

        attendance.setStatus("PRESENT");

        attendance.setRemarks(
                "Employee checked in"
        );

        Attendance saved =
                attendanceRepository.save(attendance);

        return convertToDTO(saved);
    }

    // =========================================================
    // CHECK OUT
    // =========================================================

    @Transactional
    public AttendanceDTO checkOut(Long employeeId) {

        Employee employee = employeeRepository
                .findById(employeeId)
                .orElseThrow(() ->
                        new RuntimeException(
                                "Employee not found with ID: "
                                        + employeeId
                        )
                );

        LocalDate today = LocalDate.now();

        Attendance attendance =
                attendanceRepository
                        .findByEmployeeIdAndAttendanceDate(
                                employeeId,
                                today
                        )
                        .orElseThrow(() ->
                                new RuntimeException(
                                        "Employee has not checked in today"
                                )
                        );

        if (attendance.getCheckIn() == null) {
            throw new RuntimeException(
                    "Employee has not checked in today"
            );
        }

        if (attendance.getCheckOut() != null) {
            throw new RuntimeException(
                    "Employee has already checked out today"
            );
        }

        LocalDateTime checkOutTime =
                LocalDateTime.now();

        if (checkOutTime.isBefore(
                attendance.getCheckIn())) {

            throw new RuntimeException(
                    "Check-out time cannot be before check-in time"
            );
        }

        attendance.setCheckOut(checkOutTime);

        attendance.setStatus("PRESENT");

        attendance.setRemarks(
                "Employee checked out"
        );

        Attendance saved =
                attendanceRepository.save(attendance);

        return convertToDTO(saved);
    }

    // =========================================================
    // MARK HALF DAY
    // =========================================================

    @Transactional
    public AttendanceDTO markHalfDay(Long employeeId, LocalDate date) {
        Employee employee = employeeRepository.findById(employeeId)
                .orElseThrow(() -> new RuntimeException("Employee not found with ID: " + employeeId));

        LocalDate targetDate = date == null ? LocalDate.now() : date;
        Attendance attendance = attendanceRepository
                .findByEmployeeIdAndAttendanceDate(employeeId, targetDate)
                .orElseGet(() -> {
                    Attendance created = new Attendance();
                    created.setEmployee(employee);
                    created.setAttendanceDate(targetDate);
                    return created;
                });

        // For today's self-service half-day, preserve/complete punch times.
        if (targetDate.equals(LocalDate.now())) {
            if (attendance.getCheckIn() == null) {
                attendance.setCheckIn(LocalDateTime.now());
            }
            if (attendance.getCheckOut() == null) {
                attendance.setCheckOut(LocalDateTime.now());
            }
        }

        attendance.setStatus("HALF_DAY");
        attendance.setRemarks("Half day attendance");
        return convertToDTO(attendanceRepository.save(attendance));
    }

    // =========================================================
// ATTENDANCE DASHBOARD
// =========================================================

    public AttendanceDashboardDTO getAttendanceDashboard(
            LocalDate date) {

        if (date == null) {
            date = LocalDate.now();
        }

        long totalEmployees =
                employeeRepository
                        .countByEmploymentStatus("ACTIVE");

        long present =
                attendanceRepository
                        .countByAttendanceDateAndStatus(
                                date,
                                "PRESENT"
                        );

        long absent =
                Math.max(
                        totalEmployees - present,
                        0
                );

        long checkedIn =
                attendanceRepository
                        .findByAttendanceDate(date)
                        .stream()
                        .filter(attendance ->
                                attendance.getCheckIn() != null)
                        .count();

        long checkedOut =
                attendanceRepository
                        .findByAttendanceDate(date)
                        .stream()
                        .filter(attendance ->
                                attendance.getCheckOut() != null)
                        .count();

        return new AttendanceDashboardDTO(
                date,
                totalEmployees,
                present,
                absent,
                checkedIn,
                checkedOut
        );
    }

    // =========================================================
    // GET ALL ATTENDANCE
    // =========================================================

    public List<AttendanceDTO> getAllAttendance() {

        return attendanceRepository
                .findAll()
                .stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    // =========================================================
    // GET ATTENDANCE BY ID
    // =========================================================

    public AttendanceDTO getAttendanceById(Long id) {

        Attendance attendance =
                attendanceRepository
                        .findById(id)
                        .orElseThrow(() ->
                                new RuntimeException(
                                        "Attendance record not found with ID: "
                                                + id
                                )
                        );

        return convertToDTO(attendance);
    }

    // =========================================================
    // GET EMPLOYEE ATTENDANCE
    // =========================================================

    public List<AttendanceDTO> getEmployeeAttendance(
            Long employeeId) {

        return attendanceRepository
                .findByEmployeeId(employeeId)
                .stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    // =========================================================
    // GET ATTENDANCE BY DATE
    // =========================================================

    public List<AttendanceDTO> getAttendanceByDate(
            LocalDate date) {

        return attendanceRepository
                .findByAttendanceDate(date)
                .stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    // =========================================================
    // GET BETWEEN DATES
    // =========================================================

    public List<AttendanceDTO>
    getEmployeeAttendanceBetweenDates(
            Long employeeId,
            LocalDate startDate,
            LocalDate endDate) {

        return attendanceRepository
                .findByEmployeeIdAndAttendanceDateBetween(
                        employeeId,
                        startDate,
                        endDate
                )
                .stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    // =========================================================
    // GET BY STATUS
    // =========================================================

    public List<AttendanceDTO> getAttendanceByStatus(
            String status) {

        return attendanceRepository
                .findByStatus(status.toUpperCase())
                .stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    // =========================================================
    // UPDATE ATTENDANCE
    // =========================================================

    @Transactional
    public AttendanceDTO updateAttendance(
            Long id,
            AttendanceDTO dto) {

        Attendance attendance =
                attendanceRepository
                        .findById(id)
                        .orElseThrow(() ->
                                new RuntimeException(
                                        "Attendance record not found with ID: "
                                                + id
                                )
                        );

        if (dto.getAttendanceDate() != null &&
                dto.getEmployeeId() != null) {

            var duplicate =
                    attendanceRepository
                            .findByEmployeeIdAndAttendanceDate(
                                    dto.getEmployeeId(),
                                    dto.getAttendanceDate()
                            );

            if (duplicate.isPresent() &&
                    !duplicate.get()
                            .getId()
                            .equals(id)) {

                throw new RuntimeException(
                        "Attendance already exists for this employee on this date"
                );
            }
        }

        if (dto.getAttendanceDate() != null) {
            attendance.setAttendanceDate(
                    dto.getAttendanceDate()
            );
        }

        if (dto.getCheckIn() != null) {
            attendance.setCheckIn(
                    dto.getCheckIn()
            );
        }

        attendance.setCheckOut(
                dto.getCheckOut()
        );

        if (dto.getStatus() != null) {
            attendance.setStatus(
                    dto.getStatus().toUpperCase()
            );
        }

        attendance.setRemarks(
                dto.getRemarks()
        );

        Attendance updated =
                attendanceRepository.save(attendance);

        return convertToDTO(updated);
    }

    // =========================================================
    // DELETE
    // =========================================================

    @Transactional
    public void deleteAttendance(Long id) {

        if (!attendanceRepository.existsById(id)) {

            throw new RuntimeException(
                    "Attendance record not found with ID: "
                            + id
            );
        }

        attendanceRepository.deleteById(id);
    }

    // =========================================================
    // ENTITY -> DTO
    // =========================================================

    private AttendanceDTO convertToDTO(
            Attendance attendance) {

        AttendanceDTO dto = new AttendanceDTO();

        dto.setId(attendance.getId());

        if (attendance.getEmployee() != null) {
            dto.setEmployeeId(
                    attendance.getEmployee().getId()
            );
        }

        dto.setAttendanceDate(
                attendance.getAttendanceDate()
        );

        dto.setCheckIn(
                attendance.getCheckIn()
        );

        dto.setCheckOut(
                attendance.getCheckOut()
        );

        dto.setStatus(
                attendance.getStatus()
        );

        dto.setRemarks(
                attendance.getRemarks()
        );

        if (attendance.getCheckIn() != null &&
                attendance.getCheckOut() != null) {

            long seconds = Math.max(0L, Duration.between(
                    attendance.getCheckIn(),
                    attendance.getCheckOut()
            ).getSeconds());

            long workingMinutes = Math.round(seconds / 60.0);
            double workingHours = Math.round((workingMinutes / 60.0) * 100.0) / 100.0;

            dto.setWorkingMinutes(workingMinutes);
            dto.setWorkingHours(workingHours);

        } else {

            dto.setWorkingMinutes(0L);
            dto.setWorkingHours(0.0);
        }

        return dto;
    }
}