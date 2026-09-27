# Java Unit Test Developer — Running Tests

_Part of the `java-unit-test-developer` skill — see `../SKILL.md` for the topic index. Load this file only when your current task touches this topic._

## 14. Running Tests

```bash
# All tests
cd JPPhotoManagerWeb/backend
mvn test

# Single test class
mvn test -Dtest=CatalogFolderServiceAdapterTest

# Single test method
mvn test -Dtest=CatalogFolderServiceAdapterTest#catalogFolder_newFolder_savesFolderToRepository

# All tests in a package
mvn test -Dtest="com.jpablodrexler.photomanager.infrastructure.service.*"

# Skip tests during build
mvn clean package -DskipTests

# Check line coverage against the 80% minimum (see code-reviewer skill §19.2)
mvn test
mvn jacoco:check

# Trending snapshot instead of a pass/fail gate — dated report under
# JPPhotoManagerWeb/docs/reports/code-coverage/ (see code-reviewer skill §19.4)
bash scripts/coverage-report.sh
```

---

