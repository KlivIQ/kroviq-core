# Contributing to Kroviq Core

Thank you for your interest in contributing to Kroviq Core! This document provides guidelines for contributing to the project.

## Code of Conduct

Please read our [Code of Conduct](CODE_OF_CONDUCT.md) before contributing.

## How to Contribute

### Reporting Bugs

- Use [GitHub Issues](../../issues) to report bugs
- Include steps to reproduce, expected behavior, and actual behavior
- Include Java version, OS, and Maven version

### Suggesting Features

- Open a [GitHub Issue](../../issues) with the `enhancement` label
- Describe the use case and expected behavior
- Note: All v2.x interfaces are **frozen** — changes to existing contracts are not accepted

### Pull Requests

1. Fork the repository
2. Create a feature branch (`git checkout -b feature/your-feature`)
3. Make your changes
4. Ensure all tests pass (`mvn clean test`)
5. Commit with clear messages (`git commit -m "Add: description"`)
6. Push to your fork (`git push origin feature/your-feature`)
7. Open a Pull Request

## Development Setup

### Prerequisites

- Java 17+
- Maven 3.8+
- IDE with Lombok plugin

### Build

```bash
mvn clean install
```

### Run Tests

```bash
mvn clean test
```

## Guidelines

### Code Style

- 4-space indentation
- Opening brace on same line
- Minimal Javadoc — code should be self-documenting
- Follow existing patterns in the codebase

### Architecture Rules

- **No test cases** in this repository — only framework code
- **No module-specific logic** — keep everything generic
- **Backward compatibility** must be maintained
- **API Freeze**: Do not modify frozen interfaces (see `docs/API_FREEZE_v2.x.md`)

### What's Accepted

- New engine implementations (e.g., new UI framework support)
- New optional interfaces
- New default methods on existing interfaces
- Bug fixes
- Performance improvements
- Documentation improvements

### What's NOT Accepted

- Changes to frozen interface signatures
- Removal of existing methods
- Test cases (those belong in consumer repositories)
- Module-specific logic

## Questions?

Open a [Discussion](../../discussions) or reach out at [www.kliviq.com](https://www.kliviq.com).
