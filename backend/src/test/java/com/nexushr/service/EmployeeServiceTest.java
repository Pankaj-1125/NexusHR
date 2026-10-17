package com.nexushr.service;

import com.nexushr.dto.EmployeeDTO;
import com.nexushr.entity.Employee;
import com.nexushr.entity.User;
import com.nexushr.repository.EmployeeRepository;
import com.nexushr.repository.UserRepository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EmployeeServiceTest {

    @Mock
    private EmployeeRepository employeeRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private AuditLogService auditLogService;

    @InjectMocks
    private EmployeeService employeeService;

    private Employee employee;
    private EmployeeDTO employeeDTO;
    private User user;

    @BeforeEach
    void setUp() {

        employee = new Employee(
                "John",
                "Doe",
                "john.doe@example.com",
                "9876543210",
                "IT",
                "Software Engineer",
                LocalDate.of(2026, 1, 10),
                75000.0,
                "ACTIVE"
        );

        employee.setId(1L);

        user = new User(
                "john.doe",
                "encoded-password",
                "EMPLOYEE"
        );

        user.setId(10L);
        user.setEnabled(true);

        employeeDTO = new EmployeeDTO();

        employeeDTO.setId(1L);
        employeeDTO.setFirstName("John");
        employeeDTO.setLastName("Doe");
        employeeDTO.setEmail("john.doe@example.com");
        employeeDTO.setPhone("9876543210");
        employeeDTO.setDepartment("IT");
        employeeDTO.setJobTitle("Software Engineer");
        employeeDTO.setJoiningDate(LocalDate.of(2026, 1, 10));
        employeeDTO.setSalary(75000.0);
        employeeDTO.setEmploymentStatus("ACTIVE");
    }

    // -------------------------------------------------------------------------
    // ADD EMPLOYEE
    // -------------------------------------------------------------------------

    @Test
    void addEmployee_shouldCreateEmployeeSuccessfully() {

        when(employeeRepository.findByEmail(employeeDTO.getEmail()))
                .thenReturn(Optional.empty());

        when(employeeRepository.save(any(Employee.class)))
                .thenReturn(employee);

        EmployeeDTO result =
                employeeService.addEmployee(employeeDTO);

        assertNotNull(result);
        assertEquals("John", result.getFirstName());
        assertEquals("Doe", result.getLastName());
        assertEquals("john.doe@example.com", result.getEmail());
        assertEquals("IT", result.getDepartment());
        assertEquals("Software Engineer", result.getJobTitle());
        assertEquals("ACTIVE", result.getEmploymentStatus());

        verify(employeeRepository)
                .findByEmail(employeeDTO.getEmail());

        verify(employeeRepository)
                .save(any(Employee.class));

        verify(auditLogService)
                .log(
                        eq("EMPLOYEE_CREATED"),
                        eq("EMPLOYEE"),
                        eq(1L),
                        contains("Employee created")
                );
    }

    @Test
    void addEmployee_shouldRejectDuplicateEmail() {

        when(employeeRepository.findByEmail(employeeDTO.getEmail()))
                .thenReturn(Optional.of(employee));

        RuntimeException exception =
                assertThrows(
                        RuntimeException.class,
                        () -> employeeService.addEmployee(employeeDTO)
                );

        assertTrue(
                exception.getMessage()
                        .contains("already exists")
        );

        verify(employeeRepository, never())
                .save(any(Employee.class));

        verifyNoInteractions(auditLogService);
    }

    @Test
    void addEmployee_shouldDefaultStatusToActive() {

        employeeDTO.setEmploymentStatus(null);

        when(employeeRepository.findByEmail(employeeDTO.getEmail()))
                .thenReturn(Optional.empty());

        when(employeeRepository.save(any(Employee.class)))
                .thenAnswer(invocation -> {
                    Employee saved =
                            invocation.getArgument(0);

                    saved.setId(1L);

                    return saved;
                });

        EmployeeDTO result =
                employeeService.addEmployee(employeeDTO);

        assertEquals(
                "ACTIVE",
                result.getEmploymentStatus()
        );

        verify(auditLogService)
                .log(
                        eq("EMPLOYEE_CREATED"),
                        eq("EMPLOYEE"),
                        eq(1L),
                        contains("Employee created")
                );
    }

    // -------------------------------------------------------------------------
    // ONBOARD EMPLOYEE
    // -------------------------------------------------------------------------

    @Test
    void onboardEmployee_shouldCreateLinkedEmployeeUser() {

        when(employeeRepository.findById(1L))
                .thenReturn(Optional.of(employee));

        when(userRepository.findByUsername("john.doe"))
                .thenReturn(Optional.empty());

        when(passwordEncoder.encode("Temp@123"))
                .thenReturn("encoded-password");

        when(userRepository.save(any(User.class)))
                .thenAnswer(invocation -> {
                    User saved =
                            invocation.getArgument(0);

                    saved.setId(10L);

                    return saved;
                });

        when(employeeRepository.save(any(Employee.class)))
                .thenAnswer(invocation ->
                        invocation.getArgument(0)
                );

        EmployeeDTO result =
                employeeService.onboardEmployee(
                        1L,
                        "john.doe",
                        "Temp@123"
                );

        assertNotNull(result);
        assertEquals("john.doe", result.getUsername());
        assertEquals(10L, result.getUserId());
        assertEquals("ACTIVE", result.getEmploymentStatus());

        assertNotNull(employee.getUser());

        assertEquals(
                "john.doe",
                employee.getUser().getUsername()
        );

        assertEquals(
                "EMPLOYEE",
                employee.getUser().getRole()
        );

        assertEquals(
                "encoded-password",
                employee.getUser().getPassword()
        );

        assertTrue(employee.getUser().isEnabled());

        verify(passwordEncoder)
                .encode("Temp@123");

        verify(userRepository)
                .save(any(User.class));

        verify(employeeRepository)
                .save(employee);

        verify(auditLogService)
                .log(
                        eq("EMPLOYEE_ONBOARDED"),
                        eq("EMPLOYEE"),
                        eq(1L),
                        contains("john.doe")
                );
    }

    @Test
    void onboardEmployee_shouldRejectMissingEmployee() {

        when(employeeRepository.findById(999L))
                .thenReturn(Optional.empty());

        RuntimeException exception =
                assertThrows(
                        RuntimeException.class,
                        () -> employeeService.onboardEmployee(
                                999L,
                                "john.doe",
                                "Temp@123"
                        )
                );

        assertEquals(
                "Employee not found with ID: 999",
                exception.getMessage()
        );

        verify(userRepository, never())
                .save(any(User.class));

        verifyNoInteractions(auditLogService);
    }

    @Test
    void onboardEmployee_shouldRejectAlreadyOnboardedEmployee() {

        employee.setUser(user);

        when(employeeRepository.findById(1L))
                .thenReturn(Optional.of(employee));

        RuntimeException exception =
                assertThrows(
                        RuntimeException.class,
                        () -> employeeService.onboardEmployee(
                                1L,
                                "john.doe",
                                "Temp@123"
                        )
                );

        assertEquals(
                "Employee already has a login account",
                exception.getMessage()
        );

        verify(userRepository, never())
                .save(any(User.class));

        verifyNoInteractions(auditLogService);
    }

    @Test
    void onboardEmployee_shouldRejectBlankUsername() {

        when(employeeRepository.findById(1L))
                .thenReturn(Optional.of(employee));

        RuntimeException exception =
                assertThrows(
                        RuntimeException.class,
                        () -> employeeService.onboardEmployee(
                                1L,
                                "   ",
                                "Temp@123"
                        )
                );

        assertEquals(
                "Username is required",
                exception.getMessage()
        );

        verify(userRepository, never())
                .save(any(User.class));

        verifyNoInteractions(auditLogService);
    }

    @Test
    void onboardEmployee_shouldRejectBlankTemporaryPassword() {

        when(employeeRepository.findById(1L))
                .thenReturn(Optional.of(employee));

        RuntimeException exception =
                assertThrows(
                        RuntimeException.class,
                        () -> employeeService.onboardEmployee(
                                1L,
                                "john.doe",
                                "   "
                        )
                );

        assertEquals(
                "Temporary password is required",
                exception.getMessage()
        );

        verify(userRepository, never())
                .save(any(User.class));

        verifyNoInteractions(auditLogService);
    }

    @Test
    void onboardEmployee_shouldRejectDuplicateUsername() {

        when(employeeRepository.findById(1L))
                .thenReturn(Optional.of(employee));

        when(userRepository.findByUsername("john.doe"))
                .thenReturn(Optional.of(user));

        RuntimeException exception =
                assertThrows(
                        RuntimeException.class,
                        () -> employeeService.onboardEmployee(
                                1L,
                                "john.doe",
                                "Temp@123"
                        )
                );

        assertTrue(
                exception.getMessage()
                        .contains("already exists")
        );

        verify(userRepository, never())
                .save(any(User.class));

        verify(passwordEncoder, never())
                .encode(anyString());

        verifyNoInteractions(auditLogService);
    }

    // -------------------------------------------------------------------------
    // OFFBOARD EMPLOYEE
    // -------------------------------------------------------------------------

    @Test
    void offboardEmployee_shouldDeactivateEmployeeAndLogin() {

        employee.setUser(user);

        when(employeeRepository.findById(1L))
                .thenReturn(Optional.of(employee));

        when(userRepository.save(any(User.class)))
                .thenAnswer(invocation ->
                        invocation.getArgument(0)
                );

        when(employeeRepository.save(any(Employee.class)))
                .thenAnswer(invocation ->
                        invocation.getArgument(0)
                );

        EmployeeDTO result =
                employeeService.offboardEmployee(1L);

        assertEquals(
                "INACTIVE",
                result.getEmploymentStatus()
        );

        assertFalse(
                employee.getUser().isEnabled()
        );

        assertEquals(
                "john.doe",
                result.getUsername()
        );

        verify(userRepository)
                .save(user);

        verify(employeeRepository)
                .save(employee);

        verify(auditLogService)
                .log(
                        eq("EMPLOYEE_OFFBOARDED"),
                        eq("EMPLOYEE"),
                        eq(1L),
                        eq("Employee offboarded")
                );
    }

    @Test
    void offboardEmployee_shouldRejectMissingEmployee() {

        when(employeeRepository.findById(999L))
                .thenReturn(Optional.empty());

        RuntimeException exception =
                assertThrows(
                        RuntimeException.class,
                        () -> employeeService.offboardEmployee(999L)
                );

        assertEquals(
                "Employee not found with ID: 999",
                exception.getMessage()
        );

        verify(userRepository, never())
                .save(any(User.class));

        verifyNoInteractions(auditLogService);
    }

    // -------------------------------------------------------------------------
    // REACTIVATE EMPLOYEE
    // -------------------------------------------------------------------------

    @Test
    void reactivateEmployee_shouldActivateEmployeeAndLogin() {

        employee.setEmploymentStatus("INACTIVE");

        user.setEnabled(false);
        employee.setUser(user);

        when(employeeRepository.findById(1L))
                .thenReturn(Optional.of(employee));

        when(userRepository.save(any(User.class)))
                .thenAnswer(invocation ->
                        invocation.getArgument(0)
                );

        when(employeeRepository.save(any(Employee.class)))
                .thenAnswer(invocation ->
                        invocation.getArgument(0)
                );

        EmployeeDTO result =
                employeeService.reactivateEmployee(1L);

        assertEquals(
                "ACTIVE",
                result.getEmploymentStatus()
        );

        assertTrue(
                employee.getUser().isEnabled()
        );

        assertEquals(
                "john.doe",
                result.getUsername()
        );

        verify(userRepository)
                .save(user);

        verify(employeeRepository)
                .save(employee);

        verify(auditLogService)
                .log(
                        eq("EMPLOYEE_REACTIVATED"),
                        eq("EMPLOYEE"),
                        eq(1L),
                        eq("Employee reactivated")
                );
    }

    @Test
    void reactivateEmployee_shouldRejectMissingEmployee() {

        when(employeeRepository.findById(999L))
                .thenReturn(Optional.empty());

        RuntimeException exception =
                assertThrows(
                        RuntimeException.class,
                        () -> employeeService.reactivateEmployee(999L)
                );

        assertEquals(
                "Employee not found with ID: 999",
                exception.getMessage()
        );

        verify(userRepository, never())
                .save(any(User.class));

        verifyNoInteractions(auditLogService);
    }

    // -------------------------------------------------------------------------
    // GET EMPLOYEES
    // -------------------------------------------------------------------------

    @Test
    void getAllEmployees_shouldReturnEmployees() {

        when(employeeRepository.findAll())
                .thenReturn(List.of(employee));

        List<EmployeeDTO> result =
                employeeService.getAllEmployees();

        assertNotNull(result);
        assertEquals(1, result.size());

        assertEquals(
                "john.doe@example.com",
                result.get(0).getEmail()
        );
    }

    @Test
    void getEmployeeById_shouldReturnEmployee() {

        when(employeeRepository.findById(1L))
                .thenReturn(Optional.of(employee));

        EmployeeDTO result =
                employeeService.getEmployeeById(1L);

        assertNotNull(result);
        assertEquals(1L, result.getId());
        assertEquals("John", result.getFirstName());
    }

    @Test
    void getEmployeeById_shouldRejectMissingEmployee() {

        when(employeeRepository.findById(999L))
                .thenReturn(Optional.empty());

        RuntimeException exception =
                assertThrows(
                        RuntimeException.class,
                        () -> employeeService.getEmployeeById(999L)
                );

        assertEquals(
                "Employee not found with ID: 999",
                exception.getMessage()
        );
    }

    @Test
    void getEmployeeByEmail_shouldReturnEmployee() {

        when(employeeRepository.findByEmail(
                "john.doe@example.com"
        )).thenReturn(Optional.of(employee));

        EmployeeDTO result =
                employeeService.getEmployeeByEmail(
                        "john.doe@example.com"
                );

        assertNotNull(result);
        assertEquals(
                "John",
                result.getFirstName()
        );
    }

    // -------------------------------------------------------------------------
    // UPDATE EMPLOYEE
    // -------------------------------------------------------------------------

    @Test
    void updateEmployee_shouldUpdateFields() {

        EmployeeDTO updateDTO =
                new EmployeeDTO();

        updateDTO.setFirstName("Jane");
        updateDTO.setLastName("Smith");
        updateDTO.setDepartment("HR");
        updateDTO.setJobTitle("HR Manager");
        updateDTO.setSalary(85000.0);

        when(employeeRepository.findById(1L))
                .thenReturn(Optional.of(employee));

        when(employeeRepository.save(any(Employee.class)))
                .thenAnswer(invocation ->
                        invocation.getArgument(0)
                );

        EmployeeDTO result =
                employeeService.updateEmployee(
                        1L,
                        updateDTO
                );

        assertEquals(
                "Jane",
                result.getFirstName()
        );

        assertEquals(
                "Smith",
                result.getLastName()
        );

        assertEquals(
                "HR",
                result.getDepartment()
        );

        assertEquals(
                "HR Manager",
                result.getJobTitle()
        );

        assertEquals(
                85000.0,
                result.getSalary()
        );

        verify(auditLogService)
                .log(
                        eq("EMPLOYEE_UPDATED"),
                        eq("EMPLOYEE"),
                        eq(1L),
                        eq("Employee profile updated")
                );
    }

    @Test
    void updateEmployee_shouldRejectMissingEmployee() {

        when(employeeRepository.findById(999L))
                .thenReturn(Optional.empty());

        RuntimeException exception =
                assertThrows(
                        RuntimeException.class,
                        () -> employeeService.updateEmployee(
                                999L,
                                employeeDTO
                        )
                );

        assertEquals(
                "Employee not found with ID: 999",
                exception.getMessage()
        );

        verifyNoInteractions(auditLogService);
    }

    @Test
    void updateEmployee_shouldRejectDuplicateEmail() {

        Employee anotherEmployee =
                new Employee();

        anotherEmployee.setId(2L);
        anotherEmployee.setEmail(
                "another@example.com"
        );

        employeeDTO.setEmail(
                "another@example.com"
        );

        when(employeeRepository.findById(1L))
                .thenReturn(Optional.of(employee));

        when(employeeRepository.findByEmail(
                "another@example.com"
        )).thenReturn(Optional.of(anotherEmployee));

        RuntimeException exception =
                assertThrows(
                        RuntimeException.class,
                        () -> employeeService.updateEmployee(
                                1L,
                                employeeDTO
                        )
                );

        assertTrue(
                exception.getMessage()
                        .contains("Another employee already uses email")
        );

        verify(employeeRepository, never())
                .save(any(Employee.class));

        verifyNoInteractions(auditLogService);
    }

    @Test
    void updateEmployee_shouldDisableLinkedUserWhenStatusBecomesInactive() {

        employee.setUser(user);

        employeeDTO.setEmploymentStatus("INACTIVE");

        when(employeeRepository.findById(1L))
                .thenReturn(Optional.of(employee));

        when(userRepository.save(any(User.class)))
                .thenAnswer(invocation ->
                        invocation.getArgument(0)
                );

        when(employeeRepository.save(any(Employee.class)))
                .thenAnswer(invocation ->
                        invocation.getArgument(0)
                );

        EmployeeDTO result =
                employeeService.updateEmployee(
                        1L,
                        employeeDTO
                );

        assertEquals(
                "INACTIVE",
                result.getEmploymentStatus()
        );

        assertFalse(
                employee.getUser().isEnabled()
        );

        verify(userRepository)
                .save(user);

        verify(auditLogService)
                .log(
                        eq("EMPLOYEE_UPDATED"),
                        eq("EMPLOYEE"),
                        eq(1L),
                        eq("Employee profile updated")
                );
    }

    @Test
    void updateEmployee_shouldEnableLinkedUserWhenStatusBecomesActive() {

        employee.setEmploymentStatus("INACTIVE");

        user.setEnabled(false);
        employee.setUser(user);

        employeeDTO.setEmploymentStatus("ACTIVE");

        when(employeeRepository.findById(1L))
                .thenReturn(Optional.of(employee));

        when(userRepository.save(any(User.class)))
                .thenAnswer(invocation ->
                        invocation.getArgument(0)
                );

        when(employeeRepository.save(any(Employee.class)))
                .thenAnswer(invocation ->
                        invocation.getArgument(0)
                );

        EmployeeDTO result =
                employeeService.updateEmployee(
                        1L,
                        employeeDTO
                );

        assertEquals(
                "ACTIVE",
                result.getEmploymentStatus()
        );

        assertTrue(
                employee.getUser().isEnabled()
        );

        verify(userRepository)
                .save(user);

        verify(auditLogService)
                .log(
                        eq("EMPLOYEE_UPDATED"),
                        eq("EMPLOYEE"),
                        eq(1L),
                        eq("Employee profile updated")
                );
    }

    // -------------------------------------------------------------------------
    // DELETE EMPLOYEE
    // -------------------------------------------------------------------------

    @Test
    void deleteEmployee_shouldDeleteExistingEmployee() {

        when(employeeRepository.findById(1L))
                .thenReturn(Optional.of(employee));

        employeeService.deleteEmployee(1L);

        verify(employeeRepository)
                .findById(1L);

        verify(employeeRepository)
                .delete(employee);

        verify(auditLogService)
                .log(
                        eq("EMPLOYEE_DELETED"),
                        eq("EMPLOYEE"),
                        eq(1L),
                        contains("Employee deleted")
                );
    }

    @Test
    void deleteEmployee_shouldRejectMissingEmployee() {

        when(employeeRepository.findById(999L))
                .thenReturn(Optional.empty());

        RuntimeException exception =
                assertThrows(
                        RuntimeException.class,
                        () -> employeeService.deleteEmployee(999L)
                );

        assertEquals(
                "Employee not found with ID: 999",
                exception.getMessage()
        );

        verify(employeeRepository, never())
                .delete(any(Employee.class));

        verifyNoInteractions(auditLogService);
    }

    // -------------------------------------------------------------------------
    // SEARCH
    // -------------------------------------------------------------------------

    @Test
    void searchEmployees_shouldFindByNameEmailDepartmentOrJobTitle() {

        Employee secondEmployee =
                new Employee(
                        "Alice",
                        "Johnson",
                        "alice@example.com",
                        "9999999999",
                        "Finance",
                        "Accountant",
                        LocalDate.of(2026, 2, 1),
                        65000.0,
                        "ACTIVE"
                );

        secondEmployee.setId(2L);

        when(employeeRepository.findAll())
                .thenReturn(
                        List.of(
                                employee,
                                secondEmployee
                        )
                );

        List<EmployeeDTO> result =
                employeeService.searchEmployees("finance");

        assertEquals(1, result.size());

        assertEquals(
                "alice@example.com",
                result.get(0).getEmail()
        );
    }

    // -------------------------------------------------------------------------
    // DEPARTMENT FILTER
    // -------------------------------------------------------------------------

    @Test
    void getEmployeesByDepartment_shouldReturnMatchingEmployees() {

        when(employeeRepository.findAll())
                .thenReturn(List.of(employee));

        List<EmployeeDTO> result =
                employeeService.getEmployeesByDepartment("it");

        assertEquals(1, result.size());

        assertEquals(
                "IT",
                result.get(0).getDepartment()
        );
    }

    @Test
    void getEmployeesByDepartment_shouldReturnEmptyForNoMatch() {

        when(employeeRepository.findAll())
                .thenReturn(List.of(employee));

        List<EmployeeDTO> result =
                employeeService.getEmployeesByDepartment(
                        "Finance"
                );

        assertTrue(result.isEmpty());
    }

    // -------------------------------------------------------------------------
    // STATUS FILTER
    // -------------------------------------------------------------------------

    @Test
    void getEmployeesByStatus_shouldReturnMatchingEmployees() {

        when(employeeRepository.findAll())
                .thenReturn(List.of(employee));

        List<EmployeeDTO> result =
                employeeService.getEmployeesByStatus("active");

        assertEquals(1, result.size());

        assertEquals(
                "ACTIVE",
                result.get(0).getEmploymentStatus()
        );
    }

    @Test
    void getEmployeesByStatus_shouldReturnEmptyForNoMatch() {

        when(employeeRepository.findAll())
                .thenReturn(List.of(employee));

        List<EmployeeDTO> result =
                employeeService.getEmployeesByStatus(
                        "INACTIVE"
                );

        assertTrue(result.isEmpty());
    }

    // -------------------------------------------------------------------------
    // DTO USER MAPPING
    // -------------------------------------------------------------------------

    @Test
    void getEmployeeById_shouldIncludeLinkedUserInformation() {

        employee.setUser(user);

        when(employeeRepository.findById(1L))
                .thenReturn(Optional.of(employee));

        EmployeeDTO result =
                employeeService.getEmployeeById(1L);

        assertEquals(
                10L,
                result.getUserId()
        );

        assertEquals(
                "john.doe",
                result.getUsername()
        );
    }
}