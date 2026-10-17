package com.nexushr.service;

import com.nexushr.dto.AttendanceDTO;
import com.nexushr.entity.Attendance;
import com.nexushr.entity.Employee;
import com.nexushr.repository.AttendanceRepository;
import com.nexushr.repository.EmployeeRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AttendanceServiceTest {

    @Mock
    private AttendanceRepository attendanceRepository;

    @Mock
    private EmployeeRepository employeeRepository;

    @InjectMocks
    private AttendanceService attendanceService;

    @Test
    void createAttendance_shouldCreateAttendanceWithDefaultPresentStatus() {
        Employee employee = new Employee();
        employee.setId(1L);

        AttendanceDTO dto = new AttendanceDTO();
        dto.setEmployeeId(1L);
        dto.setAttendanceDate(LocalDate.of(2026, 10, 1));
        dto.setRemarks("Office");

        Attendance saved = new Attendance();
        saved.setId(10L);
        saved.setEmployee(employee);
        saved.setAttendanceDate(dto.getAttendanceDate());
        saved.setStatus("PRESENT");
        saved.setRemarks("Office");

        when(employeeRepository.findById(1L))
                .thenReturn(Optional.of(employee));

        when(attendanceRepository
                .findByEmployeeIdAndAttendanceDate(
                        1L,
                        dto.getAttendanceDate()
                ))
                .thenReturn(Optional.empty());

        when(attendanceRepository.save(any(Attendance.class)))
                .thenReturn(saved);

        AttendanceDTO result =
                attendanceService.createAttendance(dto);

        assertNotNull(result);
        assertEquals(10L, result.getId());
        assertEquals(1L, result.getEmployeeId());
        assertEquals(
                LocalDate.of(2026, 10, 1),
                result.getAttendanceDate()
        );
        assertEquals("PRESENT", result.getStatus());
        assertEquals("Office", result.getRemarks());

        verify(employeeRepository).findById(1L);
        verify(attendanceRepository)
                .findByEmployeeIdAndAttendanceDate(
                        1L,
                        dto.getAttendanceDate()
                );
        verify(attendanceRepository).save(any(Attendance.class));
    }

    @Test
    void createAttendance_shouldThrowWhenEmployeeIdMissing() {
        AttendanceDTO dto = new AttendanceDTO();
        dto.setAttendanceDate(LocalDate.of(2026, 10, 1));

        RuntimeException exception = assertThrows(
                RuntimeException.class,
                () -> attendanceService.createAttendance(dto)
        );

        assertEquals(
                "Employee ID is required",
                exception.getMessage()
        );

        verifyNoInteractions(
                employeeRepository,
                attendanceRepository
        );
    }

    @Test
    void createAttendance_shouldThrowWhenAttendanceDateMissing() {
        AttendanceDTO dto = new AttendanceDTO();
        dto.setEmployeeId(1L);

        RuntimeException exception = assertThrows(
                RuntimeException.class,
                () -> attendanceService.createAttendance(dto)
        );

        assertEquals(
                "Attendance date is required",
                exception.getMessage()
        );

        verifyNoInteractions(
                employeeRepository,
                attendanceRepository
        );
    }

    @Test
    void createAttendance_shouldThrowWhenEmployeeDoesNotExist() {
        AttendanceDTO dto = new AttendanceDTO();
        dto.setEmployeeId(999L);
        dto.setAttendanceDate(LocalDate.of(2026, 10, 1));

        when(employeeRepository.findById(999L))
                .thenReturn(Optional.empty());

        RuntimeException exception = assertThrows(
                RuntimeException.class,
                () -> attendanceService.createAttendance(dto)
        );

        assertTrue(
                exception.getMessage().contains(
                        "Employee not found with ID"
                )
        );

        verify(employeeRepository).findById(999L);
        verify(attendanceRepository, never())
                .save(any(Attendance.class));
    }

    @Test
    void createAttendance_shouldRejectDuplicateAttendance() {
        Employee employee = new Employee();
        employee.setId(1L);

        Attendance existing = new Attendance();
        existing.setId(20L);
        existing.setEmployee(employee);

        AttendanceDTO dto = new AttendanceDTO();
        dto.setEmployeeId(1L);
        dto.setAttendanceDate(LocalDate.of(2026, 10, 1));

        when(employeeRepository.findById(1L))
                .thenReturn(Optional.of(employee));

        when(attendanceRepository
                .findByEmployeeIdAndAttendanceDate(
                        1L,
                        dto.getAttendanceDate()
                ))
                .thenReturn(Optional.of(existing));

        RuntimeException exception = assertThrows(
                RuntimeException.class,
                () -> attendanceService.createAttendance(dto)
        );

        assertEquals(
                "Attendance already exists for this employee on this date",
                exception.getMessage()
        );

        verify(attendanceRepository, never())
                .save(any(Attendance.class));
    }

    @Test
    void createAttendance_shouldUppercaseProvidedStatus() {
        Employee employee = new Employee();
        employee.setId(1L);

        AttendanceDTO dto = new AttendanceDTO();
        dto.setEmployeeId(1L);
        dto.setAttendanceDate(LocalDate.of(2026, 10, 1));
        dto.setStatus("late");

        Attendance saved = new Attendance();
        saved.setId(11L);
        saved.setEmployee(employee);
        saved.setAttendanceDate(dto.getAttendanceDate());
        saved.setStatus("LATE");

        when(employeeRepository.findById(1L))
                .thenReturn(Optional.of(employee));

        when(attendanceRepository
                .findByEmployeeIdAndAttendanceDate(
                        1L,
                        dto.getAttendanceDate()
                ))
                .thenReturn(Optional.empty());

        when(attendanceRepository.save(any(Attendance.class)))
                .thenReturn(saved);

        AttendanceDTO result =
                attendanceService.createAttendance(dto);

        assertEquals("LATE", result.getStatus());

        verify(attendanceRepository).save(any(Attendance.class));
    }

    @Test
    void checkIn_shouldCreateNewAttendanceWhenNoneExistsToday() {
        Employee employee = new Employee();
        employee.setId(1L);

        Attendance saved = new Attendance();
        saved.setId(30L);
        saved.setEmployee(employee);
        saved.setAttendanceDate(LocalDate.now());
        saved.setCheckIn(LocalDateTime.now());
        saved.setStatus("PRESENT");
        saved.setRemarks("Employee checked in");

        when(employeeRepository.findById(1L))
                .thenReturn(Optional.of(employee));

        when(attendanceRepository
                .findByEmployeeIdAndAttendanceDate(
                        eq(1L),
                        any(LocalDate.class)
                ))
                .thenReturn(Optional.empty());

        when(attendanceRepository.save(any(Attendance.class)))
                .thenReturn(saved);

        AttendanceDTO result =
                attendanceService.checkIn(1L);

        assertNotNull(result);
        assertEquals(30L, result.getId());
        assertEquals(1L, result.getEmployeeId());
        assertEquals("PRESENT", result.getStatus());
        assertNotNull(result.getCheckIn());
        assertEquals(
                "Employee checked in",
                result.getRemarks()
        );

        verify(attendanceRepository)
                .save(any(Attendance.class));
    }

    @Test
    void checkIn_shouldRejectSecondCheckIn() {
        Employee employee = new Employee();
        employee.setId(1L);

        Attendance existing = new Attendance();
        existing.setId(31L);
        existing.setEmployee(employee);
        existing.setAttendanceDate(LocalDate.now());
        existing.setCheckIn(LocalDateTime.now().minusHours(1));
        existing.setStatus("PRESENT");

        when(employeeRepository.findById(1L))
                .thenReturn(Optional.of(employee));

        when(attendanceRepository
                .findByEmployeeIdAndAttendanceDate(
                        eq(1L),
                        any(LocalDate.class)
                ))
                .thenReturn(Optional.of(existing));

        RuntimeException exception = assertThrows(
                RuntimeException.class,
                () -> attendanceService.checkIn(1L)
        );

        assertEquals(
                "Employee has already checked in today",
                exception.getMessage()
        );

        verify(attendanceRepository, never())
                .save(any(Attendance.class));
    }

    @Test
    void checkOut_shouldRejectWhenNoAttendanceExists() {
        Employee employee = new Employee();
        employee.setId(1L);

        when(employeeRepository.findById(1L))
                .thenReturn(Optional.of(employee));

        when(attendanceRepository
                .findByEmployeeIdAndAttendanceDate(
                        eq(1L),
                        any(LocalDate.class)
                ))
                .thenReturn(Optional.empty());

        RuntimeException exception = assertThrows(
                RuntimeException.class,
                () -> attendanceService.checkOut(1L)
        );

        assertEquals(
                "Employee has not checked in today",
                exception.getMessage()
        );

        verify(attendanceRepository, never())
                .save(any(Attendance.class));
    }

    @Test
    void checkOut_shouldRejectSecondCheckOut() {
        Employee employee = new Employee();
        employee.setId(1L);

        Attendance existing = new Attendance();
        existing.setId(32L);
        existing.setEmployee(employee);
        existing.setAttendanceDate(LocalDate.now());
        existing.setCheckIn(LocalDateTime.now().minusHours(2));
        existing.setCheckOut(LocalDateTime.now().minusHours(1));
        existing.setStatus("PRESENT");

        when(employeeRepository.findById(1L))
                .thenReturn(Optional.of(employee));

        when(attendanceRepository
                .findByEmployeeIdAndAttendanceDate(
                        eq(1L),
                        any(LocalDate.class)
                ))
                .thenReturn(Optional.of(existing));

        RuntimeException exception = assertThrows(
                RuntimeException.class,
                () -> attendanceService.checkOut(1L)
        );

        assertEquals(
                "Employee has already checked out today",
                exception.getMessage()
        );

        verify(attendanceRepository, never())
                .save(any(Attendance.class));
    }

    @Test
    void getAttendanceByStatus_shouldUppercaseStatus() {
        Employee employee = new Employee();
        employee.setId(1L);

        Attendance attendance = new Attendance();
        attendance.setId(40L);
        attendance.setEmployee(employee);
        attendance.setAttendanceDate(LocalDate.of(2026, 10, 1));
        attendance.setStatus("PRESENT");

        when(attendanceRepository.findByStatus("PRESENT"))
                .thenReturn(List.of(attendance));

        List<AttendanceDTO> result =
                attendanceService.getAttendanceByStatus("present");

        assertEquals(1, result.size());
        assertEquals("PRESENT", result.get(0).getStatus());
        assertEquals(1L, result.get(0).getEmployeeId());

        verify(attendanceRepository)
                .findByStatus("PRESENT");
    }

    @Test
    void deleteAttendance_shouldDeleteExistingRecord() {
        when(attendanceRepository.existsById(50L))
                .thenReturn(true);

        attendanceService.deleteAttendance(50L);

        verify(attendanceRepository).existsById(50L);
        verify(attendanceRepository).deleteById(50L);
    }

    @Test
    void deleteAttendance_shouldThrowWhenRecordDoesNotExist() {
        when(attendanceRepository.existsById(999L))
                .thenReturn(false);

        RuntimeException exception = assertThrows(
                RuntimeException.class,
                () -> attendanceService.deleteAttendance(999L)
        );

        assertEquals(
                "Attendance record not found with ID: 999",
                exception.getMessage()
        );

        verify(attendanceRepository, never())
                .deleteById(999L);
    }
}

