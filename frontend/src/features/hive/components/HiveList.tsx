import React, {useMemo, useState} from 'react'
import {
  Alert,
  Box,
  Button,
  Chip,
  FormControl,
  InputAdornment,
  InputLabel,
  MenuItem,
  Paper,
  Select,
  Stack,
  TextField,
  ToggleButton,
  ToggleButtonGroup,
  Typography,
  useMediaQuery,
  useTheme,
} from '@mui/material'
import {
  Add as AddIcon,
  Refresh as RefreshIcon,
  Search as SearchIcon,
  ViewList as ViewListIcon,
  ViewModule as ViewModuleIcon,
} from '@mui/icons-material'
import {HiveCard} from './HiveCard'
import {CreateHiveForm} from './CreateHiveForm'
import {CreateHiveRequest, Hive, HiveMember} from '@shared/types'
import {ContentSkeleton} from '@shared/components/loading'

interface HiveListProps {
  hives: Hive[]
  members?: Record<string, HiveMember[]> // hiveId -> members
  currentUserId?: string
  isLoading?: boolean
  error?: string | null
  onJoin?: (hiveId: string, message?: string) => void
  onLeave?: (hiveId: string) => void
  onEnter?: (hiveId: string) => void
  onSettings?: (hiveId: string) => void
  onShare?: (hiveId: string) => void
  onRefresh?: () => void
  onCreateHive?: (hive: CreateHiveRequest) => void
  title?: string
  showCreateButton?: boolean
  showFilters?: boolean
  defaultView?: 'grid' | 'list'
}

type SortOption = 'name' | 'members' | 'activity' | 'created'
type FilterOption = 'all' | 'public' | 'private' | 'joined' | 'available'

export const HiveList: React.FC<HiveListProps> = ({
                                                    hives,
                                                    members = {},
                                                    currentUserId,
                                                    isLoading = false,
                                                    error = null,
                                                    onJoin,
                                                    onLeave,
                                                    onEnter,
                                                    onSettings,
                                                    onShare,
                                                    onRefresh,
                                                    onCreateHive,
                                                    title = 'Hives',
                                                    showCreateButton = true,
                                                    showFilters = true,
                                                    defaultView = 'grid',
                                                  }) => {
  const theme = useTheme()
  const isMobile = useMediaQuery(theme.breakpoints.down('desktop'))

  // State for filters and search
  const [searchQuery, setSearchQuery] = useState('')
  const [sortBy, setSortBy] = useState<SortOption>('activity')
  const [filterBy, setFilterBy] = useState<FilterOption>('all')
  const [selectedTags, setSelectedTags] = useState<string[]>([])
  const [viewMode, setViewMode] = useState<'grid' | 'list'>(defaultView)
  const [createDialogOpen, setCreateDialogOpen] = useState(false)

  // Get all available tags from hives
  const allTags = useMemo(() => {
    const tags = new Set<string>()
    hives.forEach(hive => {
      hive.tags.forEach(tag => tags.add(tag))
    })
    return Array.from(tags).sort()
  }, [hives])

  // Filter and sort hives
  const filteredHives = useMemo(() => {
    const filtered = hives.filter(hive => {
      // Search filter
      if (searchQuery) {
        const query = searchQuery.toLowerCase()
        const matchesName = hive.name.toLowerCase().includes(query)
        const matchesDescription = hive.description.toLowerCase().includes(query)
        const matchesTags = hive.tags.some(tag => tag.toLowerCase().includes(query))

        if (!matchesName && !matchesDescription && !matchesTags) {
          return false
        }
      }

      // Category filter
      if (filterBy !== 'all') {
        const isMember = currentUserId && members[hive.id]?.some(m => m.userId === currentUserId)

        switch (filterBy) {
          case 'public':
            if (!hive.isPublic) return false
            break
          case 'private':
            if (hive.isPublic) return false
            break
          case 'joined':
            if (!isMember) return false
            break
          case 'available':
            if (isMember || hive.currentMembers >= hive.maxMembers) return false
            break
        }
      }

      // Tags filter - ALL selected tags must be present in the hive
      if (selectedTags.length > 0) {
        if (!selectedTags.every(tag => hive.tags.includes(tag))) {
          return false
        }
      }

      return true
    })

    // Sort hives
    filtered.sort((a, b) => {
      switch (sortBy) {
        case 'name':
          return a.name.localeCompare(b.name)
        case 'members':
          return b.currentMembers - a.currentMembers
        case 'activity': {
          // Sort by online members, then by total members
          const aOnline = members[a.id]?.filter(m => m.isActive).length || 0
          const bOnline = members[b.id]?.filter(m => m.isActive).length || 0
          if (aOnline !== bOnline) return bOnline - aOnline
          return b.currentMembers - a.currentMembers
        }
        case 'created':
          return new Date(b.createdAt).getTime() - new Date(a.createdAt).getTime()
        default:
          return 0
      }
    })

    return filtered
  }, [hives, searchQuery, sortBy, filterBy, selectedTags, members, currentUserId])

  const handleTagToggle = (tag: string): void => {
    setSelectedTags(prev =>
        prev.includes(tag)
            ? prev.filter(t => t !== tag)
            : [...prev, tag]
    )
  }

  const handleCreateHive = (hiveData: CreateHiveRequest): void => {
    onCreateHive?.(hiveData)
    setCreateDialogOpen(false)
  }

  const renderLoadingSkeleton = () => (
      <ContentSkeleton
          type="hive"
          count={6}
          animation="wave"
      />
  )

  const renderFilters = (): React.ReactElement | null => {
    if (!showFilters) return null

    return (
        <Paper elevation={0} sx={{p: 2, mb: 3, bgcolor: 'background.default'}}>
          <Stack spacing={2}>
            {/* Search and View Toggle */}
            <Box sx={{display: 'flex', gap: 2, alignItems: 'center', flexWrap: 'wrap'}}>
              <TextField
                  placeholder="Search hives..."
                  value={searchQuery}
                  onChange={(e) => setSearchQuery(e.target.value)}
                  size="small"
                  InputProps={{
                    startAdornment: (
                        <InputAdornment position="start">
                          <SearchIcon/>
                        </InputAdornment>
                    ),
                  }}
                  sx={{flexGrow: 1, minWidth: 200}}
              />

              {!isMobile && (
                  <ToggleButtonGroup
                      value={viewMode}
                      exclusive
                      onChange={(_, newView) => newView && setViewMode(newView)}
                      size="small"
                  >
                    <ToggleButton value="grid" aria-label="grid view">
                      <ViewModuleIcon/>
                    </ToggleButton>
                    <ToggleButton value="list" aria-label="list view">
                      <ViewListIcon/>
                    </ToggleButton>
                  </ToggleButtonGroup>
              )}
            </Box>

            {/* Filters */}
            <Box sx={{display: 'flex', gap: 2, alignItems: 'center', flexWrap: 'wrap'}}>
              <FormControl size="small" sx={{minWidth: 120}}>
                <InputLabel>Category</InputLabel>
                <Select
                    value={filterBy}
                    label="Category"
                    onChange={(e) => setFilterBy(e.target.value as FilterOption)}
                >
                  <MenuItem value="all">All Hives</MenuItem>
                  <MenuItem value="joined">Joined</MenuItem>
                  <MenuItem value="available">Available</MenuItem>
                  <MenuItem value="public">Public</MenuItem>
                  <MenuItem value="private">Private</MenuItem>
                </Select>
              </FormControl>

              <FormControl size="small" sx={{minWidth: 120}}>
                <InputLabel>Sort by</InputLabel>
                <Select
                    value={sortBy}
                    label="Sort by"
                    onChange={(e) => setSortBy(e.target.value as SortOption)}
                >
                  <MenuItem value="activity">Activity</MenuItem>
                  <MenuItem value="name">Name</MenuItem>
                  <MenuItem value="members">Members</MenuItem>
                  <MenuItem value="created">Created</MenuItem>
                </Select>
              </FormControl>
            </Box>

            {/* Tags Filter */}
            {allTags.length > 0 && (
                <Box>
                  <Typography variant="body2" color="text.secondary" sx={{mb: 1}}>
                    Filter by tags:
                  </Typography>
                  <Box sx={{display: 'flex', gap: 1, flexWrap: 'wrap'}}>
                    {allTags.map(tag => (
                        <Chip
                            key={tag}
                            label={tag}
                            size="small"
                            variant={selectedTags.includes(tag) ? 'filled' : 'outlined'}
                            color={selectedTags.includes(tag) ? 'primary' : 'default'}
                            onClick={() => handleTagToggle(tag)}
                            sx={{cursor: 'pointer'}}
                        />
                    ))}
                  </Box>
                </Box>
            )}
          </Stack>
        </Paper>
    )
  }

  if (error) {
    return (
        <Alert
            severity="error"
            action={
                onRefresh && (
                    <Button color="inherit" size="small" onClick={onRefresh}>
                      <RefreshIcon sx={{mr: 1}}/>
                      Retry
                    </Button>
                )
            }
        >
          {error}
        </Alert>
    )
  }

  return (
      <Box>
        {/* Header */}
        <Box sx={{display: 'flex', justifyContent: 'space-between', alignItems: 'center', mb: 3}}>
          <Typography variant="h4" component="h1" fontWeight={600}>
            {title}
          </Typography>

          <Box sx={{display: 'flex', gap: 1}}>
            {onRefresh && (
                <Button
                    variant="outlined"
                    startIcon={<RefreshIcon/>}
                    onClick={onRefresh}
                    disabled={isLoading}
                >
                  Refresh
                </Button>
            )}

            {showCreateButton && (
                <Button
                    variant="contained"
                    startIcon={<AddIcon/>}
                    onClick={() => setCreateDialogOpen(true)}
                >
                  Create Hive
                </Button>
            )}
          </Box>
        </Box>

        {/* Filters */}
        {renderFilters()}

        {/* Results Count */}
        <Typography variant="body2" color="text.secondary" sx={{mb: 2}}>
          {isLoading ? 'Loading...' : `${filteredHives.length} hive${filteredHives.length !== 1 ? 's' : ''} found`}
        </Typography>

        {/* Hives Grid/List */}
        {isLoading ? (
            renderLoadingSkeleton()
        ) : filteredHives.length === 0 ? (
            <Box sx={{textAlign: 'center', py: 8}}>
              <Typography variant="h6" color="text.secondary" gutterBottom>
                No hives found
              </Typography>
              <Typography variant="body2" color="text.secondary" sx={{mb: 3}}>
                {searchQuery || selectedTags.length > 0
                    ? 'Try adjusting your search or filters'
                    : 'Be the first to create a hive!'
                }
              </Typography>
              {showCreateButton && (
                  <Button
                      variant="contained"
                      startIcon={<AddIcon/>}
                      onClick={() => setCreateDialogOpen(true)}
                  >
                    Create Your First Hive
                  </Button>
              )}
            </Box>
        ) : (
            <Box
                sx={{
                  display: 'grid',
                  gridTemplateColumns: viewMode === 'list'
                      ? '1fr'
                      : {
                        mobile: '1fr',
                        tablet: '1fr 1fr',
                        desktop: 'repeat(3, 1fr)',
                        desktopLg: 'repeat(4, 1fr)'
                      },
                  gap: 3
                }}
            >
              {filteredHives.map((hive) => (
                  <HiveCard
                      key={hive.id}
                      hive={hive}
                      members={members[hive.id] || []}
                      currentUserId={currentUserId}
                      onJoin={onJoin}
                      onLeave={onLeave}
                      onEnter={onEnter}
                      onSettings={onSettings}
                      onShare={onShare}
                      variant={viewMode === 'list' ? 'compact' : 'default'}
                  />
              ))}
            </Box>
        )}

        {/* Create Hive Dialog */}
        {createDialogOpen && (
            <CreateHiveForm
                open={createDialogOpen}
                onClose={() => setCreateDialogOpen(false)}
                onSubmit={handleCreateHive}
            />
        )}
      </Box>
  )
}

export default HiveList