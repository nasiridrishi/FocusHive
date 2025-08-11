import React, { useState, useCallback, useEffect, useMemo } from 'react'
import {
  Box,
  Card,
  CardContent,
  CardMedia,
  CardActions,
  Typography,
  IconButton,
  Button,
  TextField,
  InputAdornment,
  Chip,
  Menu,
  MenuItem,
  Dialog,
  DialogTitle,
  DialogContent,
  DialogActions,
  Grid,
  Skeleton,
  Fab,
  Tooltip,
  Badge,
  alpha,
  useTheme,
} from '@mui/material'
import {
  Search,
  FilterList,
  Add,
  PlayArrow,
  Edit,
  Share,
  Delete,
  MoreVert,
  Public,
  Lock,
  People,
  SmartToy,
  MusicNote,
  Favorite,
  FavoriteBorder,
} from '@mui/icons-material'
import { useMusic } from '../../context'
import { PlaylistSelectorProps, Playlist, CreatePlaylistRequest } from '../../types'

const PlaylistSelector: React.FC<PlaylistSelectorProps> = ({
  onPlaylistSelect,
  selectedPlaylistId,
  showCreateButton = true,
  hiveId,
  type = 'all',
}) => {
  const theme = useTheme()
  const { state, loadPlaylists, createPlaylist, deletePlaylist } = useMusic()
  
  const [searchQuery, setSearchQuery] = useState('')
  const [filterType, setFilterType] = useState<typeof type>(type)
  const [createDialogOpen, setCreateDialogOpen] = useState(false)
  const [menuAnchorEl, setMenuAnchorEl] = useState<null | HTMLElement>(null)
  const [selectedPlaylist, setSelectedPlaylist] = useState<Playlist | null>(null)
  const [newPlaylist, setNewPlaylist] = useState<CreatePlaylistRequest>({
    name: '',
    description: '',
    isPublic: true,
    isCollaborative: false,
    hiveId,
  })
  const [favoritePlaylist, setFavoritePlaylist] = useState<Set<string>>(new Set())

  // Load playlists on mount
  useEffect(() => {
    loadPlaylists()
  }, [loadPlaylists])

  // Filter and search playlists
  const filteredPlaylists = useMemo(() => {
    let filtered = state.playlists

    // Filter by type
    if (filterType !== 'all') {
      filtered = filtered.filter(playlist => {
        switch (filterType) {
          case 'personal':
            return playlist.type === 'personal'
          case 'hive':
            return playlist.type === 'hive'
          case 'smart':
            return playlist.type === 'smart'
          default:
            return true
        }
      })
    }

    // Filter by hive if specified
    if (hiveId) {
      filtered = filtered.filter(playlist => 
        playlist.hiveId === hiveId || playlist.type === 'personal'
      )
    }

    // Search filter
    if (searchQuery) {
      const query = searchQuery.toLowerCase()
      filtered = filtered.filter(playlist =>
        playlist.name.toLowerCase().includes(query) ||
        playlist.description?.toLowerCase().includes(query) ||
        playlist.createdBy.name.toLowerCase().includes(query)
      )
    }

    return filtered.sort((a, b) => {
      // Sort by: selected first, then by updated date
      if (a.id === selectedPlaylistId) return -1
      if (b.id === selectedPlaylistId) return 1
      return new Date(b.updatedAt).getTime() - new Date(a.updatedAt).getTime()
    })
  }, [state.playlists, filterType, hiveId, searchQuery, selectedPlaylistId])

  // Handlers
  const handleSearchChange = useCallback((event: React.ChangeEvent<HTMLInputElement>) => {
    setSearchQuery(event.target.value)
  }, [])

  const handleFilterChange = useCallback((newType: typeof filterType) => {
    setFilterType(newType)
  }, [])

  const handlePlaylistSelect = useCallback((playlist: Playlist) => {
    onPlaylistSelect(playlist)
  }, [onPlaylistSelect])

  const handlePlaylistPlay = useCallback((playlist: Playlist, event: React.MouseEvent) => {
    event.stopPropagation()
    // TODO: Implement play playlist functionality
  }, [])

  const handleMenuOpen = useCallback((event: React.MouseEvent<HTMLElement>, playlist: Playlist) => {
    event.stopPropagation()
    setMenuAnchorEl(event.currentTarget)
    setSelectedPlaylist(playlist)
  }, [])

  const handleMenuClose = useCallback(() => {
    setMenuAnchorEl(null)
    setSelectedPlaylist(null)
  }, [])

  const handleCreateDialogOpen = useCallback(() => {
    setCreateDialogOpen(true)
    setNewPlaylist({
      name: '',
      description: '',
      isPublic: true,
      isCollaborative: false,
      hiveId,
    })
  }, [hiveId])

  const handleCreateDialogClose = useCallback(() => {
    setCreateDialogOpen(false)
  }, [])

  const handleCreatePlaylist = useCallback(async () => {
    if (!newPlaylist.name.trim()) return

    try {
      const playlist = await createPlaylist(newPlaylist)
      setCreateDialogOpen(false)
      onPlaylistSelect(playlist)
    } catch (error) {
      // Failed to create playlist
    }
  }, [newPlaylist, createPlaylist, onPlaylistSelect])

  const handleDeletePlaylist = useCallback(async () => {
    if (selectedPlaylist) {
      try {
        await deletePlaylist(selectedPlaylist.id)
        handleMenuClose()
      } catch (error) {
        // Failed to delete playlist
      }
    }
  }, [selectedPlaylist, deletePlaylist, handleMenuClose])

  const handleToggleFavorite = useCallback((playlistId: string, event: React.MouseEvent) => {
    event.stopPropagation()
    setFavoritePlaylist(prev => {
      const newSet = new Set(prev)
      if (newSet.has(playlistId)) {
        newSet.delete(playlistId)
      } else {
        newSet.add(playlistId)
      }
      return newSet
    })
    // TODO: Implement API call to save favorite status
  }, [])

  const getPlaylistIcon = useCallback((playlist: Playlist) => {
    switch (playlist.type) {
      case 'smart':
        return <SmartToy />
      case 'hive':
        return <People />
      default:
        return <MusicNote />
    }
  }, [])

  const getPlaylistTypeColor = useCallback((playlistType: string) => {
    switch (playlistType) {
      case 'smart':
        return theme.palette.secondary.main
      case 'hive':
        return theme.palette.primary.main
      default:
        return theme.palette.text.secondary
    }
  }, [theme])

  const formatDuration = useCallback((seconds: number) => {
    const hours = Math.floor(seconds / 3600)
    const minutes = Math.floor((seconds % 3600) / 60)
    
    if (hours > 0) {
      return `${hours}h ${minutes}m`
    }
    return `${minutes}m`
  }, [])

  if (state.isLoading && filteredPlaylists.length === 0) {
    return (
      <Box>
        {/* Search and Filters Skeleton */}
        <Box mb={3}>
          <Skeleton variant="rectangular" height={56} sx={{ mb: 2 }} />
          <Box display="flex" gap={1}>
            {Array.from({ length: 4 }).map((_, index) => (
              <Skeleton key={index} variant="rectangular" width={80} height={32} />
            ))}
          </Box>
        </Box>

        {/* Grid Skeleton */}
        <Grid container spacing={3}>
          {Array.from({ length: 6 }).map((_, index) => (
            <Grid item xs={12} sm={6} md={4} key={index}>
              <Card>
                <Skeleton variant="rectangular" height={140} />
                <CardContent>
                  <Skeleton variant="text" height={24} />
                  <Skeleton variant="text" height={20} width="60%" />
                  <Skeleton variant="text" height={16} width="40%" />
                </CardContent>
              </Card>
            </Grid>
          ))}
        </Grid>
      </Box>
    )
  }

  return (
    <Box>
      {/* Search and Filter Header */}
      <Box mb={3}>
        <TextField
          fullWidth
          placeholder="Search playlists..."
          value={searchQuery}
          onChange={handleSearchChange}
          InputProps={{
            startAdornment: (
              <InputAdornment position="start">
                <Search />
              </InputAdornment>
            ),
          }}
          sx={{ mb: 2 }}
        />

        <Box display="flex" alignItems="center" justifyContent="space-between" flexWrap="wrap" gap={1}>
          <Box display="flex" gap={1} flexWrap="wrap">
            <Chip
              label="All"
              onClick={() => handleFilterChange('all')}
              color={filterType === 'all' ? 'primary' : 'default'}
              variant={filterType === 'all' ? 'filled' : 'outlined'}
              icon={<FilterList />}
            />
            <Chip
              label="Personal"
              onClick={() => handleFilterChange('personal')}
              color={filterType === 'personal' ? 'primary' : 'default'}
              variant={filterType === 'personal' ? 'filled' : 'outlined'}
              icon={<Lock />}
            />
            <Chip
              label="Hive"
              onClick={() => handleFilterChange('hive')}
              color={filterType === 'hive' ? 'primary' : 'default'}
              variant={filterType === 'hive' ? 'filled' : 'outlined'}
              icon={<People />}
            />
            <Chip
              label="Smart"
              onClick={() => handleFilterChange('smart')}
              color={filterType === 'smart' ? 'primary' : 'default'}
              variant={filterType === 'smart' ? 'filled' : 'outlined'}
              icon={<SmartToy />}
            />
          </Box>

          <Typography variant="body2" color="text.secondary">
            {filteredPlaylists.length} playlist{filteredPlaylists.length !== 1 ? 's' : ''}
          </Typography>
        </Box>
      </Box>

      {/* Empty State */}
      {filteredPlaylists.length === 0 ? (
        <Box 
          display="flex" 
          flexDirection="column" 
          alignItems="center" 
          justifyContent="center"
          py={8}
          textAlign="center"
        >
          <MusicNote sx={{ fontSize: 64, color: 'text.secondary', mb: 2 }} />
          <Typography variant="h6" gutterBottom>
            {searchQuery ? 'No playlists found' : 'No playlists yet'}
          </Typography>
          <Typography variant="body2" color="text.secondary" sx={{ mb: 3 }}>
            {searchQuery 
              ? 'Try adjusting your search or filter criteria'
              : 'Create your first playlist to get started'
            }
          </Typography>
          {showCreateButton && !searchQuery && (
            <Button
              variant="contained"
              startIcon={<Add />}
              onClick={handleCreateDialogOpen}
              size="large"
            >
              Create Playlist
            </Button>
          )}
        </Box>
      ) : (
        <>
          {/* Playlists Grid */}
          <Grid container spacing={3}>
            {filteredPlaylists.map((playlist) => (
              <Grid item xs={12} sm={6} md={4} key={playlist.id}>
                <Card
                  sx={{
                    cursor: 'pointer',
                    transition: 'all 0.2s',
                    border: selectedPlaylistId === playlist.id 
                      ? `2px solid ${theme.palette.primary.main}` 
                      : '2px solid transparent',
                    '&:hover': {
                      transform: 'translateY(-4px)',
                      boxShadow: theme.shadows[8],
                    },
                  }}
                  onClick={() => handlePlaylistSelect(playlist)}
                >
                  {/* Cover Image */}
                  <Box position="relative">
                    <CardMedia
                      component="div"
                      sx={{
                        height: 140,
                        backgroundColor: alpha(getPlaylistTypeColor(playlist.type), 0.1),
                        display: 'flex',
                        alignItems: 'center',
                        justifyContent: 'center',
                        position: 'relative',
                      }}
                    >
                      {playlist.coverImage ? (
                        <img
                          src={playlist.coverImage}
                          alt={playlist.name}
                          style={{
                            width: '100%',
                            height: '100%',
                            objectFit: 'cover',
                          }}
                        />
                      ) : (
                        <Box
                          sx={{
                            color: getPlaylistTypeColor(playlist.type),
                            fontSize: '3rem',
                          }}
                        >
                          {getPlaylistIcon(playlist)}
                        </Box>
                      )}

                      {/* Overlay Controls */}
                      <Box
                        sx={{
                          position: 'absolute',
                          top: 0,
                          left: 0,
                          right: 0,
                          bottom: 0,
                          backgroundColor: alpha(theme.palette.common.black, 0.6),
                          display: 'flex',
                          alignItems: 'center',
                          justifyContent: 'center',
                          opacity: 0,
                          transition: 'opacity 0.2s',
                          '&:hover': {
                            opacity: 1,
                          },
                        }}
                      >
                        <Tooltip title="Play playlist">
                          <IconButton
                            onClick={(e) => handlePlaylistPlay(playlist, e)}
                            sx={{
                              backgroundColor: theme.palette.primary.main,
                              color: theme.palette.primary.contrastText,
                              mr: 1,
                              '&:hover': {
                                backgroundColor: theme.palette.primary.dark,
                              },
                            }}
                          >
                            <PlayArrow />
                          </IconButton>
                        </Tooltip>
                      </Box>

                      {/* Privacy/Type Badge */}
                      <Chip
                        size="small"
                        label={playlist.isPublic ? 'Public' : 'Private'}
                        icon={playlist.isPublic ? <Public /> : <Lock />}
                        sx={{
                          position: 'absolute',
                          top: 8,
                          left: 8,
                          backgroundColor: alpha(theme.palette.background.paper, 0.9),
                        }}
                      />

                      {/* Favorite Button */}
                      <IconButton
                        onClick={(e) => handleToggleFavorite(playlist.id, e)}
                        sx={{
                          position: 'absolute',
                          top: 8,
                          right: 8,
                          backgroundColor: alpha(theme.palette.background.paper, 0.9),
                          '&:hover': {
                            backgroundColor: alpha(theme.palette.background.paper, 1),
                          },
                        }}
                        size="small"
                      >
                        {favoritePlaylist.has(playlist.id) ? (
                          <Favorite color="error" />
                        ) : (
                          <FavoriteBorder />
                        )}
                      </IconButton>
                    </Box>
                  </Box>

                  <CardContent sx={{ flexGrow: 1 }}>
                    <Box display="flex" alignItems="flex-start" justifyContent="space-between">
                      <Box flex={1} minWidth={0}>
                        <Typography 
                          variant="h6" 
                          gutterBottom 
                          noWrap
                          fontWeight="medium"
                        >
                          {playlist.name}
                        </Typography>
                        
                        {playlist.description && (
                          <Typography 
                            variant="body2" 
                            color="text.secondary"
                            sx={{
                              display: '-webkit-box',
                              WebkitLineClamp: 2,
                              WebkitBoxOrient: 'vertical',
                              overflow: 'hidden',
                              mb: 1,
                            }}
                          >
                            {playlist.description}
                          </Typography>
                        )}

                        <Typography variant="caption" color="text.secondary">
                          by {playlist.createdBy.name}
                        </Typography>
                      </Box>

                      <IconButton
                        onClick={(e) => handleMenuOpen(e, playlist)}
                        size="small"
                        sx={{ ml: 1 }}
                      >
                        <MoreVert />
                      </IconButton>
                    </Box>
                  </CardContent>

                  <CardActions sx={{ justifyContent: 'space-between', px: 2, pb: 2 }}>
                    <Box display="flex" alignItems="center" gap={1}>
                      <Badge 
                        badgeContent={playlist.trackCount} 
                        color="primary"
                        showZero
                        max={999}
                      >
                        <MusicNote fontSize="small" />
                      </Badge>
                      <Typography variant="caption" color="text.secondary">
                        {formatDuration(playlist.duration)}
                      </Typography>
                    </Box>

                    <Box display="flex" gap={1}>
                      {playlist.isCollaborative && (
                        <Tooltip title="Collaborative playlist">
                          <People fontSize="small" color="primary" />
                        </Tooltip>
                      )}
                      {playlist.type === 'smart' && (
                        <Tooltip title="Smart playlist">
                          <SmartToy fontSize="small" color="secondary" />
                        </Tooltip>
                      )}
                    </Box>
                  </CardActions>
                </Card>
              </Grid>
            ))}
          </Grid>

          {/* Create Button FAB */}
          {showCreateButton && (
            <Fab
              color="primary"
              aria-label="add playlist"
              onClick={handleCreateDialogOpen}
              sx={{
                position: 'fixed',
                bottom: 16,
                right: 16,
                zIndex: theme.zIndex.speedDial,
              }}
            >
              <Add />
            </Fab>
          )}
        </>
      )}

      {/* Context Menu */}
      <Menu
        anchorEl={menuAnchorEl}
        open={Boolean(menuAnchorEl)}
        onClose={handleMenuClose}
        anchorOrigin={{
          vertical: 'top',
          horizontal: 'right',
        }}
        transformOrigin={{
          vertical: 'top',
          horizontal: 'right',
        }}
      >
        <MenuItem onClick={handleMenuClose}>
          <Edit sx={{ mr: 1 }} />
          Edit
        </MenuItem>
        <MenuItem onClick={handleMenuClose}>
          <Share sx={{ mr: 1 }} />
          Share
        </MenuItem>
        <MenuItem onClick={handleDeletePlaylist} sx={{ color: 'error.main' }}>
          <Delete sx={{ mr: 1 }} />
          Delete
        </MenuItem>
      </Menu>

      {/* Create Playlist Dialog */}
      <Dialog
        open={createDialogOpen}
        onClose={handleCreateDialogClose}
        maxWidth="sm"
        fullWidth
      >
        <DialogTitle>Create New Playlist</DialogTitle>
        <DialogContent>
          <TextField
            autoFocus
            margin="dense"
            label="Playlist Name"
            fullWidth
            variant="outlined"
            value={newPlaylist.name}
            onChange={(e) => setNewPlaylist(prev => ({ ...prev, name: e.target.value }))}
            sx={{ mb: 2 }}
          />
          <TextField
            margin="dense"
            label="Description (optional)"
            fullWidth
            multiline
            rows={3}
            variant="outlined"
            value={newPlaylist.description}
            onChange={(e) => setNewPlaylist(prev => ({ ...prev, description: e.target.value }))}
            sx={{ mb: 2 }}
          />
          <Box display="flex" gap={1} flexWrap="wrap">
            <Chip
              label="Public"
              onClick={() => setNewPlaylist(prev => ({ ...prev, isPublic: true }))}
              color={newPlaylist.isPublic ? 'primary' : 'default'}
              variant={newPlaylist.isPublic ? 'filled' : 'outlined'}
              icon={<Public />}
            />
            <Chip
              label="Private"
              onClick={() => setNewPlaylist(prev => ({ ...prev, isPublic: false }))}
              color={!newPlaylist.isPublic ? 'primary' : 'default'}
              variant={!newPlaylist.isPublic ? 'filled' : 'outlined'}
              icon={<Lock />}
            />
            <Chip
              label="Collaborative"
              onClick={() => setNewPlaylist(prev => ({ ...prev, isCollaborative: !prev.isCollaborative }))}
              color={newPlaylist.isCollaborative ? 'primary' : 'default'}
              variant={newPlaylist.isCollaborative ? 'filled' : 'outlined'}
              icon={<People />}
            />
          </Box>
        </DialogContent>
        <DialogActions>
          <Button onClick={handleCreateDialogClose}>
            Cancel
          </Button>
          <Button 
            onClick={handleCreatePlaylist}
            variant="contained"
            disabled={!newPlaylist.name.trim()}
          >
            Create
          </Button>
        </DialogActions>
      </Dialog>
    </Box>
  )
}

export default PlaylistSelector