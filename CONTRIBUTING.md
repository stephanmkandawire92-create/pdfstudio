# Contributing to PDFStudio

Thank you for your interest in contributing to PDFStudio! This document provides guidelines and instructions for contributing.

## Code of Conduct

Please be respectful and constructive in all interactions with maintainers and fellow contributors.

## Getting Started

1. **Fork the repository** on GitHub
2. **Clone your fork** locally:
   ```bash
   git clone https://github.com/YOUR_USERNAME/pdfstudio.git
   cd pdfstudio
   ```
3. **Add upstream remote**:
   ```bash
   git remote add upstream https://github.com/stephanmkandawire92-create/pdfstudio.git
   ```
4. **Create a feature branch**:
   ```bash
   git checkout -b feature/your-feature-name
   ```

## Development Workflow

1. **Keep your fork updated**:
   ```bash
   git fetch upstream
   git rebase upstream/main
   ```

2. **Build and test locally**:
   ```bash
   ./gradlew build
   ./gradlew test
   ```

3. **Run the app**:
   ```bash
   ./gradlew installDebug
   ```

## Coding Standards

- **Kotlin Style**: Follow [Kotlin official style guide](https://kotlinlang.org/docs/coding-conventions.html)
- **Compose Best Practices**: Use [Compose guidelines](https://developer.android.com/develop/ui/compose/style-guide)
- **Architecture**: Maintain MVVM with Clean Architecture pattern
- **Naming**: Use clear, descriptive names for variables, functions, and classes
- **Comments**: Document complex logic and non-obvious implementations

## Commit Guidelines

- Write clear, descriptive commit messages
- Use imperative mood: "Add feature" not "Added feature"
- Reference issues when applicable: "Fix #123 - Description"
- Keep commits focused and atomic

Example:
```
git commit -m "Add PDF annotation tools - Fixes #45"
```

## Pull Request Process

1. **Push your changes** to your fork:
   ```bash
   git push origin feature/your-feature-name
   ```

2. **Create a Pull Request** on GitHub with:
   - Clear title and description
   - Reference to related issues (`Fixes #123`)
   - Screenshots/videos for UI changes
   - Test results

3. **PR Description Template**:
   ```markdown
   ## Description
   Brief description of changes

   ## Type of Change
   - [ ] Bug fix
   - [ ] New feature
   - [ ] Enhancement
   - [ ] Documentation

   ## Changes Made
   - Change 1
   - Change 2

   ## Testing
   - [ ] Unit tests added/updated
   - [ ] Manual testing completed
   - [ ] No breaking changes

   ## Screenshots (if applicable)
   [Add screenshots for UI changes]

   ## Checklist
   - [ ] Code follows style guidelines
   - [ ] Self-review completed
   - [ ] Comments added for complex logic
   - [ ] Documentation updated
   - [ ] No new warnings generated
   ```

4. **Address review feedback** promptly

5. **Squash commits** before merge if requested

## Types of Contributions

### Bug Reports
- Use clear title describing the bug
- Include steps to reproduce
- Provide actual vs expected behavior
- Add device/Android version info

### Feature Requests
- Explain the use case
- Describe desired functionality
- Include mockups/examples if helpful
- Consider performance implications

### Documentation
- Improve README or guides
- Fix typos and clarity issues
- Add code examples
- Update API documentation

### Code Improvements
- Optimize performance
- Refactor for maintainability
- Add tests
- Remove dead code

## Testing

All contributions should include:

- **Unit tests** for business logic
- **UI tests** for compose components
- **Integration tests** for features
- **Manual testing** confirmation

Run tests with:
```bash
./gradlew test                    # Unit tests
./gradlew connectedAndroidTest    # Instrumented tests
```

## Documentation

- Update README.md for new features
- Add code comments for complex logic
- Document API changes
- Update CONTRIBUTING.md if needed

## Questions?

- Check [GitHub Issues](https://github.com/stephanmkandawire92-create/pdfstudio/issues) for similar questions
- Open a discussion for general questions
- Ask in PR comments for clarification

## Recognition

Contributors will be recognized in:
- README.md contributors section
- Commit history
- Release notes

Thank you for contributing to PDFStudio! 🎉
