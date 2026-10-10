# JaCoCo Code Coverage Guide

## Overview
JaCoCo (Java Code Coverage) is now integrated into the CI/CD pipeline for both `auth-service` and `notification-service`.

## Configuration

### Coverage Thresholds
- **Minimum Required:** 70% instruction coverage
- Build will **fail** if coverage drops below 70%

### What's Covered
JaCoCo tracks:
- **Instructions** (bytecode instructions executed)
- **Branches** (if/else, switch statements)
- **Lines** (source code lines)
- **Methods** (method execution)
- **Classes** (class coverage)

## Local Development

### Generate Coverage Report
```bash
# For auth-service
cd auth-service
mvn clean test

# For notification-service
cd notification-service
mvn clean test

# View HTML report
# auth-service: auth-service/target/site/jacoco/index.html
# notification-service: notification-service/target/site/jacoco/index.html
```

### Check Coverage Thresholds
```bash
# Run verify to check if coverage meets minimum
mvn clean verify

# If coverage is below 70%, build will fail with:
# [ERROR] Rule violated for bundle: instruction covered ratio is 0.65, but expected minimum is 0.70
```

### Skip Coverage Check (Not Recommended)
```bash
# Only for local testing
mvn clean test -Djacoco.skip=true
```

## CI/CD Integration

### What Happens in GitHub Actions

1. **Test Execution**
   - JaCoCo agent instruments code during test execution
   - Collects coverage data

2. **Report Generation**
   - HTML reports generated in `target/site/jacoco/`
   - XML reports for automated tools

3. **Coverage Check**
   - Build fails if coverage < 70%
   - Prevents merging code with insufficient tests

4. **Artifacts Upload**
   - Coverage reports uploaded as GitHub artifacts
   - Available for 30 days after workflow run

5. **PR Comments** (Pull Requests only)
   - Automatic comment with coverage details
   - Shows coverage changes for modified files
   - Highlights which files need more tests

### Viewing Coverage in CI/CD

#### Via GitHub Actions Summary
- Go to: Actions → Select workflow run
- Check "Summary" section for coverage percentage

#### Via Artifacts
- Go to: Actions → Select workflow run → Artifacts section
- Download: `jacoco-report-auth-service` or `jacoco-report-notification-service`
- Unzip and open `index.html` in browser

#### Via PR Comments (Pull Requests)
- Coverage report automatically posted as PR comment
- Shows:
  - Overall coverage %
  - Coverage for changed files
  - Coverage trend (increased/decreased)

## Improving Coverage

### Find Uncovered Code
1. Run tests locally: `mvn clean test`
2. Open HTML report: `target/site/jacoco/index.html`
3. Navigate to specific class
4. **Red lines** = not covered
5. **Yellow lines** = partially covered (some branches)
6. **Green lines** = fully covered

### Coverage Best Practices

✅ **DO:**
- Write tests for business logic
- Test edge cases and error paths
- Cover all public methods
- Test exception handling

❌ **DON'T:**
- Write tests just to increase coverage
- Test trivial getters/setters (unless they have logic)
- Skip integration tests
- Ignore branch coverage

### Example: Improving Coverage

**Before (60% coverage):**
```java
public class UserService {
    public User findUser(Long id) {
        return repository.findById(id)
            .orElseThrow(() -> new NotFoundException("User not found"));
    }
}

// Test only happy path
@Test
void findUser_Success() {
    when(repository.findById(1L)).thenReturn(Optional.of(user));
    User result = userService.findUser(1L);
    assertNotNull(result);
}
```

**After (100% coverage):**
```java
// Add test for exception path
@Test
void findUser_NotFound_ThrowsException() {
    when(repository.findById(1L)).thenReturn(Optional.empty());
    assertThrows(NotFoundException.class, () -> userService.findUser(1L));
}
```

## Excluding Code from Coverage

### When to Exclude
- Generated code (Lombok, MapStruct)
- Configuration classes
- DTOs without logic
- Main application class

### How to Exclude

#### Option 1: Annotation (Preferred)
```java
@lombok.Generated  // Lombok classes are automatically excluded
public class UserDTO {
    // ...
}

@javax.annotation.Generated("ExcludeFromCoverage")
public class ConfigClass {
    // ...
}
```

#### Option 2: JaCoCo Configuration (pom.xml)
```xml
<execution>
    <id>report</id>
    <goals>
        <goal>report</goal>
    </goals>
    <configuration>
        <excludes>
            <exclude>**/config/**</exclude>
            <exclude>**/dto/**</exclude>
            <exclude>**/*Application.class</exclude>
        </excludes>
    </configuration>
</execution>
```

## Troubleshooting

### Build Fails with Coverage Error
```
[ERROR] Rule violated for bundle: instruction covered ratio is 0.65, but expected minimum is 0.70
```
**Solution:** Add more tests to increase coverage above 70%

### Coverage Report Not Generated
```bash
# Ensure tests are running
mvn clean test

# Check if JaCoCo agent ran
ls -la target/jacoco.exec

# If file exists but no report, regenerate
mvn jacoco:report
```

### Coverage Lower Than Expected
- Check if tests are actually running: `mvn test`
- Verify test assertions are meaningful
- Ensure integration tests are included
- Check for excluded packages

## Adjusting Coverage Threshold

If you need to change the minimum coverage:

### Edit pom.xml
```xml
<execution>
    <id>jacoco-check</id>
    <goals>
        <goal>check</goal>
    </goals>
    <configuration>
        <rules>
            <rule>
                <element>BUNDLE</element>
                <limits>
                    <limit>
                        <counter>INSTRUCTION</counter>
                        <value>COVEREDRATIO</value>
                        <minimum>0.80</minimum>  <!-- Change to 80% -->
                    </limit>
                </limits>
            </rule>
        </rules>
    </configuration>
</execution>
```

### Recommended Thresholds
- **Startup/Learning:** 60%
- **Production-Ready:** 70-80%
- **Mission-Critical:** 80-90%
- **Safety-Critical:** 90%+

## Integration with SonarQube

JaCoCo reports are compatible with SonarQube. The CI pipeline already integrates with SonarCloud:

```yaml
# In .github/workflows/ecr-push.yml
- name: Build and SonarCloud analysis
  run: |
    mvn -B verify org.sonarsource.scanner.maven:sonar-maven-plugin:sonar
```

SonarCloud automatically picks up JaCoCo reports from `target/site/jacoco/jacoco.xml`.

## Resources

- [JaCoCo Documentation](https://www.jacoco.org/jacoco/trunk/doc/)
- [Maven JaCoCo Plugin](https://www.jacoco.org/jacoco/trunk/doc/maven.html)
- [Code Coverage Best Practices](https://martinfowler.com/bliki/TestCoverage.html)

---

**Questions?** Check the [JaCoCo FAQ](https://www.jacoco.org/jacoco/trunk/doc/faq.html) or ask the team!
