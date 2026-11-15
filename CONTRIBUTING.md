# Contributing to page4j

Thank you for your interest in contributing to page4j! We welcome contributions from the community.

## How to Contribute

### Reporting Bugs

Before creating bug reports, please check existing issues to avoid duplicates. When creating a bug report, include:

- A clear and descriptive title
- Steps to reproduce the issue
- Expected vs actual behavior
- Java version, page4j version, and OS
- Code samples or test cases if applicable

### Suggesting Features

Feature requests are welcome! Please provide:

- A clear description of the feature
- Use cases and benefits
- Possible implementation approach (optional)

### Pull Requests

1. Fork the repository
2. Create a feature branch (`git checkout -b feature/amazing-feature`)
3. Make your changes
4. Run tests (`./gradlew test`)
5. Commit your changes (`git commit -m 'feat: add amazing feature'`)
6. Push to the branch (`git push origin feature/amazing-feature`)
7. Open a Pull Request

### Development Setup

```bash
# Clone the repository
git clone https://github.com/OCR4all/page4j.git
cd page4j

# Build the project
./gradlew build

# Run tests
./gradlew test
```

### Code Style

- Follow existing code conventions
- Run `./gradlew spotlessApply` before committing
- Ensure all tests pass
- Add tests for new features

### Commit Messages

Use [Conventional Commits](https://www.conventionalcommits.org): releases and the changelog are
calculated from them.

- `fix: ...` for a bug fix (patch release)
- `feat: ...` for a new feature (minor release)
- `feat!: ...` or a `BREAKING CHANGE:` footer for an incompatible change (major release)
- `docs:`, `test:`, `refactor:`, `chore:` and the like for everything else (no release)

## License

By contributing, you agree that your contributions will be licensed under the Apache License 2.0.
