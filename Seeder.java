import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;

/**
 * User seeder for user-management-service.
 *
 * Uses the /api/v1/auth/register endpoint so the service handles BCrypt hashing.
 *
 * Usage:
 *   java Seeder.java
 *
 * Requirements:
 *   - Java 17+
 *   - First admin must already exist in DB (inserted manually), OR
 *     the first user in the list must be a non-admin so it registers without a JWT.
 *
 * Flow:
 *   1. Login as the bootstrap admin to get a JWT
 *   2. Register all remaining users using that JWT
 */
public class Seeder {

    static final String BASE_URL      = env("SEED_BASE_URL",      "http://204.168.203.32:8085");
    static final String ADMIN_USERNAME = env("SEED_ADMIN_USER",    "admin");
    static final String ADMIN_PASSWORD = env("SEED_ADMIN_PASSWORD", null);   // required
    static final String USER_PASSWORD  = env("SEED_USER_PASSWORD",  null);   // required

    // ── Users to seed ─────────────────────────────────────────────────────────
    record User(String username, String email, String phone, String password, String role) {}

    static final List<User> USERS = List.of(
        new User("supervisor",         "sup@landslide.test",        "+51999444444", USER_PASSWORD, "admin"),
        new User("operador_shangtan",  "op.shangtan@landslide.test","+51999222222", USER_PASSWORD, "operator"),
        new User("operador_lingzhan",  "op.lingzhan@landslide.test","+51999333333", USER_PASSWORD, "operator"),
        new User("autoridad_regional", "autoridad@gob.test",        "+51999555555", USER_PASSWORD, "viewer")
    );

    // ── HTTP client ───────────────────────────────────────────────────────────
    static final HttpClient HTTP = HttpClient.newHttpClient();

    static String env(String key, String fallback) {
        String val = System.getenv(key);
        return (val != null && !val.isBlank()) ? val : fallback;
    }

    static void requireEnv(String... keys) {
        boolean missing = false;
        for (String k : keys) {
            if (System.getenv(k) == null || System.getenv(k).isBlank()) {
                System.out.println("💥 Missing required env var: " + k);
                missing = true;
            }
        }
        if (missing) System.exit(1);
    }

    public static void main(String[] args) throws Exception {
        requireEnv("SEED_ADMIN_PASSWORD", "SEED_USER_PASSWORD");
        System.out.println("🌱 Seeder starting — " + BASE_URL + "\n");

        // Step 1 — Login as bootstrap admin
        System.out.println("[1/2] Logging in as '" + ADMIN_USERNAME + "'...");
        String token = login(ADMIN_USERNAME, ADMIN_PASSWORD);
        if (token == null) {
            System.out.println("💥 Login failed. Make sure the admin user exists in the DB.");
            System.exit(1);
        }
        System.out.println("      ✅ JWT obtained\n");

        // Step 2 — Register users
        System.out.println("[2/2] Registering users...");
        int ok = 0, skipped = 0, failed = 0;
        for (User u : USERS) {
            int status = register(u, token);
            if (status == 201) {
                System.out.printf("  ✅ Created:  %s (%s)%n", u.username(), u.role());
                ok++;
            } else if (status == 409) {
                System.out.printf("  ⚠️  Skipped:  %s (already exists)%n", u.username());
                skipped++;
            } else {
                System.out.printf("  ❌ Failed:   %s (HTTP %d)%n", u.username(), status);
                failed++;
            }
        }

        System.out.printf("%n✅ Done — created: %d  skipped: %d  failed: %d%n", ok, skipped, failed);
    }

    static String login(String username, String password) throws Exception {
        String body = """
                {"username":"%s","password":"%s"}
                """.formatted(username, password);

        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + "/api/v1/auth/login"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build();

        HttpResponse<String> res = HTTP.send(req, HttpResponse.BodyHandlers.ofString());

        if (res.statusCode() == 200) {
            // Extract token from JSON without a dependency — simple parse
            String json = res.body();
            int start = json.indexOf("\"token\":\"") + 9;
            int end   = json.indexOf("\"", start);
            return json.substring(start, end);
        }

        System.out.println("      Login response (" + res.statusCode() + "): " + res.body());
        return null;
    }

    static int register(User u, String token) throws Exception {
        String body = """
                {"username":"%s","email":"%s","phone_e164":"%s","password":"%s","role":"%s"}
                """.formatted(u.username(), u.email(), u.phone(), u.password(), u.role());

        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + "/api/v1/auth/register"))
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + token)
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build();

        HttpResponse<String> res = HTTP.send(req, HttpResponse.BodyHandlers.ofString());
        return res.statusCode();
    }
}
