package mysqldbhelper;

import bobthebuildtool.pojos.buildfile.Project;
import bobthebuildtool.pojos.error.VersionTooOld;
import org.flywaydb.core.Flyway;

import java.nio.file.Files;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Map;

import static bobthebuildtool.services.Functions.isNullOrEmpty;
import static bobthebuildtool.services.Update.requireBobVersion;

public enum BobPlugin {;

    public static void installPlugin(final Project project) throws VersionTooOld {
        requireBobVersion("5");
        project.addCommand("localdb", "Creates a localdb from the migration scripts", BobPlugin::createLocalDB);
    }

    private static int createLocalDB(final Project project, final Map<String, String> environment, final String[] args)
            throws SQLException {
        final String serverUrl = serverUrl(environment);
        final String user = user(environment);
        final String password = password(environment);

        try (final var conn = DriverManager.getConnection(serverUrl, user, password)) {
            conn.createStatement().executeUpdate("CREATE DATABASE " + project.config.name);
        }

        final String[] locations = project.getResourceDirectories()
            .stream()
            .map(path -> path.resolve("db/migration"))
            .filter(Files::isDirectory)
            .map(path -> "filesystem:" + path.toAbsolutePath())
            .toArray(String[]::new);

        Flyway.configure()
            .dataSource(databaseUrl(environment, project.config.name), user, password)
            .locations(locations)
            .load()
            .migrate();

        return 0;
    }

    static String serverUrl(final Map<String, String> environment) {
        return "jdbc:mariadb://" + host(environment) + ":" + port(environment) + "/";
    }

    static String databaseUrl(final Map<String, String> environment, final String databaseName) {
        return serverUrl(environment) + databaseName;
    }

    static String host(final Map<String, String> environment) {
        return env(environment, "LOCALDB_HOST", "localhost");
    }

    static String port(final Map<String, String> environment) {
        return env(environment, "LOCALDB_PORT", "3306");
    }

    static String user(final Map<String, String> environment) {
        return env(environment, "LOCALDB_USER", "root");
    }

    static String password(final Map<String, String> environment) {
        return env(environment, "LOCALDB_PASSWORD", "");
    }

    private static String env(final Map<String, String> environment, final String key, final String fallback) {
        final String value = environment.get(key);
        return isNullOrEmpty(value) ? fallback : value;
    }

}
