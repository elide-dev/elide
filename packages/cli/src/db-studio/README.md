# Database Studio

Web-based database management UI that runs on the Elide runtime. At the moment it only supports SQLite with Elide's `elide:sqlite` js bindings. We assume that an existing valid database file exists for the `elide db studio` command to find. If you need a database file to run this on, [Northwind](https://github.com/jpwhite3/northwind-SQLite3) and [Chinook](https://github.com/lerocha/chinook-database/releases) are some good prepopulated examples.

## Directory Structure

```
db-studio/
├── api/              # TypeScript REST API server
│   ├── ...
│   ├── index.ts      # Server entrypoint
│   └── elide.pkl
└── ui/               # React frontend application
    ├── src/
    └── dist/         # Vite production build
```

## Running Locally

### Using the Gradle Command

The best way to run this locally is through the CLI in JVM mode. Simply passing `db studio` as args.

**Note:** In order to see UI changes you've made, you'll need to run `pnpm build` in the `ui/` directory first.

```bash
# Build the UI
cd packages/cli/src/db-studio/ui
pnpm build

# Go back to the elide repo root
# Then run the db studio command
./gradlew :packages:cli:run --args="db studio"

# With a specific database
./gradlew :packages:cli:run --args="db studio path/to/database.db"
```

### Running api or ui independently

Database Studio consists of two separate projects that can be run independently:

- `api`: run directly by calling `elide run index.ts` within the directory.
- `ui`: using `pnpm dev` within the directory.
