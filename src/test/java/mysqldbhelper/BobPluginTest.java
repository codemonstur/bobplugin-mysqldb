package mysqldbhelper;

import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

import static mysqldbhelper.BobPlugin.databaseUrl;
import static mysqldbhelper.BobPlugin.host;
import static mysqldbhelper.BobPlugin.password;
import static mysqldbhelper.BobPlugin.port;
import static mysqldbhelper.BobPlugin.serverUrl;
import static mysqldbhelper.BobPlugin.user;
import static org.junit.Assert.assertEquals;

public class BobPluginTest {

    @Test
    public void defaultsMatchThePreviousHardcodedConnection() {
        final Map<String, String> env = new HashMap<>();

        assertEquals("localhost", host(env));
        assertEquals("3306", port(env));
        assertEquals("jdbc:mariadb://localhost:3306/", serverUrl(env));
        assertEquals("root", user(env));
        assertEquals("", password(env));
    }

    @Test
    public void serverUrlIsBuiltFromHostAndPort() {
        final Map<String, String> env = Map.of(
            "LOCALDB_HOST", "db.internal",
            "LOCALDB_PORT", "3307"
        );

        assertEquals("jdbc:mariadb://db.internal:3307/", serverUrl(env));
    }

    @Test
    public void databaseUrlAppendsTheDatabaseName() {
        final Map<String, String> env = Map.of("LOCALDB_HOST", "db.internal");

        assertEquals("jdbc:mariadb://db.internal:3306/myproject", databaseUrl(env, "myproject"));
    }

    @Test
    public void credentialsCanBeOverridden() {
        final Map<String, String> env = Map.of(
            "LOCALDB_USER", "migrator",
            "LOCALDB_PASSWORD", "s3cret"
        );

        assertEquals("migrator", user(env));
        assertEquals("s3cret", password(env));
    }

    @Test
    public void anExplicitEmptyPasswordIsPreserved() {
        final Map<String, String> env = Map.of("LOCALDB_PASSWORD", "");

        assertEquals("", password(env));
    }

    @Test
    public void blankValuesFallBackToDefaults() {
        final Map<String, String> env = Map.of(
            "LOCALDB_HOST", "",
            "LOCALDB_PORT", "",
            "LOCALDB_USER", ""
        );

        assertEquals("localhost", host(env));
        assertEquals("3306", port(env));
        assertEquals("root", user(env));
    }

}
