import { useState } from "react";

import "./Login.css";

import {
  apiUrl,
} from "./api/apiClient";
import { saveAuthSession } from "./api/authSession";

const LOGIN_API =
  apiUrl("/auth/login");

const FORGOT_PASSWORD_API =
  apiUrl("/auth/forgot-password");

const RESET_PASSWORD_API =
  apiUrl("/auth/reset-password");

function Login({
  onLogin,
}) {

  const [
    username,
    setUsername,
  ] = useState(() => {
    try {
      return localStorage.getItem("nexushr-login-username") || "";
    } catch {
      return "";
    }
  });

  const [
    password,
    setPassword,
  ] = useState("");

  const [
    remember,
    setRemember,
  ] = useState(true);

  const [
    showPassword,
    setShowPassword,
  ] = useState(false);

  const [
    loading,
    setLoading,
  ] = useState(false);

  const [
    error,
    setError,
  ] = useState("");

  const [
    resetMode,
    setResetMode,
  ] = useState(false);

  const [
    resetStep,
    setResetStep,
  ] = useState("request");

  const [
    resetIdentity,
    setResetIdentity,
  ] = useState("");

  const [
    resetUsername,
    setResetUsername,
  ] = useState("");

  const [
    resetCode,
    setResetCode,
  ] = useState("");

  const [
    desiredUsername,
    setDesiredUsername,
  ] = useState("");

  const [
    newPassword,
    setNewPassword,
  ] = useState("");

  const [
    confirmPassword,
    setConfirmPassword,
  ] = useState("");

  const [
    showNewPassword,
    setShowNewPassword,
  ] = useState(false);

  const [
    resetMessage,
    setResetMessage,
  ] = useState("");

  // =========================================================
  // LOGIN
  // =========================================================

  async function handleSubmit(
    event
  ) {

    event.preventDefault();

    setError("");
    setResetMessage("");

    const cleanUsername =
      username.trim();

    if (
      !cleanUsername ||
      !password
    ) {

      setError(
        "Please enter your username/email and password."
      );

      return;
    }

    setLoading(true);

    try {

      const response =
        await fetch(
          LOGIN_API,
          {
            method: "POST",

            credentials:
              "include",

            headers: {
              "Content-Type":
                "application/json",
            },

            body:
              JSON.stringify({
                username:
                  cleanUsername,

                password,
              }),
          }
        );

      let data = {};

      try {

        data =
          await response.json();

      } catch {

        data = {};
      }

      if (!response.ok) {

        throw new Error(
          data?.message ||
            "Invalid username/email or password."
        );
      }

      if (remember) {

        localStorage.setItem(
          "nexushr-login-username",
          cleanUsername
        );

      } else {

        localStorage.removeItem(
          "nexushr-login-username"
        );
      }

      const session = saveAuthSession(data);

      onLogin(
        session
      );

    } catch (err) {

      setError(
        err?.message ||
          "Unable to sign in."
      );

    } finally {

      setLoading(
        false
      );
    }
  }

  // =========================================================
  // REQUEST OTP
  // =========================================================

  async function handleForgotPassword(
    event
  ) {

    event.preventDefault();

    setError("");
    setResetMessage("");

    const identity =
      resetIdentity.trim();

    if (!identity) {

      setError(
        "Enter your registered email or username."
      );

      return;
    }

    setLoading(
      true
    );

    try {

      const response =
        await fetch(
          FORGOT_PASSWORD_API,
          {
            method:
              "POST",

            headers: {
              "Content-Type":
                "application/json",
            },

            body:
              JSON.stringify({
                usernameOrEmail:
                  identity,
              }),
          }
        );

      let data = {};

      try {

        data =
          await response.json();

      } catch {

        data = {};
      }

      if (!response.ok) {

        throw new Error(
          data?.message ||
            data?.error ||
            "Unable to request reset code."
        );
      }

      /*
       * IMPORTANT:
       *
       * Keep exact email/username
       * through reset flow.
       *
       * Do not ask employee to
       * manually retype a generated
       * internal username.
       */
      setResetUsername(
        identity
      );

      setResetStep(
        "reset"
      );

      setResetMessage(
        data?.message ||
          "If the account exists, a reset code has been sent."
      );

    } catch (err) {

      setError(
        err?.message ||
          "Unable to request reset code."
      );

    } finally {

      setLoading(
        false
      );
    }
  }

  // =========================================================
  // RESET PASSWORD / FIRST-TIME SETUP
  // =========================================================

  async function handleResetPassword(
    event
  ) {

    event.preventDefault();

    setError("");
    setResetMessage("");

    if (
      !resetUsername.trim()
    ) {

      setError(
        "Username or registered email is required."
      );

      return;
    }

    if (
      !/^\d{6}$/.test(
        resetCode.trim()
      )
    ) {

      setError(
        "Enter the 6-digit reset code."
      );

      return;
    }

    if (
      !newPassword ||
      newPassword.length < 8
    ) {

      setError(
        "Password must contain at least 8 characters."
      );

      return;
    }

    if (
      !/[A-Z]/.test(
        newPassword
      )
    ) {

      setError(
        "Password must contain an uppercase letter."
      );

      return;
    }

    if (
      !/[a-z]/.test(
        newPassword
      )
    ) {

      setError(
        "Password must contain a lowercase letter."
      );

      return;
    }

    if (
      !/\d/.test(
        newPassword
      )
    ) {

      setError(
        "Password must contain a number."
      );

      return;
    }

    if (
      newPassword !==
      confirmPassword
    ) {

      setError(
        "Passwords do not match."
      );

      return;
    }

    setLoading(
      true
    );

    try {

      const response =
        await fetch(
          RESET_PASSWORD_API,
          {
            method:
              "POST",

            headers: {
              "Content-Type":
                "application/json",
            },

            body:
              JSON.stringify({
                username:
                  resetUsername.trim(),

                code:
                  resetCode.trim(),

                newPassword,

                /*
                 * Backend only accepts this
                 * for EMPLOYEE pending_xxx
                 * first-time setup.
                 *
                 * Existing accounts cannot
                 * change username.
                 */
                desiredUsername:
                  desiredUsername
                    .trim() ||
                  null,
              }),
          }
        );

      let data = {};

      try {

        data =
          await response.json();

      } catch {

        data = {};
      }

      if (!response.ok) {

        throw new Error(
          data?.message ||
            data?.error ||
            "Unable to reset password."
        );
      }

      setResetMessage(
        data?.message ||
          "Password reset successfully."
      );

      /*
       * If employee chose username,
       * place it in login field.
       *
       * Otherwise use same identifier.
       */
      if (
        desiredUsername.trim()
      ) {

        setUsername(
          desiredUsername
            .trim()
            .toLowerCase()
        );

      } else {

        setUsername(
          resetUsername.trim()
        );
      }

      setPassword("");

      setResetCode("");

      setDesiredUsername("");

      setNewPassword("");

      setConfirmPassword("");

      setTimeout(
        () => {

          setResetMode(
            false
          );

          setResetStep(
            "request"
          );

          setResetIdentity("");

          setResetUsername("");

          setError("");

        },
        1000
      );

    } catch (err) {

      setError(
        err?.message ||
          "Unable to reset password."
      );

    } finally {

      setLoading(
        false
      );
    }
  }

  function openForgotPassword() {

    setResetMode(
      true
    );

    setResetStep(
      "request"
    );

    setResetIdentity(
      username.trim()
    );

    setResetUsername("");

    setResetCode("");

    setDesiredUsername("");

    setNewPassword("");

    setConfirmPassword("");

    setResetMessage("");

    setError("");
  }

  function backToLogin() {

    setResetMode(
      false
    );

    setResetStep(
      "request"
    );

    setResetIdentity("");

    setResetUsername("");

    setResetCode("");

    setDesiredUsername("");

    setNewPassword("");

    setConfirmPassword("");

    setResetMessage("");

    setError("");
  }

  // =========================================================
  // UI
  // =========================================================

  return (
    <main className="nx-login">
      <div className="nx-login-grid" aria-hidden="true" />
      <div className="nx-login-glow nx-login-glow-a" aria-hidden="true" />
      <div className="nx-login-glow nx-login-glow-b" aria-hidden="true" />
      <div className="nx-login-glow nx-login-glow-c" aria-hidden="true" />

      <section className="nx-login-shell">
        <aside className="nx-login-brand-panel">
          <div className="nx-login-brand-top">
            <div className="nx-login-logo">N</div>
            <div>
              <strong>NexusHR</strong>
              <span>Enterprise Workforce Intelligence</span>
            </div>
          </div>

          <div className="nx-login-brand-content">
            <div className="nx-login-live-pill">
              <i /> Secure workforce workspace
            </div>

            <h1>
              One intelligent place for your <em>people operations.</em>
            </h1>

            <p>
              Manage attendance, leave, payroll, recruitment and employee
              workflows from one modern HR platform built for employees,
              managers and administrators.
            </p>

            <div className="nx-login-feature-grid">
              <article>
                <span className="nx-feature-icon">01</span>
                <div>
                  <strong>Attendance & Leave</strong>
                  <small>Real-time workforce activity and approval flows.</small>
                </div>
              </article>

              <article>
                <span className="nx-feature-icon">02</span>
                <div>
                  <strong>Hiring Pipeline</strong>
                  <small>Shortlisting, interviews and final selection.</small>
                </div>
              </article>

              <article>
                <span className="nx-feature-icon">03</span>
                <div>
                  <strong>Employee Workspace</strong>
                  <small>Self-service HR information in one dashboard.</small>
                </div>
              </article>
            </div>
          </div>

          <div className="nx-login-brand-footer">
            <span>Protected access</span>
            <span>•</span>
            <span>Role-based security</span>
            <span>•</span>
            <span>Audit ready</span>
          </div>
        </aside>

        <section className="nx-login-form-panel">
          <div className="nx-login-form-wrap">
            <div className="nx-login-mobile-brand">
              <div className="nx-login-logo">N</div>
              <div>
                <strong>NexusHR</strong>
                <span>Enterprise Workforce Intelligence</span>
              </div>
            </div>

            {!resetMode && (
              <>
                <header className="nx-login-heading-block">
                  <span className="nx-login-kicker">WELCOME BACK</span>
                  <h2>Sign in to your workspace</h2>
                  <p>
                    Use your username or registered employee email to continue.
                  </p>
                </header>

                <form className="nx-login-form" onSubmit={handleSubmit}>
                  <label className="nx-login-field">
                    <span>Username or email</span>
                    <div className="nx-login-input-wrap">
                      <span className="nx-login-input-icon" aria-hidden="true">@</span>
                      <input
                        value={username}
                        onChange={(event) => setUsername(event.target.value)}
                        placeholder="Enter username or email"
                        autoComplete="username"
                        autoFocus
                      />
                    </div>
                  </label>

                  <label className="nx-login-field">
                    <div className="nx-login-label-row">
                      <span>Password</span>
                      <button type="button" className="nx-login-link" onClick={openForgotPassword}>
                        Forgot password?
                      </button>
                    </div>

                    <div className="nx-login-input-wrap">
                      <span className="nx-login-input-icon" aria-hidden="true">●</span>
                      <input
                        value={password}
                        onChange={(event) => setPassword(event.target.value)}
                        type={showPassword ? "text" : "password"}
                        autoComplete="current-password"
                        placeholder="Enter your password"
                      />
                      <button
                        type="button"
                        className="nx-login-password-toggle"
                        onClick={() => setShowPassword(!showPassword)}
                      >
                        {showPassword ? "Hide" : "Show"}
                      </button>
                    </div>
                  </label>

                  <div className="nx-login-options">
                    <label className="nx-login-check">
                      <input
                        type="checkbox"
                        checked={remember}
                        onChange={(event) => setRemember(event.target.checked)}
                      />
                      <span>Remember username</span>
                    </label>
                    <span className="nx-login-encrypted">Encrypted session</span>
                  </div>

                  {error && <ErrorBox text={error} />}

                  <button className="nx-login-submit" disabled={loading} type="submit">
                    {loading ? (
                      <>
                        <span className="nx-login-spinner" />
                        Signing in...
                      </>
                    ) : (
                      <>
                        <span>Sign in securely</span>
                        <span className="nx-login-arrow">→</span>
                      </>
                    )}
                  </button>

                  <button
                    type="button"
                    className="nx-login-careers"
                    onClick={() => { window.location.href = "/careers"; }}
                  >
                    <span>
                      <strong>Looking for a career?</strong>
                      <small>Explore open roles and track your application</small>
                    </span>
                    <b>→</b>
                  </button>
                </form>

                <div className="nx-login-trust">
                  <div className="nx-login-trust-icon">✓</div>
                  <div>
                    <strong>Secure NexusHR authentication</strong>
                    <p>Your access is protected by role-based authorization and session controls.</p>
                  </div>
                </div>
              </>
            )}

            {resetMode && resetStep === "request" && (
              <>
                <header className="nx-login-heading-block">
                  <span className="nx-login-kicker">ACCOUNT RECOVERY</span>
                  <h2>Reset your password</h2>
                  <p>Enter your registered employee email or username. We will send a verification code.</p>
                </header>

                <form className="nx-login-form" onSubmit={handleForgotPassword}>
                  <label className="nx-login-field">
                    <span>Username or registered email</span>
                    <div className="nx-login-input-wrap">
                      <span className="nx-login-input-icon">@</span>
                      <input
                        value={resetIdentity}
                        onChange={(event) => setResetIdentity(event.target.value)}
                        placeholder="employee@email.com"
                        autoComplete="username"
                      />
                    </div>
                  </label>

                  {error && <ErrorBox text={error} />}
                  {resetMessage && <SuccessBox text={resetMessage} />}

                  <button className="nx-login-submit" disabled={loading} type="submit">
                    {loading ? (
                      <><span className="nx-login-spinner" /> Sending code...</>
                    ) : (
                      <><span>Send verification code</span><span className="nx-login-arrow">→</span></>
                    )}
                  </button>

                  <button type="button" className="nx-login-secondary" onClick={backToLogin}>
                    ← Back to sign in
                  </button>
                </form>
              </>
            )}

            {resetMode && resetStep === "reset" && (
              <>
                <header className="nx-login-heading-block">
                  <span className="nx-login-kicker">SECURE ACCOUNT SETUP</span>
                  <h2>Complete your setup</h2>
                  <p>Enter the 6-digit code and create a strong new password.</p>
                </header>

                <form className="nx-login-form nx-login-reset-form" onSubmit={handleResetPassword}>
                  <label className="nx-login-field">
                    <span>Username or registered email</span>
                    <div className="nx-login-input-wrap">
                      <span className="nx-login-input-icon">@</span>
                      <input
                        value={resetUsername}
                        onChange={(event) => setResetUsername(event.target.value)}
                        autoComplete="username"
                      />
                    </div>
                  </label>

                  <label className="nx-login-field">
                    <span>Choose username <small>First-time employee only</small></span>
                    <div className="nx-login-input-wrap">
                      <span className="nx-login-input-icon">U</span>
                      <input
                        value={desiredUsername}
                        onChange={(event) => setDesiredUsername(event.target.value)}
                        placeholder="Choose your username"
                        autoComplete="username"
                      />
                    </div>
                  </label>

                  <label className="nx-login-field">
                    <span>6-digit verification code</span>
                    <div className="nx-login-input-wrap">
                      <span className="nx-login-input-icon">#</span>
                      <input
                        className="nx-login-otp-input"
                        value={resetCode}
                        onChange={(event) => setResetCode(event.target.value.replace(/\D/g, "").slice(0, 6))}
                        placeholder="123456"
                        inputMode="numeric"
                        autoComplete="one-time-code"
                      />
                    </div>
                  </label>

                  <label className="nx-login-field">
                    <span>New password</span>
                    <div className="nx-login-input-wrap">
                      <span className="nx-login-input-icon">●</span>
                      <input
                        type={showNewPassword ? "text" : "password"}
                        value={newPassword}
                        onChange={(event) => setNewPassword(event.target.value)}
                        autoComplete="new-password"
                        placeholder="Minimum 8 characters"
                      />
                      <button
                        type="button"
                        className="nx-login-password-toggle"
                        onClick={() => setShowNewPassword(!showNewPassword)}
                      >
                        {showNewPassword ? "Hide" : "Show"}
                      </button>
                    </div>
                  </label>

                  <label className="nx-login-field">
                    <span>Confirm password</span>
                    <div className="nx-login-input-wrap">
                      <span className="nx-login-input-icon">✓</span>
                      <input
                        type="password"
                        value={confirmPassword}
                        onChange={(event) => setConfirmPassword(event.target.value)}
                        autoComplete="new-password"
                        placeholder="Confirm new password"
                      />
                    </div>
                  </label>

                  <div className="nx-password-rules">
                    <span className={newPassword.length >= 8 ? "ok" : ""}>8+ characters</span>
                    <span className={/[A-Z]/.test(newPassword) ? "ok" : ""}>Uppercase</span>
                    <span className={/[a-z]/.test(newPassword) ? "ok" : ""}>Lowercase</span>
                    <span className={/\d/.test(newPassword) ? "ok" : ""}>Number</span>
                  </div>

                  {error && <ErrorBox text={error} />}
                  {resetMessage && <SuccessBox text={resetMessage} />}

                  <button className="nx-login-submit" disabled={loading} type="submit">
                    {loading ? (
                      <><span className="nx-login-spinner" /> Saving...</>
                    ) : (
                      <><span>Set new password</span><span className="nx-login-arrow">→</span></>
                    )}
                  </button>

                  <button
                    type="button"
                    className="nx-login-secondary"
                    onClick={() => {
                      setResetStep("request");
                      setResetCode("");
                      setDesiredUsername("");
                      setNewPassword("");
                      setConfirmPassword("");
                      setError("");
                      setResetMessage("");
                    }}
                  >
                    Request another code
                  </button>
                </form>
              </>
            )}

            <footer className="nx-login-form-footer">
              <span>© {new Date().getFullYear()} NexusHR</span>
              <span>Enterprise HR & Workforce Intelligence Platform · Build V16 Auth + Dashboard Fix</span>
            </footer>
          </div>
        </section>
      </section>
    </main>
  );
}

function ErrorBox({ text }) {
  return (
    <div className="nx-login-alert nx-login-alert-error" role="alert">
      {text}
    </div>
  );
}

function SuccessBox({ text }) {
  return (
    <div className="nx-login-alert nx-login-alert-success" role="status">
      {text}
    </div>
  );
}

export default Login;