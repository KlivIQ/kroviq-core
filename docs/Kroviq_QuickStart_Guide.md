# Kroviq Core — Quick Start Guide

---

## Get Running in 10 Minutes

### Prerequisites

| Requirement | Verify With |
|---|---|
| Java 17+ | `java -version` |
| Maven 3.8+ | `mvn -version` |
| Chrome / Edge / Firefox | Installed and up to date |
| Git | `git --version` |

> Browser drivers are managed automatically by WebDriverManager — no manual ChromeDriver download needed.

---

## Option A: Quick Start (Recommended for First-Time Users)

The fastest way to see Kroviq in action:

```bash
# 1. Clone and install the framework (~3 min)
git clone https://github.com/kroviq/kroviq-core.git
cd kroviq-core
mvn clean install -DskipTests

# 2. Clone and run the Quick Start demo (~2 min)
cd ..
git clone https://github.com/kroviq/kroviq-quickstart.git
cd kroviq-quickstart
mvn exec:java
```

**What happens:** Chrome opens, 10 WingIt Airlines scenarios execute against local HTML fixtures, and a report is generated in `Reports/`.

---

## Option B: Full Demo (54 Scenarios)

For a comprehensive showcase of all Kroviq capabilities:

```bash
# 1. Clone and install the framework
git clone https://github.com/kroviq/kroviq-core.git
cd kroviq-core
mvn clean install -DskipTests

# 2. Clone and run the full template
cd ..
git clone https://github.com/kroviq/kroviq-testsuite-template.git
cd kroviq-testsuite-template
mvn exec:java
```

**Default suite:** `Demo_Web` (48 web scenarios across 6 UI frameworks). Edit `selectedSuite` in `RunManager.json` to switch suites.

---

## What Success Looks Like

### Console Output

```
[START] Starting Kroviq TestSuite Execution...
[OK] Applied tag filter: @QuickStart
...
10 Scenarios (10 passed)
30 Steps (30 passed)
[OK] Test execution completed
```

### Report

Open `Reports/Run_YYYYMMDD_HHMMSS/TestRun_*_Custom.html` in any browser.

---

## Supported Technologies (What Can Be Tested)

**Web UI Frameworks:**
- AG Grid, Ant Design, PrimeNG, Angular Material, MUI DataGrid, Generic HTML
- Any standard web application (Selenium-based)

**REST API:**
- Any REST/HTTP endpoint
- Supports REST-only, Hybrid (UI + API), and web modes

**Desktop:**
- Windows Desktop apps via WinAppDriver (requires separate setup)

---

## Key Features

- **Gherkin Test Scripts** — Write tests in plain English (Given/When/Then)
- **Dual Test Data Support** — JSON or Excel as test data source
- **Smart Locator Recovery** — Retries alternate locators when elements change
- **AI Failure RCA** — Automatic root-cause analysis on failure
- **AI Defect Writer** — Auto-generates defect drafts from failures
- **Kroviq Engine** — Universal table automation across 6 UI frameworks
- **Parallel Execution** — Run tests concurrently
- **Custom HTML + Excel Reporting** — Self-contained reports with screenshots

---

## Configuration

**RunManager.json (key fields):**

| Field | Purpose | Default |
|---|---|---|
| `projectName` | Your project name | `"WingIt Airlines"` |
| `browser` | chrome / firefox / edge | `"chrome"` |
| `selectedSuite` | Which suite to run | `"QuickStart"` |
| `testDataSource` | json or excel | `"json"` |
| `rcaEnabled` | AI root-cause analysis | `true` |
| `parallelExecution` | Run in parallel | `false` |

---

## Creating Your Own Test Project

1. Copy `kroviq-quickstart` → rename it
2. Update `pom.xml` (groupId, artifactId)
3. Update `RunManager.json` (projectName, selectedSuite)
4. Add your module:
   - `src/main/java/kroviq/constants/{Module}Constants.java` (locators)
   - `src/main/resources/testdata/json/{Module}.json` (test data)
   - `src/test/resources/features/{Module}/{Module}.feature` (scenarios)
5. Run — convention-based resolution handles everything

---

## Troubleshooting

| Problem | Solution |
|---|---|
| `Could not find artifact com.kroviq:kroviq-core:2.0.0` | Run `mvn clean install -DskipTests` in kroviq-core first |
| Chrome doesn't open | Ensure Chrome is installed and up to date |
| `java: invalid source release: 17` | Install Java 17+ and set `JAVA_HOME` |
| Tests pass but no report | Check `Reports/` folder after execution completes |
| Maven SSL errors | Add `-Dmaven.wagon.http.ssl.insecure=true -Dmaven.wagon.http.ssl.allowall=true` |
| 0 scenarios run | Check `selectedSuite` in RunManager.json matches a defined suite |

---

## Documentation

| Document | Description |
|---|---|
| [Product Portal](docs/Kroviq_Product_Portal.html) | Complete product overview |
| [User Guide](docs/Kroviq_User_Guide.html) | Step-by-step documentation |
| [API Freeze](docs/API_FREEZE_v2.x.md) | Frozen interface contracts |
| [Design Principles](docs/KROVIQ_DESIGN_PRINCIPLES.md) | Architecture decisions |

---

## Ecosystem

```
kroviq-core              ← Framework (this repo)
kroviq-quickstart        ← 10-minute Quick Start (WingIt Airlines)
kroviq-testsuite-template ← Full 54-scenario showcase
```

---

**Kroviq by KlivIQ Technologies** — [www.kliviq.com](https://www.kliviq.com)
