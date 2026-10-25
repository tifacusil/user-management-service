import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;

/**
 * Subscription seeder for user-management-service.
 *
 * Uses POST /api/v1/users/{user_id}/subscriptions (admin only).
 *
 * Flow:
 *   1. Login as admin → get JWT
 *   2. Login as each user → get their user_id from the login response
 *   3. POST each subscription using the admin JWT
 *
 * Usage:
 *   java SubscriptionSeeder.java
 */
public class SubscriptionSeeder {

    static final String BASE_URL      = env("SEED_BASE_URL",       "http://204.168.203.32:8085");
    static final String ADMIN_PASSWORD = env("SEED_ADMIN_PASSWORD", null);  // required
    static final String USER_PASSWORD  = env("SEED_USER_PASSWORD",  null);  // required

    // ── Subscription definition ───────────────────────────────────────────────
    record Subscription(
            String username,
            String password,
            String projectId,
            String minAlertLevel,
            boolean smsEnabled,
            boolean pushEnabled,
            boolean emailEnabled,
            String quietHoursStart,  // null if not set
            String quietHoursEnd     // null if not set
    ) {
        // Convenience constructor without quiet hours
        Subscription(String username, String password, String projectId,
                     String minAlertLevel, boolean sms, boolean push, boolean email) {
            this(username, password, projectId, minAlertLevel, sms, push, email, null, null);
        }
    }

    static final List<Subscription> SUBSCRIPTIONS = List.of(
        // admin — all alerts, both projects
        new Subscription("admin",              ADMIN_PASSWORD, "shangtan", "blue",   true,  true,  true),
        new Subscription("admin",              ADMIN_PASSWORD, "lingzhan", "blue",   true,  true,  true),
        // operador_shangtan — shangtan only, quiet hours 22:00–06:00
        new Subscription("operador_shangtan",  USER_PASSWORD,  "shangtan", "blue",   true,  true,  false, "22:00", "06:00"),
        // operador_lingzhan — lingzhan only
        new Subscription("operador_lingzhan",  USER_PASSWORD,  "lingzhan", "blue",   true,  true,  false),
        // supervisor — orange+ on both projects
        new Subscription("supervisor",         USER_PASSWORD,  "shangtan", "orange", true,  true,  false),
        new Subscription("supervisor",         USER_PASSWORD,  "lingzhan", "orange", true,  true,  false),
        // autoridad_regional — red only, sms + email (no push)
        new Subscription("autoridad_regional", USER_PASSWORD,  "shangtan", "red",    true,  false, true),
        new Subscription("autoridad_regional", USER_PASSWORD,  "lingzhan", "red",    true,  false, true)
    );

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
        System.out.println("🌱 Subscription seeder — " + BASE_URL + "\n");

        // Step 1 — Login as admin to get JWT
        System.out.println("[1/2] Logging in as admin...");
        String[] adminLogin = login("admin", ADMIN_PASSWORD);
        if (adminLogin == null) {
            System.out.println("💥 Admin login failed. Aborting.");
            System.exit(1);
        }
        String adminToken = adminLogin[0];
        System.out.println("      ✅ JWT obtained\n");

        // Step 2 — Create subscriptions
        System.out.println("[2/2] Creating subscriptions...");
        int ok = 0, skipped = 0, failed = 0;

        for (Subscription s : SUBSCRIPTIONS) {
            // Get user_id by logging in as that user
            String[] userLogin = login(s.username(), s.password());
            if (userLogin == null) {
                System.out.printf("  ❌ Could not get user_id for '%s' — skipping%n", s.username());
                failed++;
                continue;
            }
            String userId = userLogin[1];

            int status = createSubscription(userId, s, adminToken);
            String label = s.username() + " → " + s.projectId() + " (" + s.minAlertLevel() + ")";
            if (status == 201) {
                System.out.printf("  ✅ Created:  %s%n", label);
                ok++;
            } else if (status == 409) {
                System.out.printf("  ⚠️  Skipped:  %s (already exists)%n", label);
                skipped++;
            } else {
                System.out.printf("  ❌ Failed:   %s (HTTP %d)%n", label, status);
                failed++;
            }
        }

        System.out.printf("%n✅ Done — created: %d  skipped: %d  failed: %d%n", ok, skipped, failed);
    }

    /** Returns [token, userId] or null on failure. */
    static String[] login(String username, String password) throws Exception {
        String body = """
                {"username":"%s","password":"%s"}
                """.formatted(username, password);

        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + "/api/v1/auth/login"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build();

        HttpResponse<String> res = HTTP.send(req, HttpResponse.BodyHandlers.ofString());
        if (res.statusCode() != 200) return null;

        String json = res.body();
        String token  = extractJson(json, "token");
        String userId = extractJson(json, "user_id");
        return new String[]{token, userId};
    }

    static int createSubscription(String userId, Subscription s, String token) throws Exception {
        StringBuilder body = new StringBuilder();
        body.append("{");
        body.append("\"project_id\":\"").append(s.projectId()).append("\",");
        body.append("\"min_alert_level\":\"").append(s.minAlertLevel()).append("\",");
        body.append("\"sms_enabled\":").append(s.smsEnabled()).append(",");
        body.append("\"push_enabled\":").append(s.pushEnabled()).append(",");
        body.append("\"email_enabled\":").append(s.emailEnabled());
        if (s.quietHoursStart() != null) {
            body.append(",\"quiet_hours_start\":\"").append(s.quietHoursStart()).append("\"");
            body.append(",\"quiet_hours_end\":\"").append(s.quietHoursEnd()).append("\"");
        }
        body.append("}");

        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + "/api/v1/users/" + userId + "/subscriptions"))
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + token)
                .POST(HttpRequest.BodyPublishers.ofString(body.toString()))
                .build();

        HttpResponse<String> res = HTTP.send(req, HttpResponse.BodyHandlers.ofString());
        return res.statusCode();
    }

    /** Minimal JSON value extractor — no external dependencies. */
    static String extractJson(String json, String key) {
        String search = "\"" + key + "\":";
        int i = json.indexOf(search);
        if (i == -1) return null;
        i += search.length();
        if (json.charAt(i) == '"') {
            int end = json.indexOf('"', i + 1);
            return json.substring(i + 1, end);
        } else {
            int end = json.indexOf(',', i);
            if (end == -1) end = json.indexOf('}', i);
            return json.substring(i, end).trim();
        }
    }
}
