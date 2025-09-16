# Git & Workspace Creation Master Plan

## 🚨 CRITICAL: Do NOT proceed without following this plan!

### Current Git State
- Branch: `main`
- Changes: 2,070 files (1,395 deleted, 675 modified, 634 untracked)
- Risk: Mixing cleanup work with new development

## Step-by-Step Execution Plan

### Phase 1: Preserve Current Work
```bash
# 1.1 - Stash the workspace setup files we just created
git add workspaces/ setup-workspaces.sh
git stash push -m "workspace-setup-files" workspaces/ setup-workspaces.sh

# 1.2 - Create a cleanup branch for all the deletions
git checkout -b cleanup/remove-old-files
git add -A  # Add all deletions and modifications
git commit -m "cleanup: remove old test files and unused documentation"

# 1.3 - Push cleanup branch (optional, for backup)
git push origin cleanup/remove-old-files
```

### Phase 2: Create Clean Baseline
```bash
# 2.1 - Go back to main (without the deletions)
git checkout main
git reset --hard origin/main  # Reset to remote state

# 2.2 - Create a baseline branch for agents
git checkout -b baseline/agent-workspaces
git pull origin main  # Ensure we're up to date

# 2.3 - Apply ONLY the workspace setup files
git stash pop  # Retrieve our workspace setup files
git add workspaces/ setup-workspaces.sh
git commit -m "feat: add multi-agent workspace setup system"
```

### Phase 3: Prepare Frontend for Copying
```bash
# 3.1 - Ensure frontend is in clean state
cd frontend
git status  # Should show minimal changes

# 3.2 - Stash any local frontend changes
git stash push -m "frontend-local-changes"

# 3.3 - Create a reference point
git tag baseline-v1.0
```

### Phase 4: Execute Workspace Creation
```bash
# 4.1 - Run the setup script
cd /Users/nasir/uol/focushive
./setup-workspaces.sh

# 4.2 - Each workspace will get:
# - Copy of frontend at baseline-v1.0
# - Fresh git repo
# - Unique feature branch
```

### Phase 5: Configure Each Agent Workspace

#### Agent 1 - Core Infrastructure
```bash
cd workspaces/agent-1-core
git init
git remote add origin <your-github-url>
git checkout -b feature/core-infrastructure
git add -A
git commit -m "init: Agent 1 workspace - Core Infrastructure"
```

#### Agent 2 - Backend Features
```bash
cd workspaces/agent-2-backend
git init
git remote add origin <your-github-url>
git checkout -b feature/backend-services
git add -A
git commit -m "init: Agent 2 workspace - Backend Features"
```

#### Agent 3 - Frontend UI
```bash
cd workspaces/agent-3-ui
git init
git remote add origin <your-github-url>
git checkout -b feature/ui-components
git add -A
git commit -m "init: Agent 3 workspace - Frontend UI"
```

#### Agent 4 - Quality Assurance
```bash
cd workspaces/agent-4-qa
git init
git remote add origin <your-github-url>
git checkout -b feature/testing-qa
git add -A
git commit -m "init: Agent 4 workspace - Quality Assurance"
```

## Merge Strategy (After 40 Hours)

### Option A: Sequential Merge (Safest)
```
main
  ← feature/core-infrastructure (Agent 1)
    ← feature/backend-services (Agent 2)
      ← feature/ui-components (Agent 3)
        ← feature/testing-qa (Agent 4)
```

### Option B: Integration Branch (Recommended)
```
main
  → integration/sprint-1
    ← feature/core-infrastructure (Agent 1)
    ← feature/backend-services (Agent 2)
    ← feature/ui-components (Agent 3)
    ← feature/testing-qa (Agent 4)
  ← integration/sprint-1 (after testing)
```

### Option C: Direct to Main (Risky)
```
main
  ← feature/core-infrastructure (PR)
  ← feature/backend-services (PR)
  ← feature/ui-components (PR)
  ← feature/testing-qa (PR)
```

## File Conflict Prevention

### Automated Prevention
- Each workspace has `file_guard.py` that blocks wrong file edits
- Git hooks prevent commits to wrong files
- CI/CD can validate file ownership

### Manual Prevention
- Each agent works ONLY in their designated directories
- Use the integration test suite before merging
- Code review focuses on boundary violations

## Rollback Plan

If anything goes wrong:

```bash
# 1. Preserve any important work
git stash save "emergency-backup"

# 2. Reset to baseline
git checkout baseline/agent-workspaces

# 3. Re-run setup
./setup-workspaces.sh

# 4. Restore work if needed
git stash pop
```

## Monitoring Progress

```bash
# Check all agents' progress
for agent in agent-1-core agent-2-backend agent-3-ui agent-4-qa; do
  echo "=== $agent ==="
  cd workspaces/$agent
  git log --oneline -5
  git status -s | wc -l
  echo ""
done
```

## Critical Success Factors

1. ✅ Each agent starts from SAME baseline
2. ✅ No uncommitted changes in workspaces
3. ✅ File guards are active
4. ✅ Git branches are isolated
5. ✅ Services are running (ports 8080-8087)
6. ✅ Integration tests pass

## Emergency Contacts

- If merge conflicts: Use Option B (Integration Branch)
- If services down: `docker-compose up -d` in services/focushive-backend
- If file guards fail: Check Python 3 is installed
- If tests fail: Agent 4 should fix immediately

## Timeline

**Hour 0**: Execute this plan
**Hour 0-10**: Agent 1 builds foundation
**Hour 5**: Agents 2 & 3 start
**Hour 0-40**: Agent 4 continuous testing
**Hour 40**: Integration & merge

---

**DO NOT SKIP ANY STEPS!** Each step prevents future problems.