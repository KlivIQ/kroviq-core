<p align="center">
  <img src="src/main/resources/assets/kroviq_logo.png" alt="Kroviq" width="320">
</p>

<h1 align="center">Kroviq Core</h1>

<p align="center">
  <strong>Write Once. Run Across Technologies.</strong><br>
  Open-source multi-technology automation framework powering the Kroviq platform.
</p>

<p align="center">
  <a href="LICENSE"><img src="https://img.shields.io/badge/License-Apache_2.0-blue.svg" alt="License"></a>
  <img src="https://img.shields.io/badge/Java-17+-orange.svg" alt="Java 17+">
  <img src="https://img.shields.io/badge/Selenium-4.25-43B02A.svg" alt="Selenium">
  <img src="https://img.shields.io/badge/Cucumber-7.24-23D96C.svg" alt="Cucumber">
  <img src="https://img.shields.io/badge/Maven-3.8+-C71A36.svg" alt="Maven">
  <img src="https://img.shields.io/badge/Release-v2.5.9-brightgreen.svg" alt="Release">
</p>

<p align="center">
  <a href="docs/Kroviq_Product_Portal.html">Product Portal</a> ·
  <a href="docs/Kroviq_User_Guide.html">User Guide</a> ·
  <a href="docs/Kroviq_PreSales_Brochure.html">Brochure</a> ·
  <a href="docs/Kroviq_QuickStart_Guide.md">Quick Start</a>
</p>

---

## Overview

Kroviq Core is a reusable Java-based automation framework built using Selenium, Cucumber, and Maven.
It provides core utilities, wrappers, reporting, and execution infrastructure for UI test automation.

This repository contains **only framework code** (no test cases).

---

## Kroviq Platform

```
Kroviq
├── Kroviq Core         ← This repository (open-source framework)
├── Kroviq Studio       → Low-code test automation platform
└── Kroviq Enterprise   → Advanced commercial features
```

---

## Features

* Selenium WebDriver utilities
* Cucumber BDD support
* Dynamic test data handling (JSON + Excel)
* Extent + custom reporting
* Modular and scalable design
* Thread-safe execution (TestContext)
* Multi-framework UI wrappers (AntD, AG Grid, MUI, Angular Material, PrimeNG)
* **Kroviq Engine** — find-by-value, cross-page search, row actions, pagination, sort, filter, select, expand, edit
* **File Upload Handler** — Universal file upload automation (Hard Frozen)
* **Multi-Framework DatePicker** — Framework-agnostic date selection (Hard Frozen)
* **Multi-Framework MultiSelect** — Multi-value selection with chip management (Soft Frozen)
* **REST API Testing** — Native REST automation with assertions and schema validation (Soft Frozen)
* **Windows Desktop Support** — via WinAppDriver (Soft Frozen)
* **AI Capabilities** — Failure RCA Assistant + AI Defect Writer + AI Spark (BRD-to-Gherkin)

---

## Supported Technologies

| Technology | Engine |
|---|---|
| AG Grid | `AgGridTableEngine` |
| Ant Design | `AntDTableEngine` |
| PrimeNG | `PrimeNGTableEngine` |
| Angular Material | `AngularMaterialTableEngine` |
| MUI DataGrid | `MUITableEngine` |
| Generic HTML | `GenericTableEngine` |
| REST API | `ApiClient` |
| Windows Desktop | `WinAppDriverEngine` |

---

## Prerequisites

* Java 17+
* Maven 3.8+

---

## Quick Start

Get running in under 10 minutes:

```bash
# 1. Install framework
git clone https://github.com/kroviq/kroviq-core.git
cd kroviq-core
mvn clean install -DskipTests

# 2. Run the Quick Start demo
cd ..
git clone https://github.com/kroviq/kroviq-quickstart.git
cd kroviq-quickstart
mvn exec:java
```

See [Quick Start Guide](docs/Kroviq_QuickStart_Guide.md) for full details.

---

## Ecosystem

| Repository | Purpose |
|---|---|
| **kroviq-core** | Framework (this repo) — install first |
| [kroviq-quickstart](https://github.com/kroviq/kroviq-quickstart) | 10-minute Quick Start (WingIt Airlines) |
| [kroviq-testsuite-template](https://github.com/kroviq/kroviq-testsuite-template) | Full 54-scenario showcase |

---

## Build

```bash
mvn clean install
```

---

## Usage

Add dependency in your test project:

```xml
<dependency>
    <groupId>com.kroviq</groupId>
    <artifactId>kroviq-core</artifactId>
    <version>2.0.0</version>
</dependency>
```

---

## Documentation

| Document | Description |
|---|---|
| [Product Portal](docs/Kroviq_Product_Portal.html) | Complete product overview, architecture, and roadmap |
| [User Guide](docs/Kroviq_User_Guide.html) | Step-by-step usage documentation |
| [Pre-Sales Brochure](docs/Kroviq_PreSales_Brochure.html) | Enterprise positioning and differentiators |
| [Quick Start Guide](docs/Kroviq_QuickStart_Guide.md) | Get running in 10 minutes |
| [API Freeze](docs/API_FREEZE_v2.x.md) | Frozen interface contracts |
| [Design Principles](docs/KROVIQ_DESIGN_PRINCIPLES.md) | Architecture and design decisions |
| [Release Notes](docs/releases/) | Version history and changelogs |

---

## Project Structure

```
src/main/java/kroviq/
├── ai/                 # AI capabilities (RCA, Defect Writer, Spark)
├── api/                # REST API testing
├── constants/          # Framework-wide constants
├── hooks/              # Cucumber lifecycle hooks
├── model/              # Data models
├── parallel/           # Parallel execution
├── reporting/          # Report generators
├── utils/              # Core utilities
└── wrapper/            # Multi-framework UI wrappers
    ├── aggrid/         # AG Grid
    ├── angularmaterial/# Angular Material
    ├── antd/           # Ant Design
    ├── core/           # Interfaces (all frozen)
    ├── desktop/        # Windows Desktop
    ├── factory/        # Factory + Generic handlers
    ├── mui/            # Material UI
    └── primeng/        # PrimeNG
```

---

## Contributing

Please read [CONTRIBUTING.md](CONTRIBUTING.md) for details on our code of conduct and the process for submitting pull requests.

---

## Guidelines

* Do NOT add test cases here
* Do NOT add module-specific logic
* Keep code generic and reusable
* Maintain backward compatibility

---

## Author

**Kroviq by KlivIQ Technologies**

[www.kliviq.com](https://www.kliviq.com)

---

## License

Licensed under the [Apache License 2.0](LICENSE).

Copyright © 2026 KlivIQ Technologies.
