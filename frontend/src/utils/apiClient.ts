/**
 * Global API client with 401 handling
 * Intercepts all fetch requests and handles session expiration
 * 
 * When a 401 is received, redirects to trigger OAuth login flow.
 * Spring Security will redirect unauthenticated users to Cognito for login.
 * 
 * Works for all roles (learner, editor, admin):
 * - Redirects to home page (/) which is public
 * - Spring Security's saved request mechanism preserves the original destination
 * - After login, users are redirected back to their original destination
 * 
 * Security model:
 * - Learners: Can access /api/public/** (no auth) and /api/** (authenticated)
 * - Editors/Admins: Can access /api/admin/** (role-based) and /api/** (authenticated)
 */

let isRedirectingToLogin = false;

/**
 * Wrapper around fetch that handles 401 responses globally
 * Redirects to home page which triggers OAuth login flow via Spring Security
 * 
 * This works for all user roles:
 * - Learners: Redirect to / (home), after login return to their page
 * - Editors/Admins: Redirect to /, after login Spring Security redirects to /admin
 * 
 * Note: We redirect to / (home) instead of /admin because:
 * - Learners don't have access to /admin routes
 * - Spring Security's saved request mechanism preserves where they were
 * - After login, they're redirected back appropriately based on their role
 */
export async function apiFetch(
    input: RequestInfo | URL,
    init?: RequestInit
): Promise<Response> {
    const response = await fetch(input, {
        ...init,
        credentials: "include", // Always include credentials
    });

    // Handle 401 Unauthorized globally
    if (response.status === 401 && !isRedirectingToLogin) {
        isRedirectingToLogin = true;
        
        // Redirect to Spring Security's OAuth2 authorization endpoint
        // This triggers the Cognito login flow for all user roles (learner, editor, admin)
        // Spring Security will:
        // 1. Redirect to Cognito for authentication
        // 2. After successful login, redirect back to the original destination
        //    (or home page if the original request was to a public route)
        // 
        // Note: Using the OAuth2 authorization endpoint works for all roles:
        // - Learners: After login, return to their page (or home)
        // - Editors/Admins: After login, return to /admin (if that's where they were)
        window.location.href = "/oauth2/authorization/cognito";
        
        // Return a rejected promise to prevent further processing
        return Promise.reject(new Error("Session expired. Redirecting to login..."));
    }

    return response;
}

/**
 * Reset the redirecting flag (useful for testing or manual resets)
 */
export function resetRedirectFlag() {
    isRedirectingToLogin = false;
}
