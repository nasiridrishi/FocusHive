# FocusHive Git Hooks

These hooks ensure code quality and cannot be bypassed. They run automatically on every commit and push.

## Hooks Included

### pre-commit
Runs before every commit to check:
- ✅ No debugging statements (console.log, System.out.print)
- ✅ No merge conflict markers
- ✅ No hardcoded sensitive data
- ✅ TypeScript compilation (frontend)
- ✅ ESLint passes (frontend)
- ✅ All tests pass
- ✅ Java compilation (backend)
- ✅ No files larger than 5MB
- ✅ Commit message format

### pre-push
Runs before pushing to remote:
- ✅ All tests pass with coverage
- ✅ No uncommitted changes

### commit-msg
Validates commit messages:
- ✅ Follows conventional format: `type(scope): subject [UOL-XXX]`
- ✅ Auto-adds Linear task ID from branch name
- ✅ First line under 72 characters

## Setup

Run the setup script to enable hooks:
```bash
./setup-git-hooks.sh
```

## Commit Message Format

```
<type>(<scope>): <subject> [UOL-XXX]

<body>

<footer>
```

Types:
- **feat**: New feature
- **fix**: Bug fix
- **docs**: Documentation
- **style**: Formatting
- **refactor**: Code restructuring
- **test**: Tests
- **chore**: Maintenance
- **build**: Build changes
- **ci**: CI changes
- **perf**: Performance

## Cannot Be Bypassed

These hooks are configured to prevent:
- Using `--no-verify` or `-n` flags
- Skipping checks
- Direct git operations that bypass hooks

## If Checks Fail

1. Fix the identified issues
2. Stage your changes
3. Commit again

The hooks ensure consistent code quality across the project.