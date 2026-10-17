import { useEffect, useMemo, useState } from "react";

import { apiFetch } from "./api/apiClient";



const API_URL = "/employees";



function Icon({ children, size = 18 }) {

  return (

    <svg

      width={size}

      height={size}

      viewBox="0 0 24 24"

      fill="none"

      stroke="currentColor"

      strokeWidth="1.8"

      strokeLinecap="round"

      strokeLinejoin="round"

    >

      {children}

    </svg>

  );

}



const Search = ({ size = 18 }) => (

  <Icon size={size}>

    <circle cx="10.5" cy="10.5" r="6.5" />

    <path d="m16 16 5 5" />

  </Icon>

);



const Plus = ({ size = 18 }) => (

  <Icon size={size}>

    <path d="M12 5v14M5 12h14" />

  </Icon>

);



const Edit = ({ size = 17 }) => (

  <Icon size={size}>

    <path d="M12 20h9" />

    <path d="M16.5 3.5a2.1 2.1 0 0 1 3 3L8 18l-4 1 1-4Z" />

  </Icon>

);



const Trash = ({ size = 17 }) => (

  <Icon size={size}>

    <path d="M3 6h18" />

    <path d="M8 6V4h8v2" />

    <path d="M19 6l-1 15H6L5 6" />

    <path d="M10 11v6M14 11v6" />

  </Icon>

);



const Eye = ({ size = 17 }) => (

  <Icon size={size}>

    <path d="M2 12s3.5-7 10-7 10 7 10 7-3.5 7-10 7S2 12 2 12Z" />

    <circle cx="12" cy="12" r="3" />

  </Icon>

);



const X = ({ size = 18 }) => (

  <Icon size={size}>

    <path d="M6 6l12 12M18 6 6 18" />

  </Icon>

);



const Users = ({ size = 20 }) => (

  <Icon size={size}>

    <path d="M16 21v-2a4 4 0 0 0-4-4H6a4 4 0 0 0-4 4v2" />

    <circle cx="9" cy="7" r="4" />

    <path d="M22 21v-2a4 4 0 0 0-3-3.87" />

    <path d="M16 3.13a4 4 0 0 1 0 7.75" />

  </Icon>

);



const Briefcase = ({ size = 20 }) => (

  <Icon size={size}>

    <rect x="3" y="7" width="18" height="13" rx="2" />

    <path d="M8 7V5a2 2 0 0 1 2-2h4a2 2 0 0 1 2 2v2" />

    <path d="M3 12h18" />

  </Icon>

);



const CheckCircle = ({ size = 20 }) => (

  <Icon size={size}>

    <circle cx="12" cy="12" r="9" />

    <path d="m8 12 2.5 2.5L16 9" />

  </Icon>

);



const XCircle = ({ size = 20 }) => (

  <Icon size={size}>

    <circle cx="12" cy="12" r="9" />

    <path d="m9 9 6 6M15 9l-6 6" />

  </Icon>

);



function emptyForm() {

  return {

    firstName: "",

    lastName: "",

    email: "",

    phone: "",

    department: "",

    jobTitle: "",

    joiningDate: "",

    salary: "",

    employmentStatus: "ACTIVE",

  };

}



function getEmployeeName(employee) {

  return (

    `${employee?.firstName || ""} ${employee?.lastName || ""}`.trim() ||

    employee?.email ||

    `Employee #${employee?.id ?? ""}`

  );

}



function getInitials(employee) {

  const first = employee?.firstName?.charAt(0) || "";

  const last = employee?.lastName?.charAt(0) || "";



  const initials = `${first}${last}`.toUpperCase();



  return initials || "U";

}



function formatDate(value) {

  if (!value) {

    return "—";

  }



  const date = new Date(`${String(value).slice(0, 10)}T00:00:00`);



  if (Number.isNaN(date.getTime())) {

    return value;

  }



  return date.toLocaleDateString("en-IN", {

    day: "2-digit",

    month: "short",

    year: "numeric",

  });

}



function formatSalary(value) {

  if (value === null || value === undefined || value === "") {

    return "—";

  }



  const number = Number(value);



  if (Number.isNaN(number)) {

    return value;

  }



  return new Intl.NumberFormat("en-IN", {

    style: "currency",

    currency: "INR",

    maximumFractionDigits: 0,

  }).format(number);

}



function normalizeEmployee(employee) {

  return {

    ...employee,

    id: employee?.id,

    firstName: employee?.firstName || "",

    lastName: employee?.lastName || "",

    email: employee?.email || "",

    phone: employee?.phone || "",

    department: employee?.department || "",

    jobTitle: employee?.jobTitle || "",

    joiningDate: employee?.joiningDate || "",

    salary: employee?.salary ?? "",

    employmentStatus: employee?.employmentStatus || "ACTIVE",

  };

}



function Employee() {

  const [employees, setEmployees] = useState([]);

  const [loading, setLoading] = useState(true);

  const [error, setError] = useState("");



  const [search, setSearch] = useState("");

  const [statusFilter, setStatusFilter] = useState("ALL");

  const [departmentFilter, setDepartmentFilter] = useState("ALL");



  const [showModal, setShowModal] = useState(false);

  const [showDetails, setShowDetails] = useState(false);



  const [editingEmployee, setEditingEmployee] = useState(null);

  const [selectedEmployee, setSelectedEmployee] = useState(null);



  const [form, setForm] = useState(emptyForm());

  const [saving, setSaving] = useState(false);

  const [deletingId, setDeletingId] = useState(null);



  /*

   * ============================================================

   * LOAD EMPLOYEES

   * ============================================================

   */



  const loadEmployees = async () => {

    try {

      setLoading(true);

      setError("");



      console.log("Loading employees...");



      const data = await apiFetch(API_URL);



      console.log("Employees received:", data);



      const employeeList = Array.isArray(data) ? data : [];



      setEmployees(employeeList.map(normalizeEmployee));

    } catch (err) {

      console.error("Unable to load employees:", err);



      setEmployees([]);

      setError(err.message || "Unable to load employees.");

    } finally {

      setLoading(false);

    }

  };



  /*

   * ============================================================

   * INITIAL LOAD

   * ============================================================

   */



  useEffect(() => {

    loadEmployees();

  }, []);



  /*

   * ============================================================

   * FILTERS

   * ============================================================

   */



  const departments = useMemo(() => {

    const values = employees

      .map((employee) => employee.department)

      .filter(Boolean);



    return [...new Set(values)].sort((a, b) =>

      String(a).localeCompare(String(b))

    );

  }, [employees]);



  const filteredEmployees = useMemo(() => {

    const searchValue = search.trim().toLowerCase();



    return employees.filter((employee) => {

      const name = getEmployeeName(employee).toLowerCase();



      const email = String(employee.email || "").toLowerCase();

      const phone = String(employee.phone || "").toLowerCase();

      const department = String(

        employee.department || ""

      ).toLowerCase();

      const jobTitle = String(

        employee.jobTitle || ""

      ).toLowerCase();



      const matchesSearch =

        !searchValue ||

        name.includes(searchValue) ||

        email.includes(searchValue) ||

        phone.includes(searchValue) ||

        department.includes(searchValue) ||

        jobTitle.includes(searchValue);



      const matchesStatus =

        statusFilter === "ALL" ||

        employee.employmentStatus === statusFilter;



      const matchesDepartment =

        departmentFilter === "ALL" ||

        employee.department === departmentFilter;



      return (

        matchesSearch &&

        matchesStatus &&

        matchesDepartment

      );

    });

  }, [

    employees,

    search,

    statusFilter,

    departmentFilter,

  ]);



  /*

   * ============================================================

   * STATISTICS

   * ============================================================

   */



  const totalEmployees = employees.length;



  const activeEmployees = employees.filter(

    (employee) =>

      employee.employmentStatus === "ACTIVE"

  ).length;



  const inactiveEmployees = employees.filter(

    (employee) =>

      employee.employmentStatus !== "ACTIVE"

  ).length;



  /*

   * ============================================================

   * FORM

   * ============================================================

   */



  const handleInputChange = (event) => {

    const { name, value } = event.target;



    setForm((previous) => ({

      ...previous,

      [name]: value,

    }));

  };



  const openAddModal = () => {

    setEditingEmployee(null);

    setForm(emptyForm());

    setError("");

    setShowModal(true);

  };



  const openEditModal = (employee) => {

    setEditingEmployee(employee);



    setForm({

      firstName: employee.firstName || "",

      lastName: employee.lastName || "",

      email: employee.email || "",

      phone: employee.phone || "",

      department: employee.department || "",

      jobTitle: employee.jobTitle || "",

      joiningDate: employee.joiningDate || "",

      salary: employee.salary ?? "",

      employmentStatus:

        employee.employmentStatus || "ACTIVE",

    });



    setError("");

    setShowModal(true);

  };



  const closeModal = () => {

    setShowModal(false);

    setEditingEmployee(null);

    setForm(emptyForm());

  };



  /*

   * ============================================================

   * CREATE / UPDATE

   * ============================================================

   */



  const handleSubmit = async (event) => {

    event.preventDefault();



    try {

      setSaving(true);

      setError("");



      const payload = {

        firstName: form.firstName.trim(),

        lastName: form.lastName.trim(),

        email: form.email.trim(),

        phone: form.phone.trim(),

        department: form.department.trim(),

        jobTitle: form.jobTitle.trim(),

        joiningDate: form.joiningDate,

        salary:

          form.salary === "" ||

          form.salary === null

            ? null

            : Number(form.salary),

        employmentStatus:

          form.employmentStatus,

      };



      if (!payload.firstName) {

        throw new Error("First name is required.");

      }



      if (!payload.lastName) {

        throw new Error("Last name is required.");

      }



      if (!payload.email) {

        throw new Error("Email is required.");

      }



      if (

        payload.salary !== null &&

        Number.isNaN(payload.salary)

      ) {

        throw new Error("Salary must be a valid number.");

      }



      if (editingEmployee) {

        await apiFetch(

          `${API_URL}/${editingEmployee.id}`,

          {

            method: "PUT",

            body: JSON.stringify(payload),

          }

        );

      } else {

        await apiFetch(API_URL, {

          method: "POST",

          body: JSON.stringify(payload),

        });

      }



      closeModal();



      await loadEmployees();

    } catch (err) {

      console.error("Unable to save employee:", err);



      setError(

        err.message || "Unable to save employee."

      );

    } finally {

      setSaving(false);

    }

  };



  /*

   * ============================================================

   * DELETE

   * ============================================================

   */



  const handleDelete = async (employee) => {

    const employeeName = getEmployeeName(employee);



    const confirmed = window.confirm(

      `Are you sure you want to delete ${employeeName}?`

    );



    if (!confirmed) {

      return;

    }



    try {

      setDeletingId(employee.id);

      setError("");



      await apiFetch(

        `${API_URL}/${employee.id}`,

        {

          method: "DELETE",

        }

      );



      setEmployees((previous) =>

        previous.filter(

          (item) => item.id !== employee.id

        )

      );



      if (selectedEmployee?.id === employee.id) {

        setSelectedEmployee(null);

        setShowDetails(false);

      }

    } catch (err) {

      console.error(

        "Unable to delete employee:",

        err

      );



      setError(

        err.message || "Unable to delete employee."

      );

    } finally {

      setDeletingId(null);

    }

  };



  /*

   * ============================================================

   * DETAILS

   * ============================================================

   */



  const openDetails = (employee) => {

    setSelectedEmployee(employee);

    setShowDetails(true);

  };



  const closeDetails = () => {

    setSelectedEmployee(null);

    setShowDetails(false);

  };



  /*

   * ============================================================

   * RENDER

   * ============================================================

   */



  return (

    <section className="nx-page">

      <div className="nx-page-header">

        <div>

          <h1>Employee Management</h1>



          <p>

            Manage your organization's employees and

            workforce data.

          </p>

        </div>



        <button

          type="button"

          className="nx-primary-btn"

          onClick={openAddModal}

        >

          <Plus size={18} />

          Add Employee

        </button>

      </div>



      {/* ========================================================

          STATISTICS

      ======================================================== */}



      <div className="nx-stats-grid">

        <div className="nx-stat-card">

          <div className="nx-stat-icon">

            <Users />

          </div>



          <div>

            <span>Total Employees</span>

            <strong>{totalEmployees}</strong>

          </div>

        </div>



        <div className="nx-stat-card">

          <div className="nx-stat-icon">

            <CheckCircle />

          </div>



          <div>

            <span>Active Employees</span>

            <strong>{activeEmployees}</strong>

          </div>

        </div>



        <div className="nx-stat-card">

          <div className="nx-stat-icon">

            <XCircle />

          </div>



          <div>

            <span>Inactive Employees</span>

            <strong>{inactiveEmployees}</strong>

          </div>

        </div>



        <div className="nx-stat-card">

          <div className="nx-stat-icon">

            <Briefcase />

          </div>



          <div>

            <span>Departments</span>

            <strong>{departments.length}</strong>

          </div>

        </div>

      </div>



      {/* ========================================================

          ERROR

      ======================================================== */}



      {error && (

        <div className="nx-error-box">

          <div>

            <strong>Error</strong>



            <p>{error}</p>

          </div>



          <button

            type="button"

            className="nx-secondary-btn"

            onClick={loadEmployees}

            disabled={loading}

          >

            {loading ? "Loading..." : "Try Again"}

          </button>

        </div>

      )}



      {/* ========================================================

          FILTERS

      ======================================================== */}



      <div className="nx-toolbar">

        <div className="nx-search-box">

          <Search size={18} />



          <input

            type="text"

            placeholder="Search employees..."

            value={search}

            onChange={(event) =>

              setSearch(event.target.value)

            }

          />

        </div>



        <select

          value={statusFilter}

          onChange={(event) =>

            setStatusFilter(event.target.value)

          }

        >

          <option value="ALL">

            All Statuses

          </option>



          <option value="ACTIVE">

            Active

          </option>



          <option value="INACTIVE">

            Inactive

          </option>

        </select>



        <select

          value={departmentFilter}

          onChange={(event) =>

            setDepartmentFilter(event.target.value)

          }

        >

          <option value="ALL">

            All Departments

          </option>



          {departments.map((department) => (

            <option

              key={`department-${department}`}

              value={department}

            >

              {department}

            </option>

          ))}

        </select>



        <button

          type="button"

          className="nx-secondary-btn"

          onClick={loadEmployees}

          disabled={loading}

        >

          {loading ? "Loading..." : "Refresh"}

        </button>

      </div>



      {/* ========================================================

          EMPLOYEE TABLE

      ======================================================== */}



      <div className="nx-table-card">

        {loading ? (

          <div className="nx-empty-state">

            <div className="nx-loader" />



            <p>Loading employees...</p>

          </div>

        ) : filteredEmployees.length === 0 ? (

          <div className="nx-empty-state">

            <Users size={40} />



            <h3>No employees found</h3>



            <p>

              {employees.length === 0

                ? "There are currently no employees in the system."

                : "Try changing your search or filters."}

            </p>



            {employees.length === 0 && (

              <button

                type="button"

                className="nx-primary-btn"

                onClick={openAddModal}

              >

                <Plus size={18} />

                Add Employee

              </button>

            )}

          </div>

        ) : (

          <div className="nx-table-wrapper">

            <table className="nx-table">

              <thead>

                <tr>

                  <th>Employee</th>

                  <th>Department</th>

                  <th>Job Title</th>

                  <th>Joining Date</th>

                  <th>Salary</th>

                  <th>Status</th>

                  <th>Actions</th>

                </tr>

              </thead>



              <tbody>

                {filteredEmployees.map((employee) => (

                  <tr

                    key={`employee-${employee.id}`}

                  >

                    <td>

                      <div className="nx-employee-cell">

                        <div className="nx-avatar">

                          {getInitials(employee)}

                        </div>



                        <div>

                          <strong>

                            {getEmployeeName(employee)}

                          </strong>



                          <span>

                            {employee.email}

                          </span>

                        </div>

                      </div>

                    </td>



                    <td>

                      {employee.department || "—"}

                    </td>



                    <td>

                      {employee.jobTitle || "—"}

                    </td>



                    <td>

                      {formatDate(

                        employee.joiningDate

                      )}

                    </td>



                    <td>

                      {formatSalary(employee.salary)}

                    </td>



                    <td>

                      <span

                        className={`nx-status ${

                          employee.employmentStatus ===

                          "ACTIVE"

                            ? "active"

                            : "inactive"

                        }`}

                      >

                        {employee.employmentStatus ||

                          "UNKNOWN"}

                      </span>

                    </td>



                    <td>
                      <div className="nx-action-buttons">
                        <button
                          type="button"
                          className="nx-action-btn nx-view-btn"
                          title="View employee"
                          onClick={() => openDetails(employee)}
                        >
                          <Eye size={16} />
                          <span>View</span>
                        </button>

                        <button
                          type="button"
                          className="nx-action-btn nx-edit-btn"
                          title="Edit employee"
                          onClick={() => openEditModal(employee)}
                        >
                          <Edit size={16} />
                          <span>Edit</span>
                        </button>

                        <button
                          type="button"
                          className="nx-action-btn nx-delete-btn"
                          title="Delete employee"
                          disabled={deletingId === employee.id}
                          onClick={() => handleDelete(employee)}
                        >
                          <Trash size={16} />
                          <span>Delete</span>
                        </button>
                      </div>

                    </td>

                  </tr>

                ))}

              </tbody>

            </table>

          </div>

        )}

      </div>



      {/* ========================================================

          ADD / EDIT MODAL

      ======================================================== */}



      {showModal && (

        <div className="nx-modal-overlay">

          <div className="nx-modal">

            <div className="nx-modal-header">

              <div>

                <h2>

                  {editingEmployee

                    ? "Edit Employee"

                    : "Add Employee"}

                </h2>



                <p>

                  {editingEmployee

                    ? "Update employee information."

                    : "Enter the employee's information."}

                </p>

              </div>



              <button

                type="button"

                className="nx-modal-close"

                onClick={closeModal}

                disabled={saving}

              >

                <X size={20} />

              </button>

            </div>



            <form onSubmit={handleSubmit}>

              <div className="nx-form-grid">

                <div className="nx-form-group">

                  <label htmlFor="firstName">

                    First Name *

                  </label>



                  <input

                    id="firstName"

                    name="firstName"

                    value={form.firstName}

                    onChange={handleInputChange}

                    placeholder="Enter first name"

                    required

                  />

                </div>



                <div className="nx-form-group">

                  <label htmlFor="lastName">

                    Last Name *

                  </label>



                  <input

                    id="lastName"

                    name="lastName"

                    value={form.lastName}

                    onChange={handleInputChange}

                    placeholder="Enter last name"

                    required

                  />

                </div>



                <div className="nx-form-group">

                  <label htmlFor="email">

                    Email *

                  </label>



                  <input

                    id="email"

                    name="email"

                    type="email"

                    value={form.email}

                    onChange={handleInputChange}

                    placeholder="employee@example.com"

                    required

                  />

                </div>



                <div className="nx-form-group">

                  <label htmlFor="phone">

                    Phone

                  </label>



                  <input

                    id="phone"

                    name="phone"

                    value={form.phone}

                    onChange={handleInputChange}

                    placeholder="Enter phone number"

                  />

                </div>



                <div className="nx-form-group">

                  <label htmlFor="department">

                    Department

                  </label>



                  <input

                    id="department"

                    name="department"

                    value={form.department}

                    onChange={handleInputChange}

                    placeholder="e.g. Engineering"

                  />

                </div>



                <div className="nx-form-group">

                  <label htmlFor="jobTitle">

                    Job Title

                  </label>



                  <input

                    id="jobTitle"

                    name="jobTitle"

                    value={form.jobTitle}

                    onChange={handleInputChange}

                    placeholder="e.g. Software Engineer"

                  />

                </div>



                <div className="nx-form-group">

                  <label htmlFor="joiningDate">

                    Joining Date

                  </label>



                  <input

                    id="joiningDate"

                    name="joiningDate"

                    type="date"

                    value={form.joiningDate}

                    onChange={handleInputChange}

                  />

                </div>



                <div className="nx-form-group">

                  <label htmlFor="salary">

                    Salary

                  </label>



                  <input

                    id="salary"

                    name="salary"

                    type="number"

                    min="0"

                    step="0.01"

                    value={form.salary}

                    onChange={handleInputChange}

                    placeholder="Enter salary"

                  />

                </div>



                <div className="nx-form-group">

                  <label htmlFor="employmentStatus">

                    Employment Status

                  </label>



                  <select

                    id="employmentStatus"

                    name="employmentStatus"

                    value={form.employmentStatus}

                    onChange={handleInputChange}

                  >

                    <option value="ACTIVE">

                      Active

                    </option>



                    <option value="INACTIVE">

                      Inactive

                    </option>

                  </select>

                </div>

              </div>



              <div className="nx-modal-footer">

                <button

                  type="button"

                  className="nx-secondary-btn"

                  onClick={closeModal}

                  disabled={saving}

                >

                  Cancel

                </button>



                <button

                  type="submit"

                  className="nx-primary-btn"

                  disabled={saving}

                >

                  {saving

                    ? "Saving..."

                    : editingEmployee

                      ? "Update Employee"

                      : "Add Employee"}

                </button>

              </div>

            </form>

          </div>

        </div>

      )}



      {/* ========================================================

          DETAILS MODAL

      ======================================================== */}



      {showDetails && selectedEmployee && (

        <div className="nx-modal-overlay">

          <div className="nx-modal nx-details-modal">

            <div className="nx-modal-header">

              <div>

                <h2>Employee Details</h2>



                <p>

                  Complete employee information.

                </p>

              </div>



              <button

                type="button"

                className="nx-modal-close"

                onClick={closeDetails}

              >

                <X size={20} />

              </button>

            </div>



            <div className="nx-profile-header">

              <div className="nx-profile-avatar">

                {getInitials(selectedEmployee)}

              </div>



              <div>

                <h3>

                  {getEmployeeName(selectedEmployee)}

                </h3>



                <p>

                  {selectedEmployee.jobTitle ||

                    "Employee"}

                </p>



                <span

                  className={`nx-status ${

                    selectedEmployee.employmentStatus ===

                    "ACTIVE"

                      ? "active"

                      : "inactive"

                  }`}

                >

                  {selectedEmployee.employmentStatus ||

                    "UNKNOWN"}

                </span>

              </div>

            </div>



            <div className="nx-details-grid">

              <div className="nx-detail-item">

                <span>Email</span>



                <strong>

                  {selectedEmployee.email || "—"}

                </strong>

              </div>



              <div className="nx-detail-item">

                <span>Phone</span>



                <strong>

                  {selectedEmployee.phone || "—"}

                </strong>

              </div>



              <div className="nx-detail-item">

                <span>Department</span>



                <strong>

                  {selectedEmployee.department ||

                    "—"}

                </strong>

              </div>



              <div className="nx-detail-item">

                <span>Job Title</span>



                <strong>

                  {selectedEmployee.jobTitle ||

                    "—"}

                </strong>

              </div>



              <div className="nx-detail-item">

                <span>Joining Date</span>



                <strong>

                  {formatDate(

                    selectedEmployee.joiningDate

                  )}

                </strong>

              </div>



              <div className="nx-detail-item">

                <span>Salary</span>



                <strong>

                  {formatSalary(

                    selectedEmployee.salary

                  )}

                </strong>

              </div>



              <div className="nx-detail-item">

                <span>Employee ID</span>



                <strong>

                  #{selectedEmployee.id}

                </strong>

              </div>



              <div className="nx-detail-item">

                <span>Status</span>



                <strong>

                  {selectedEmployee.employmentStatus ||

                    "—"}

                </strong>

              </div>

            </div>



            <div className="nx-modal-footer">

              <button

                type="button"

                className="nx-secondary-btn"

                onClick={closeDetails}

              >

                Close

              </button>



              <button

                type="button"

                className="nx-primary-btn"

                onClick={() => {

                  const employee =

                    selectedEmployee;



                  closeDetails();

                  openEditModal(employee);

                }}

              >

                <Edit size={17} />

                Edit Employee

              </button>

            </div>

          </div>

        </div>

      )}

    </section>

  );

}



export default Employee;