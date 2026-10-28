# Spigot Command Framework - TODO (Refactor & V2)

## 1. Setup & Environment
- [x] Initialize Empty Java Maven project (Java 8 / `pom.xml`)
- [x] Add `spigot-api:1.8.8-R0.1-SNAPSHOT` with `<scope>provided</scope>`
- [x] Set up package structure (`annotation`, `adapter`, `cooldown`, `help`, `internal`)

## 2. Annotations & Routing Overhaul
- [x] Update `@Command` to target both `TYPE` and `METHOD` (remove `@SubCommand`)
- [x] Implement path-based tree routing (e.g., parsing `"arena delete"` into root `"arena"` and child node `"delete"`)
- [x] Maintain safety defaults: `console = false`, permission checks, and in-memory cooldowns

## 3. Type Adapter & Parameter Resolution
- [x] Create `ArgumentAdapter<T>` interface (`parse` and `suggest`)
- [x] Implement default adapters (`String`, `Integer`, `Double`, `Boolean`, `Player`)
- [x] Implement method parameter reflection runner:
    - [x] Auto-inject `Player` / `CommandSender`
    - [x] Auto-parse trailing inputs against method parameters via adapters
    - [x] Send automated usage error on missing arguments or parse failure

## 4. Contextual Tab Completion
- [x] Auto-complete path tokens (subcommands) for `args[0]`
- [x] Route argument completion to the matching parameter's `ArgumentAdapter#suggest`

## 5. Paginated Help System
- [x] Create `@Help(command = "...", entriesPerPage = 6)` annotation
- [x] Create `HelpEntry` metadata container
- [x] Implement `HelpPaginator` (permission filtering, page calculation, color formatting)
- [x] Hook default `/<root> help [page]` dynamic route

## 6. Cooldown Subsystem (Existing)
- [x] Maintain in-memory timestamp cooldown cache (`Map<UUID, Map<String, Long>>`)
- [x] Maintain `PlayerQuitEvent` cleanup listener

## 7. Testing & Verification
- [x] Test registration with constructor-injected dependencies (`new ArenaCommands(arenaManager)`)
- [x] Verify `/arena delete <arena>` executes with resolved `Arena` object
- [x] Verify `/arena delete <tab>` suggests arena names from adapter
- [x] Verify `/arena help 1` and `/arena help 2` paginate correctly based on permissions
