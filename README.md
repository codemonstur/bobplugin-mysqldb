
[![GitHub Release](https://img.shields.io/github/release/codemonstur/bobplugin-mysqldb.svg)](https://github.com/codemonstur/bobplugin-mysqldb/releases)
[![MIT Licence](https://badges.frapsoft.com/os/mit/mit.svg?v=103)](https://opensource.org/licenses/mit-license.php)

# Bob mysql helper plugin

A plugin for [bob](https://github.com/codemonstur/bobthebuildtool) that provisions a local database
from your Flyway migration scripts. It adds a `localdb` command that creates the project's database
on a local MariaDB/MySQL server and then runs every pending migration against it, so a fresh
checkout can be brought to the current schema with a single command.

Migrations are handled by [Flyway](https://flywaydb.org/); database access uses the
[MariaDB JDBC driver](https://mariadb.com/kb/en/mariadb-connector-j/).

### Features

- Creates the project's database on the local server
- Discovers migration scripts in every configured resource directory
- Applies pending migrations through Flyway (versioned `V1__description.sql` files)
- Connection host, port, user and password are configurable through environment variables
- Idempotent at the migration layer — Flyway records applied scripts and will not repeat them
- No command-line options to remember — the whole flow is driven by your bobfile

### Requirements

- Java 17 or newer
- bob 5 or newer (the plugin calls `requireBobVersion("5")`)
- A MariaDB/MySQL server — `localhost:3306` by default, see
  [Connection settings](#connection-settings)
- A `name:` entry in your bobfile — this determines the database that gets created

### Installation

Add this line to the top of your bobfile:

```
plugin codemonstur/bobplugin-mysqldb:1
```

The `localdb` command is then available in all your bob tasks.

### Usage

The plugin reads the database name from the `name` field of your bobfile, and looks for migrations
in `<resource directory>/db/migration`. By default the resource directory is
`src/main/resources`, so migration scripts belong in:

```
src/main/resources/db/migration/
    V1__create_users.sql
    V2__add_email_index.sql
```

A minimal bobfile wiring it up:

```yaml
plugin codemonstur/bobplugin-mysqldb:1

name: myproject

packaging: bob-plugin

dependencies:
  - project: ../bobthebuildtool
    scope: provided
  - repository: org.flywaydb:flyway-core:13.10.3
  - repository: org.flywaydb:flyway-mysql:13.10.3
  - repository: org.mariadb.jdbc:mariadb-java-client:3.5.10
```

Then call `localdb` from any task:

```
setup
    localdb
    info Database is ready at localhost:3306/myproject
```

Running `bob setup` will:

1. Connect to `jdbc:mariadb://localhost:3306/` and execute `CREATE DATABASE myproject`
2. Collect every existing `<resource dir>/db/migration` directory
3. Run `Flyway.migrate()` against `jdbc:mariadb://localhost:3306/myproject`

#### Connection settings

The connection is read from environment variables, so credentials never need to live in your
bobfile:

| Variable           | Default     | Description                                   |
| ------------------ | ----------- | --------------------------------------------- |
| `LOCALDB_HOST`     | `localhost` | Server host                                   |
| `LOCALDB_PORT`     | `3306`      | Server port                                   |
| `LOCALDB_USER`     | `root`      | User for `CREATE DATABASE` and for Flyway     |
| `LOCALDB_PASSWORD` | *(empty)*   | Password for that user                        |

Unset or blank variables fall back to the defaults. Export them before running bob:

```
export LOCALDB_HOST=db.internal
export LOCALDB_PASSWORD='the-database-password'
bob setup
```

They can also be set per task, next to the invocation:

```
setup
    LOCALDB_USER = migrator
    LOCALDB_PASSWORD = 'the-database-password'
    localdb
```

### Behaviour

- **Database name** comes from `project.config.name` — the `name:` field in your bobfile. If it is
  unset, the SQL becomes `CREATE DATABASE null` and Flyway targets a database literally named
  `null`, so always set `name:`.
- **Connection settings** come from the `LOCALDB_HOST`, `LOCALDB_PORT`, `LOCALDB_USER` and
  `LOCALDB_PASSWORD` environment variables; unset or blank values fall back to `localhost`,
  `3306`, `root` and an empty password. Both the `CREATE DATABASE` connection and the Flyway
  connection use the same settings.
- **Resource directories** default to `src/main/resources`. Each one is probed for a `db/migration`
  subdirectory; only directories that exist are passed to Flyway.
- **The command takes no options** — any arguments passed to `localdb` are ignored.
- **Re-running against an existing database fails.** `CREATE DATABASE` is issued without
  `IF NOT EXISTS`, so MariaDB returns "database exists", the `SQLException` propagates, and the
  command exits before Flyway runs. Drop the database first (or create it manually and comment out
  that step) to re-apply migrations. Once Flyway does run, its schema history table keeps it from
  repeating scripts already applied.

### Building

The project builds with [Maven](https://maven.apache.org/). From the project root:

```
mvn package
```

Or use the `Makefile`:

```
make help       # list all targets
make build      # clean and build
make test       # run the unit tests
make coverage   # run tests and generate a JaCoCo coverage report
make validate   # check dependency versions, cves, licenses, and code quality
```

Building from source expects a checkout of
[bobthebuildtool](https://github.com/codemonstur/bobthebuildtool) as a sibling directory; it is used
as a `provided` dependency for the bob plugin integration.

### License

MIT — see [LICENSE](LICENSE).
