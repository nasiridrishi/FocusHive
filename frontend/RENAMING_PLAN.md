# FocusHive Frontend Renaming Plan

## Directory Renaming Required

The following directories need to be renamed to follow kebab-case convention:

### Shared Components
- `/shared/components/ErrorBoundary/` → `/shared/components/error-boundary/`
- `/shared/components/DynamicIcon/` → `/shared/components/dynamic-icon/`
- `/shared/components/LazyCharts/` → `/shared/components/lazy-charts/`
- `/shared/components/LazyDatePickers/` → `/shared/components/lazy-date-pickers/`
- `/shared/components/Loading/` → `/shared/components/loading/`

### Music Feature Components
- `/features/music/components/MusicPlayer/` → `/features/music/components/music-player/`
- `/features/music/components/SpotifyConnect/` → `/features/music/components/spotify-connect/`
- `/features/music/components/CollaborativeQueue/` → `/features/music/components/collaborative-queue/`
- `/features/music/components/MoodSelector/` → `/features/music/components/mood-selector/`
- `/features/music/components/PlaylistSelector/` → `/features/music/components/playlist-selector/`

## Files That Need Import Updates

After renaming directories, these files will need their imports updated:

### ErrorBoundary imports
- `src/shared/components/ErrorBoundary/index.ts`
- Any files importing from ErrorBoundary components

### DynamicIcon imports
- `src/shared/components/DynamicIcon/index.ts`
- Any files importing DynamicIcon

### LazyCharts imports
- `src/shared/components/LazyCharts/index.ts`
- Any files importing LazyCharts

### LazyDatePickers imports
- `src/shared/components/LazyDatePickers/index.ts`
- Any files importing LazyDatePickers

### Loading components imports
- `src/shared/components/Loading/index.ts`
- Any files importing Loading components

### Music Player imports
- `src/features/music/components/MusicPlayer/index.ts`
- `src/features/music/components/index.ts`
- Any files importing MusicPlayer

### Spotify Connect imports
- `src/features/music/components/SpotifyConnect/index.ts`
- `src/features/music/components/index.ts`
- Any files importing SpotifyConnect components

### Collaborative Queue imports
- `src/features/music/components/CollaborativeQueue/index.ts`
- `src/features/music/components/index.ts`
- Any files importing CollaborativeQueue

### Mood Selector imports
- `src/features/music/components/MoodSelector/index.ts`
- `src/features/music/components/index.ts`
- Any files importing MoodSelector

### Playlist Selector imports
- `src/features/music/components/PlaylistSelector/index.ts`
- `src/features/music/components/index.ts`
- Any files importing PlaylistSelector

## Constants That Need Updating

### PasswordStrengthIndicator.tsx
```typescript
// Current (camelCase)
const strengthColors = { ... }
const strengthLabels = { ... }

// Should be (UPPER_SNAKE_CASE)
const STRENGTH_COLORS = { ... }
const STRENGTH_LABELS = { ... }
```

## Execution Order

1. **Phase 1: Rename Directories**
   - Shared components first (fewer dependencies)
   - Music components second

2. **Phase 2: Update Imports**
   - Update index.ts files in renamed directories
   - Update all files importing from renamed directories

3. **Phase 3: Fix Constants**
   - Update constant naming in component files

4. **Phase 4: Verification**
   - Run TypeScript compiler to check for errors
   - Run tests to ensure functionality is preserved
   - Update any missed imports

## Risk Mitigation

- Create git branch before starting
- Rename one directory at a time
- Test after each directory rename
- Use TypeScript compiler to catch import errors
- Use IDE's "Find in Files" to locate all import references

## Tools to Use

- Git for version control and rollback capability
- TypeScript compiler for error detection
- VS Code "Find and Replace" for batch import updates
- ESLint for style compliance verification