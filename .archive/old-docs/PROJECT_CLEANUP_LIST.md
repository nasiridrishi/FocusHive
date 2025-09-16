# FocusHive Project Cleanup List

## Summary
This document lists all files and directories that can be cleaned up to reduce project size and improve organization.
**DO NOT DELETE YET** - This is for review only.

## Cleanup Categories

### 1. Build Artifacts & Compiled Files

#### All `bin/` directories (Java compiled classes)
```bash
# Found in:
services/buddy-service/bin/
services/focushive-backend/bin/
services/identity-service/bin/
services/notification-service/bin/
infrastructure/api-gateway/bin/
.shelves/music-service/bin/

# Total size: ~50-100MB
# Safe to delete - regenerated on build
```

#### Build output directories
```bash
services/*/build/
services/*/target/
frontend/dist/
frontend/build/
```

### 2. Test & Coverage Reports
```bash
# Can be regenerated
services/*/coverage/
services/*/test-results/
frontend/coverage/
*.coverage
htmlcov/
.nyc_output/
```

### 3. Duplicate/Outdated Documentation

#### Root level files to delete:
```
/ProjectIdeaTemplates.md - Initial templates, not needed
/example_exam.md - Example only, not relevant
/e2e-test-execution.md - Duplicate of E2E_TESTING_GUIDE.md
/TASKS.md - 76KB outdated task list
```

#### Service-level duplicate docs:
```bash
# Each service has 15-20 .md files, keep only README.md
services/buddy-service/*.md (except README)
services/focushive-backend/*.md (except README, CLAUDE.md)
services/identity-service/*.md (except README, CLAUDE.md)
services/notification-service/*.md (except README)
```

### 4. IDE & Tool Configuration

#### Check if these are needed or should be in .gitignore:
```
.idea/ - IntelliJ IDEA (if exists)
.vscode/ - VS Code settings (if committed)
*.iml - IntelliJ module files
.project - Eclipse
.classpath - Eclipse
.settings/ - Eclipse
```

### 5. Temporary & Cache Files

#### Common temp files:
```bash
**/*.tmp
**/*.temp
**/*.cache
**/.DS_Store - macOS
**/Thumbs.db - Windows
**/*.swp - Vim swap files
**/*~ - Backup files
```

### 6. Log Files
```bash
**/*.log
**/logs/
services/*/log/
```

### 7. Unused/Empty Directories

#### Check and remove if empty:
```
infrastructure/api-gateway/ - Not actively used
load-tests/ - Check if contains useful tests
tests/integration-tests/ - Check if redundant
scripts/ - Check if scripts are current
```

### 8. Large/Redundant Test Data

#### Check test resources:
```bash
services/*/src/test/resources/large-*.json
services/*/src/test/resources/sample-*.xml
frontend/src/__fixtures__/
```

### 9. Old Database Files

#### Local database files (shouldn't be in repo):
```
*.db
*.sqlite
*.h2.db
derby.log
```

### 10. Package Manager Files

#### Lock files (keep one, remove duplicates):
```
# Check for multiple lock files
yarn.lock vs package-lock.json (keep only one)
```

#### Cache directories:
```
.pnp/
.yarn/cache/
node_modules/ (should be in .gitignore)
.gradle/ (should be in .gitignore)
.m2/ (if exists locally)
```

### 11. Docker Artifacts

#### Clean if not needed:
```
docker/unused-*.yml
*.dockerfile.backup
docker-compose.override.yml (if not used)
```

### 12. Agent Documentation (107 files)
```bash
# Not needed for final report
.claude/agents/ - All 48 agent definition files
.claude/commands/ - All 59 command files
.claude/docs/ - Keep if useful for development
```

### 13. Shelved Service
```bash
# Move to archive or document in SHELVED.md
.shelves/music-service/ - Keep minimal docs only
```

## Size Impact Estimation

| Category | Est. Files | Est. Size | Priority |
|----------|------------|-----------|----------|
| bin/ directories | ~1000 | 50-100MB | HIGH |
| node_modules | ~10000 | 200-500MB | HIGH |
| .gradle/.m2 | ~500 | 50-100MB | HIGH |
| Duplicate .md | ~350 | 5-10MB | MEDIUM |
| Test reports | ~100 | 10-20MB | MEDIUM |
| Log files | ~50 | 5-10MB | LOW |
| IDE config | ~20 | 1-2MB | LOW |

**Total potential cleanup**: 300-700MB + 10000+ files

## Safe Cleanup Commands

### Phase 1: Remove build artifacts (SAFE)
```bash
# Remove all bin directories
find . -type d -name "bin" -exec rm -rf {} + 2>/dev/null

# Remove build directories
find . -type d -name "build" -not -path "*/node_modules/*" -exec rm -rf {} + 2>/dev/null
find . -type d -name "target" -exec rm -rf {} + 2>/dev/null

# Remove coverage reports
find . -type d -name "coverage" -exec rm -rf {} + 2>/dev/null
find . -type d -name "test-results" -exec rm -rf {} + 2>/dev/null
```

### Phase 2: Clean temp files (SAFE)
```bash
# Remove OS-specific files
find . -name ".DS_Store" -delete
find . -name "Thumbs.db" -delete
find . -name "*.swp" -delete
find . -name "*~" -delete

# Remove log files
find . -name "*.log" -not -path "*/node_modules/*" -delete
```

### Phase 3: Documentation cleanup (REVIEW FIRST)
```bash
# List duplicate documentation
find . -name "*.md" -path "*/bin/*" -delete
find services -name "*.md" | grep -E "(TODO|CHANGELOG|WARP|error_categorization)"

# Remove agent docs (if not needed)
rm -rf .claude/agents .claude/commands
```

## Git Cleanup

After file cleanup, optimize git repository:
```bash
# Remove deleted files from git history (optional, aggressive)
git gc --aggressive --prune=now

# Check repository size
git count-objects -vH
```

## Verification Checklist

Before cleanup:
- [ ] Backup important files
- [ ] Ensure .gitignore is comprehensive
- [ ] Verify build process works
- [ ] Check CI/CD won't break

After cleanup:
- [ ] Run full build: `./gradlew clean build`
- [ ] Run tests: `./gradlew test`
- [ ] Check Docker: `docker-compose build`
- [ ] Verify frontend: `npm run build`

## DO NOT DELETE

Critical files to preserve:
- All source code (src/ directories)
- Configuration files (application.properties, package.json)
- Docker configurations in use
- .git directory
- README files at root level
- CLAUDE.md files
- Database migrations
- Active test files

## Next Steps

1. Review this list with team
2. Update .gitignore with patterns
3. Run cleanup in phases
4. Commit after each phase
5. Test thoroughly after cleanup