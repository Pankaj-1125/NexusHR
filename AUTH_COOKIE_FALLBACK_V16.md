# NexusHR V16 Authentication Stabilization

- Login now sets an HttpOnly `nexushr_access_token` cookie in addition to returning the JWT in JSON.
- JWT authentication accepts either the standard `Authorization: Bearer` header or the HttpOnly cookie.
- Frontend API requests include same-origin credentials.
- Logout clears both local browser auth state and the server-issued access cookie.
- This protects the dashboard from the observed 403 failure when localStorage/session propagation is missing.
