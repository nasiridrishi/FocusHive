# FocusHive Documentation Cleanup Plan

## Executive Summary
**Total .md files found**: 396
**Action Required**: Consolidate to ~15 essential documents
**Target**: Support final report creation with minimal, accurate documentation

## Current State Analysis

### File Distribution
- `.claude/` folder: 107 files (agent/command docs - NOT needed for report)
- `bin/` folders: ~100 duplicate files (build artifacts - TO DELETE)
- Service docs: ~80 files (many duplicates and outdated)
- Root docs: 21 files (needs consolidation)
- `docs/` folder: 18 files (partially outdated)
- Draft report: 15 files (needs update with actual implementation)

### Active Services (4)
1. **focushive-backend** - Core monolith with chat/analytics/forum modules
2. **identity-service** - OAuth2 authentication & personas
3. **notification-service** - Multi-channel notifications
4. **buddy-service** - Accountability partners (NO README!)

### Shelved Services
- **music-service** - Spotify integration (deferred to Phase 2)

## Documentation Categories

### 1. KEEP & UPDATE (Essential for Final Report)
```
/PROJECT_INDEX.md - Update with current architecture
/README.md - Simplify as main entry point
/API_REFERENCE.md - Update with actual endpoints
/CLAUDE.md - Keep for development reference
```

### 2. CONSOLIDATE (Merge into Single Docs)
**Testing Documentation** → `/docs/TESTING_GUIDE.md`
- E2E_TEST_RESULTS.md
- E2E_TESTING_GUIDE.md
- E2E-TESTING.md
- TDD_SETUP_SUMMARY.md
- TEST_REALITY_CHECK.md
- TESTS_FIX_PLAN.md
- TESTS.md

**Security Documentation** → `/docs/SECURITY_REPORT.md`
- docs/SECURITY_AUDIT_REPORT.md
- docs/SECURITY_FIXES_COMPLETED.md
- docs/SECURITY_VALIDATION_COMPLETE.md
- docs/SECURITY_VERIFICATION_REPORT.md
- docs/security-audit/*.md

**Architecture Documentation** → `/docs/ARCHITECTURE.md`
- docs/architecture/*.md
- docs/project-structure.md
- database-index-migrations-summary.md

**Performance Documentation** → `/docs/PERFORMANCE.md`
- REDIS_CACHING_IMPLEMENTATION.md
- N1_QUERY_SETUP_COMPLETE.md
- docs/PERFORMANCE_*.md
- docs/database-performance-optimization-report.md

### 3. DELETE (Redundant/Outdated)

#### Delete All Files In:
- All `bin/` directories (build artifacts)
- `.claude/agents/` (not needed for report)
- `.claude/commands/` (tool documentation)
- `infrastructure/api-gateway/` (not used)
- `load-tests/` (if empty/outdated)

#### Specific Files to Delete:
- ProjectIdeaTemplates.md (initial planning)
- example_exam.md (not relevant)
- All duplicate test reports in services
- All CHANGELOG files (use git history)
- All TODO.md files (outdated)
- All WARP.md files
- error_categorization.md files
- Multiple deployment/production files per service

### 4. SERVICE DOCUMENTATION STANDARDIZATION

Each service should have ONLY:
```
services/{service-name}/
  ├── README.md (API docs, setup, architecture)
  └── DEPLOYMENT.md (Docker, config, monitoring)
```

Delete all other .md files in service directories.

## Action Plan

### Phase 1: Clean Redundant Files (5 mins)
```bash
# Delete all bin directories
find /Users/nasir/uol/focushive -type d -name "bin" -exec rm -rf {} + 2>/dev/null

# Delete .claude subdirectories
rm -rf /Users/nasir/uol/focushive/.claude/agents
rm -rf /Users/nasir/uol/focushive/.claude/commands

# Delete outdated root files
rm -f /Users/nasir/uol/focushive/ProjectIdeaTemplates.md
rm -f /Users/nasir/uol/focushive/example_exam.md
rm -f /Users/nasir/uol/focushive/e2e-test-execution.md
```

### Phase 2: Consolidate Documentation (20 mins)

1. **Create Master Test Documentation**
   - Combine all test docs into `/docs/TESTING_GUIDE.md`
   - Include: Setup, E2E tests, Coverage, Results

2. **Create Security Summary**
   - Combine security audits into `/docs/SECURITY_REPORT.md`
   - Focus on: Implemented features, OWASP compliance

3. **Update Architecture Docs**
   - Merge into `/docs/ARCHITECTURE.md`
   - Include: Current microservices, database design, APIs

4. **Performance Report**
   - Create `/docs/PERFORMANCE.md`
   - Include: Caching, optimization, benchmarks

### Phase 3: Service Documentation (15 mins)

For each service:
1. Create/Update README.md with:
   - Purpose & architecture
   - API endpoints
   - Database schema
   - Dependencies

2. Keep only essential deployment info

3. Delete all other .md files

### Phase 4: Update Main Docs (10 mins)

1. **PROJECT_INDEX.md**
   - Update service count (4 active, 1 shelved)
   - Fix architecture diagram
   - Update technology versions

2. **README.md**
   - Simplify to 2-3 pages
   - Quick start guide
   - Link to other docs

3. **API_REFERENCE.md**
   - Document actual implemented endpoints
   - Remove planned but unimplemented features

4. **CLAUDE.md**
   - Update with current agents
   - Remove outdated instructions

### Phase 5: Prepare Report Docs (15 mins)

Update `/docs/draft-report/` with:
1. Actual implementation details
2. Real test results
3. Performance metrics
4. Security measures implemented
5. Updated architecture diagrams

## Final Documentation Structure

```
focushive/
├── README.md                    # Project overview & quick start
├── PROJECT_INDEX.md            # Detailed navigation guide
├── API_REFERENCE.md            # Complete API documentation
├── CLAUDE.md                   # AI assistant instructions
├── docs/
│   ├── ARCHITECTURE.md         # System design & database
│   ├── TESTING_GUIDE.md        # All testing documentation
│   ├── SECURITY_REPORT.md      # Security implementation
│   ├── PERFORMANCE.md          # Optimization & metrics
│   ├── DEPLOYMENT.md           # Docker & production setup
│   └── draft-report/           # Academic report sections
├── services/
│   ├── buddy-service/
│   │   ├── README.md          # Service documentation
│   │   └── DEPLOYMENT.md      # Docker config
│   ├── focushive-backend/
│   │   ├── README.md
│   │   └── DEPLOYMENT.md
│   ├── identity-service/
│   │   ├── README.md
│   │   └── DEPLOYMENT.md
│   └── notification-service/
│       ├── README.md
│       └── DEPLOYMENT.md
└── .shelves/
    └── SHELVED.md             # Deferred features documentation
```

## Files to Clean (Non-.md)

### Potential Cleanup Candidates:
- Unused test fixtures
- Old migration files
- Duplicate configuration files
- Empty directories
- Temporary files
- IDE configuration files (.idea, .vscode if committed)
- Build artifacts not in .gitignore

## Success Metrics

After cleanup:
- ✅ Total .md files: ~15-20 (from 396)
- ✅ No duplicate documentation
- ✅ All docs reflect actual implementation
- ✅ Clear hierarchy for report writing
- ✅ Each service has standardized docs
- ✅ No outdated information

## Timeline

**Total Estimated Time**: 65 minutes
- Phase 1: 5 mins (delete redundant)
- Phase 2: 20 mins (consolidate docs)
- Phase 3: 15 mins (service docs)
- Phase 4: 10 mins (update main)
- Phase 5: 15 mins (report prep)

## Notes for Final Report

The consolidated documentation will support:
1. **Introduction**: Project overview from README
2. **Literature Review**: Keep separate (already done)
3. **Design**: From ARCHITECTURE.md
4. **Implementation**: From service READMEs + API_REFERENCE
5. **Testing**: From TESTING_GUIDE.md
6. **Evaluation**: From PERFORMANCE.md + test results
7. **Security**: From SECURITY_REPORT.md
8. **Conclusion**: Update based on actual achievements