# Security Policy

## Reporting a Vulnerability

**Do not** publicly report security vulnerabilities. Instead, please email security concerns to the repository maintainer through GitHub's private vulnerability reporting feature.

### Steps to Report:
1. Go to [Security Advisories](../../security/advisories) in the repository
2. Click "Report a vulnerability"
3. Provide detailed information about the vulnerability
4. Submit the report

## Supported Versions

| Version | Supported |
|---------|-----------|
| 1.x | ✅ Yes |
| < 1.0 | ❌ No |

## Security Measures

### Code Security
- Regular dependency updates and vulnerability scanning
- Firebase App Check for API protection
- Secure credential handling via environment variables
- Keystore management for release signatures

### Data Security
- Room database for local data encryption
- Firebase authentication for cloud access
- Network security configuration
- Certificate pinning (when applicable)

### Privacy
- Minimal data collection
- Secure storage of sensitive information
- Clear privacy practices
- GDPR compliance considerations

## Best Practices for Users

1. **Keep Updated**: Always use the latest version
2. **Secure Credentials**: Never share API keys or keystore files
3. **Use Strong Passwords**: For Firebase and authentication
4. **Report Issues**: Use responsible disclosure for vulnerabilities
5. **Review Permissions**: Understand app permissions before installing

## Security Dependencies

This project uses:
- **Firebase Security**: Google's security infrastructure
- **Android Security Libraries**: Latest androidx security components
- **Encryption**: Standard Android encryption mechanisms
- **Network Security**: TLS 1.2+ with certificate validation

## Vulnerability Response Process

1. **Report received** - Acknowledged within 48 hours
2. **Assessment** - Evaluated for severity and impact
3. **Fix development** - Security patch created
4. **Testing** - Thoroughly tested before release
5. **Release** - Published with security advisory
6. **Credit** - Reporter credited (if desired)

## Security Headers & Policies

- **Dependency Management**: Using Gradle dependency verification
- **Code Review**: All changes reviewed before merge
- **Testing**: Security-focused test coverage
- **Compliance**: Following Android security best practices

## Contact

For security inquiries, please use GitHub's private vulnerability reporting feature.

---

**Last Updated**: 2026-08-30
