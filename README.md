# PharmaCare

PharmaCare is a Spring Boot web application for managing a small pharmacy workflow. It supports four operational roles: **Administrator**, **Doctor**, **Pharmacist**, and **Patient**. Each role sees only the pages and actions authorised for its work.

## Business goal

PharmaCare gives a pharmacy one controlled workspace for the medication journey: from an approved user account and prescription, through stock-aware dispensing, to patient visibility and management reporting. Its business purpose is to reduce fragmented manual work, prevent unauthorised access, and make the operational status of medicines and prescriptions visible to the right person at the right time.

## Design and architecture sources

- [Figma UI design](https://www.figma.com/design/Hn3t09dEGtunesiH5ytytn/Untitled?node-id=0-1&p=f&m=dev)
- [Draw.io architecture diagram](https://drive.google.com/file/d/1NkfRiLM-UqmF4XGflLnZ6zo3svQE-5zj/view?usp=sharing)

## What the application does

- Administrators create accounts, assign one role, approve registrations, and manage member status.
- Doctors create and update prescriptions.
- Pharmacists manage medicines, inventory, and dispensing.
- Patients view prescription status, notifications, and their profile.
- The system supplies branded login, password recovery, reset-password, access-denied, and error pages.

## Run locally

Requirements: Java 17 and macOS/Linux shell.

```bash
cd "/Users/susielim/Documents/Software design "

export JAVA_HOME="/opt/homebrew/opt/openjdk@17/libexec/openjdk.jdk/Contents/Home"
export PATH="$JAVA_HOME/bin:$PATH"

./gradlew bootRun
```

Open <http://localhost:8080/login>.

The first development administrator is created automatically when no matching account exists:

| Field | Default |
| --- | --- |
| Username | `admin` |
| Password | `Admin@123` |

Use environment variables `PHARMACARE_ADMIN_USERNAME`, `PHARMACARE_ADMIN_EMAIL`, and `PHARMACARE_ADMIN_PASSWORD` to override these before first start.

## Data and persistence

SQLite is used for security and registration data at `data/pharmacare.db`:

- accounts and member status;
- password credentials;
- roles, permissions, and role assignments.

This means registered users can sign in after an application restart. The database is local development data and is deliberately ignored by Git.

The database schema also includes profile, clinical, notification, medicine, inventory, dispensing, and reporting tables. After the application has started once (and therefore initialised the schema), a small, fictional, referentially consistent fixture set in `data/sample-data.sql` may be applied safely more than once:

```bash
sqlite3 data/pharmacare.db < data/sample-data.sql
```

The current application adapters for those latter domains remain in-memory, so their screens do not yet read the durable tables and still reset on restart. The database tables and fixtures provide the migration target; moving the adapters to SQLite is the remaining step to expose these records in the UI.

## Project structure

| Area | Purpose | Important entry point |
| --- | --- | --- |
| [`src/main/java/pharmacy_system/Main.java`](src/main/java/pharmacy_system/Main.java) | Application bootstrap, dependency configuration, seeded administrator | Start here to understand how the application is assembled. |
| [`controller/`](src/main/java/pharmacy_system/controller) | HTTP routes, authorisation checks, workflow orchestration | [`common/`](src/main/java/pharmacy_system/controller/common) contains session, navigation, security, and error handling. |
| [`model/`](src/main/java/pharmacy_system/model) | Business entities and lifecycle rules | Account, prescription, inventory, notification, dispensing, and report models. |
| [`storage/`](src/main/java/pharmacy_system/storage) | Persistence interfaces and implementations | [`security_user/`](src/main/java/pharmacy_system/storage/security_user) holds the SQLite account, credential, role, and permission storage. |
| [`view/`](src/main/java/pharmacy_system/view) | Presentation view models | Organised by use case and domain. |
| [`src/main/webapp/WEB-INF/jsp/`](src/main/webapp/WEB-INF/jsp) | JSP pages and shared fragments | [`fragments/`](src/main/webapp/WEB-INF/jsp/fragments) contains the shared application shell and UI components. |
| [`src/main/resources/static/`](src/main/resources/static) | CSS and browser assets | [`pharmacare.css`](src/main/resources/static/css/pharmacare.css) is the served application stylesheet. |
| [`.docs/`](.docs) | Design notes and technical guidance | [`compact structure.md`](.docs/compact%20structure.md) defines the intended architecture. |
| [`.kiro/specs/`](.kiro/specs) | Requirements, design, tasks, and dependency graph | [`pharmacy-ui-implementation/`](.kiro/specs/pharmacy-ui-implementation) contains the UI implementation specification. |

### Main application domains

- **Security and user management** — authentication, profiles, account approval, roles, and member status.
- **Clinical prescription** — prescription creation and status updates.
- **Patient information** — prescription visibility and notifications.
- **Pharmacy operations** — dispensing, medicines, stock, and inventory movements.
- **Management decision support** — operational reporting.

## Important implementation rules

- One user has one operational role.
- New accounts start as `PENDING`; an administrator must approve them before login.
- Account lifecycle is `PENDING → ACTIVE`, then `ACTIVE ↔ DISABLED`; locked accounts can be unlocked by an administrator.
- Protected routes enforce permissions in the controller layer.
- Status-changing updates use optimistic version checks to avoid silently overwriting another administrator’s change.
- Errors use the PharmaCare recovery page rather than Spring Boot’s Whitelabel page.

## Verify the project

```bash
./gradlew test
```

To rebuild from scratch after dependency changes:

```bash
./gradlew clean test
```
