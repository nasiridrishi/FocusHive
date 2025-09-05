import React, { useState, useCallback, useRef, useMemo } from 'react'
import {
  Box,
  Card,
  CardContent,
  Typography,
  IconButton,
  List,
  ListItem,
  ListItemText,
  ListItemAvatar,
  ListItemSecondaryAction,
  Avatar,
  Chip,
  Badge,
  Button,
  Tooltip,
  LinearProgress,
  Divider,
  Menu,
  MenuItem,
  Dialog,
  DialogTitle,
  DialogContent,
  DialogActions,
  TextField,
  InputAdornment,
  Fab,
  useTheme,
  alpha,
} from '@mui/material'
import {
  DragHandle,
  PlayArrow,
  Remove,
  ThumbUp,
  ThumbDown,
  Person,
  Add,
  Search,
  MoreVert,
  QueueMusic,
  MusicNote,
  Schedule,
  TrendingUp,
  TrendingDown,
  Clear,
} from '@mui/icons-material'
import { useMusic } from '../../context'
import { useCollaborativePlaylist } from '../../hooks'
import { CollaborativeQueueProps, QueueItem, Track } from '../../types'

const CollaborativeQueue: React.FC<CollaborativeQueueProps> = ({
  hiveId,
  showVoting = true,
  maxQueueSize = 50,
  allowReordering = true,
  showAddButton = true,
}) => {
  const theme = useTheme()
  const { state } = useMusic()
  
  const {
    collaborativeState,
    addTrackToQueue,
    voteOnQueueTrack,
    reorderQueueItems,
    removeTrackFromQueue,
    canAddTracks,
    canVote,
    canReorder,
    queueIsFull,
    getVotingSummary,
  } = useCollaborativePlaylist({
    hiveId: hiveId || '',
    maxQueueSize,
    enableVoting: showVoting,
  })

  const [draggedItem, setDraggedItem] = useState<QueueItem | null>(null)
  const [dragOverIndex, setDragOverIndex] = useState<number | null>(null)
  const [searchDialogOpen, setSearchDialogOpen] = useState(false)
  const [searchQuery, setSearchQuery] = useState('')
  const [searchResults, setSearchResults] = useState<Track[]>([])
  const [menuAnchorEl, setMenuAnchorEl] = useState<null | HTMLElement>(null)
  const [selectedItem, setSelectedItem] = useState<QueueItem | null>(null)
  const [isSearching, setIsSearching] = useState(false)

  const dragRefs = useRef<{ [key: string]: HTMLElement | null }>({})

  const { queue, currentTrack } = state
  const votingSummary = getVotingSummary()

  // Sort queue by position and votes
  const sortedQueue = useMemo(() => {
    return [...queue].sort((a, b) => {
      // Current track always first
      if (a.id === currentTrack?.id) return -1
      if (b.id === currentTrack?.id) return 1
      
      // Then by position, then by votes
      const positionDiff = a.position - b.position
      if (positionDiff !== 0) return positionDiff
      
      return (b.votes || 0) - (a.votes || 0)
    })
  }, [queue, currentTrack])

  const formatDuration = useCallback((seconds: number) => {
    const minutes = Math.floor(seconds / 60)
    const remainingSeconds = Math.floor(seconds % 60)
    return `${minutes}:${remainingSeconds.toString().padStart(2, '0')}`
  }, [])

  const formatTimeAgo = useCallback((dateString?: string) => {
    if (!dateString) return 'Recently'
    
    const date = new Date(dateString)
    const now = new Date()
    const diffMinutes = Math.floor((now.getTime() - date.getTime()) / (1000 * 60))
    
    if (diffMinutes < 1) return 'Just now'
    if (diffMinutes < 60) return `${diffMinutes}m ago`
    if (diffMinutes < 1440) return `${Math.floor(diffMinutes / 60)}h ago`
    return `${Math.floor(diffMinutes / 1440)}d ago`
  }, [])

  // Drag and Drop Handlers
  const handleDragStart = useCallback((event: React.DragEvent, item: QueueItem) => {
    if (!canReorder) {
      event.preventDefault()
      return
    }
    
    setDraggedItem(item)
    event.dataTransfer.effectAllowed = 'move'
    event.dataTransfer.setData('text/plain', item.queueId)
  }, [canReorder])

  const handleDragOver = useCallback((event: React.DragEvent, index: number) => {
    if (!draggedItem || !canReorder) return
    
    event.preventDefault()
    event.dataTransfer.dropEffect = 'move'
    
    setDragOverIndex(index)
  }, [draggedItem, canReorder])

  const handleDragEnd = useCallback(() => {
    setDraggedItem(null)
    setDragOverIndex(null)
  }, [])

  const handleDrop = useCallback(async (event: React.DragEvent, targetIndex: number) => {
    event.preventDefault()
    
    if (!draggedItem || !canReorder) return
    
    const sourceIndex = sortedQueue.findIndex(item => item.queueId === draggedItem.queueId)
    if (sourceIndex === targetIndex || sourceIndex === -1) return
    
    try {
      await reorderQueueItems(sourceIndex, targetIndex)
    } catch (error) {
      // Failed to reorder queue
    }
    
    handleDragEnd()
  }, [draggedItem, canReorder, sortedQueue, reorderQueueItems, handleDragEnd])

  // Vote handlers
  const handleVoteUp = useCallback(async (queueId: string, event: React.MouseEvent) => {
    event.stopPropagation()
    if (!canVote) return
    
    try {
      await voteOnQueueTrack(queueId, 'up')
    } catch (error) {
      // Failed to vote
    }
  }, [canVote, voteOnQueueTrack])

  const handleVoteDown = useCallback(async (queueId: string, event: React.MouseEvent) => {
    event.stopPropagation()
    if (!canVote) return
    
    try {
      await voteOnQueueTrack(queueId, 'down')
    } catch (error) {
      // Failed to vote
    }
  }, [canVote, voteOnQueueTrack])

  // Menu handlers
  const handleMenuOpen = useCallback((event: React.MouseEvent, item: QueueItem) => {
    event.stopPropagation()
    setMenuAnchorEl(event.currentTarget)
    setSelectedItem(item)
  }, [])

  const handleMenuClose = useCallback(() => {
    setMenuAnchorEl(null)
    setSelectedItem(null)
  }, [])

  const handleRemoveTrack = useCallback(async () => {
    if (selectedItem) {
      try {
        await removeTrackFromQueue(selectedItem.queueId)
        handleMenuClose()
      } catch (error) {
        // Failed to remove track
      }
    }
  }, [selectedItem, removeTrackFromQueue, handleMenuClose])

  // Search handlers
  const handleSearchOpen = useCallback(() => {
    setSearchDialogOpen(true)
    setSearchQuery('')
    setSearchResults([])
  }, [])

  const handleSearchClose = useCallback(() => {
    setSearchDialogOpen(false)
    setSearchQuery('')
    setSearchResults([])
  }, [])

  const handleSearchChange = useCallback(async (event: React.ChangeEvent<HTMLInputElement>) => {
    const query = event.target.value
    setSearchQuery(query)
    
    if (query.length < 2) {
      setSearchResults([])
      return
    }
    
    // TODO: Implement actual search via API
    setIsSearching(true)
    try {
      // Mock search results for now
      const mockResults: Track[] = [
        {
          id: 'track-1',
          title: `${query} - Song 1`,
          artist: 'Artist 1',
          duration: 180,
          explicit: false,
        },
        {
          id: 'track-2',
          title: `${query} - Song 2`,
          artist: 'Artist 2',
          duration: 220,
          explicit: true,
        },
      ]
      setSearchResults(mockResults)
    } catch (error) {
      setSearchResults([])
    } finally {
      setIsSearching(false)
    }
  }, [])

  const handleAddTrack = useCallback(async (track: Track) => {
    try {
      await addTrackToQueue(track)
      handleSearchClose()
    } catch (error) {
      // Failed to add track
    }
  }, [addTrackToQueue, handleSearchClose])

  const totalDuration = useMemo(() => {
    return sortedQueue.reduce((total, item) => total + item.duration, 0)
  }, [sortedQueue])

  const estimatedWaitTime = useMemo(() => {
    if (!currentTrack || sortedQueue.length === 0) return 0
    
    const currentTrackRemaining = Math.max(0, currentTrack.duration - state.playbackState.currentTime)
    const upcomingDuration = sortedQueue
      .filter(item => item.id !== currentTrack.id)
      .reduce((total, item) => total + item.duration, 0)
    
    return currentTrackRemaining + upcomingDuration
  }, [currentTrack, sortedQueue, state.playbackState.currentTime])

  return (
    <Card>
      <CardContent>
        {/* Header */}
        <Box display="flex" justifyContent="space-between" alignItems="center" mb={2}>
          <Box display="flex" alignItems="center" gap={2}>
            <Typography variant="h6" fontWeight="bold">
              Queue
            </Typography>
            <Badge 
              badgeContent={sortedQueue.length} 
              color="primary"
              max={99}
            >
              <QueueMusic />
            </Badge>
            {queueIsFull && (
              <Chip 
                label="Full" 
                color="warning" 
                size="small"
                variant="outlined"
              />
            )}
          </Box>

          <Box display="flex" alignItems="center" gap={1}>
            {collaborativeState.activeUsers.length > 0 && (
              <Tooltip 
                title={`${collaborativeState.activeUsers.length} active user${collaborativeState.activeUsers.length > 1 ? 's' : ''}`}
              >
                <Badge 
                  badgeContent={collaborativeState.activeUsers.length} 
                  color="success"
                >
                  <Person />
                </Badge>
              </Tooltip>
            )}
            
            {showAddButton && canAddTracks && (
              <Tooltip title="Add track to queue">
                <IconButton onClick={handleSearchOpen}>
                  <Add />
                </IconButton>
              </Tooltip>
            )}
          </Box>
        </Box>

        {/* Stats */}
        <Box display="flex" justifyContent="space-between" alignItems="center" mb={2}>
          <Typography variant="body2" color="text.secondary">
            {sortedQueue.length} tracks • {formatDuration(totalDuration)}
          </Typography>
          {estimatedWaitTime > 0 && (
            <Typography variant="body2" color="text.secondary">
              <Schedule sx={{ fontSize: '1rem', mr: 0.5, verticalAlign: 'middle' }} />
              ~{formatDuration(estimatedWaitTime)} remaining
            </Typography>
          )}
        </Box>

        {/* Queue Progress */}
        {maxQueueSize && (
          <Box mb={2}>
            <LinearProgress 
              variant="determinate" 
              value={(sortedQueue.length / maxQueueSize) * 100}
              sx={{
                height: 6,
                borderRadius: 1,
                backgroundColor: alpha(theme.palette.primary.main, 0.1),
                '& .MuiLinearProgress-bar': {
                  borderRadius: 1,
                  backgroundColor: sortedQueue.length >= maxQueueSize * 0.8 
                    ? theme.palette.warning.main 
                    : theme.palette.primary.main,
                },
              }}
            />
            <Typography variant="caption" color="text.secondary">
              {sortedQueue.length} / {maxQueueSize} tracks
            </Typography>
          </Box>
        )}

        {/* Empty State */}
        {sortedQueue.length === 0 ? (
          <Box 
            display="flex" 
            flexDirection="column" 
            alignItems="center" 
            justifyContent="center"
            py={6}
            textAlign="center"
          >
            <QueueMusic sx={{ fontSize: 48, color: 'text.secondary', mb: 2 }} />
            <Typography variant="h6" gutterBottom>
              Queue is empty
            </Typography>
            <Typography variant="body2" color="text.secondary" sx={{ mb: 3 }}>
              {canAddTracks 
                ? 'Add some tracks to get the music flowing!'
                : 'Wait for someone to add tracks to the queue'
              }
            </Typography>
            {showAddButton && canAddTracks && (
              <Button
                variant="contained"
                startIcon={<Add />}
                onClick={handleSearchOpen}
              >
                Add Track
              </Button>
            )}
          </Box>
        ) : (
          <List sx={{ maxHeight: 400, overflow: 'auto' }}>
            {sortedQueue.map((item, index) => {
              const isCurrentTrack = item.id === currentTrack?.id
              const isDragOver = dragOverIndex === index
              
              return (
                <React.Fragment key={item.queueId}>
                  <ListItem
                    ref={(el) => { dragRefs.current[item.queueId] = el }}
                    draggable={allowReordering && canReorder && !isCurrentTrack}
                    onDragStart={(e) => handleDragStart(e, item)}
                    onDragOver={(e) => handleDragOver(e, index)}
                    onDragEnd={handleDragEnd}
                    onDrop={(e) => handleDrop(e, index)}
                    sx={{
                      border: isCurrentTrack 
                        ? `2px solid ${theme.palette.primary.main}`
                        : isDragOver 
                        ? `2px dashed ${theme.palette.primary.main}`
                        : '2px solid transparent',
                      borderRadius: 1,
                      mb: 0.5,
                      backgroundColor: isCurrentTrack 
                        ? alpha(theme.palette.primary.main, 0.08)
                        : isDragOver
                        ? alpha(theme.palette.primary.main, 0.04)
                        : 'transparent',
                      cursor: allowReordering && canReorder && !isCurrentTrack ? 'grab' : 'default',
                      '&:hover': {
                        backgroundColor: alpha(theme.palette.primary.main, 0.04),
                      },
                      transition: 'all 0.2s',
                    }}
                  >
                    {/* Drag Handle */}
                    {allowReordering && canReorder && !isCurrentTrack && (
                      <Box mr={1} color="action.active">
                        <DragHandle fontSize="small" />
                      </Box>
                    )}

                    {/* Track Number / Play Indicator */}
                    <Box 
                      width={32} 
                      display="flex" 
                      alignItems="center" 
                      justifyContent="center"
                      mr={1}
                    >
                      {isCurrentTrack ? (
                        <PlayArrow color="primary" />
                      ) : (
                        <Typography 
                          variant="caption" 
                          color="text.secondary"
                          fontWeight="bold"
                        >
                          {index + 1}
                        </Typography>
                      )}
                    </Box>

                    {/* Album Art */}
                    <ListItemAvatar>
                      <Avatar 
                        src={item.albumArt}
                        sx={{ 
                          width: 40, 
                          height: 40,
                          borderRadius: 1,
                        }}
                      >
                        <MusicNote />
                      </Avatar>
                    </ListItemAvatar>

                    {/* Track Info */}
                    <ListItemText
                      primary={
                        <Box display="flex" alignItems="center" gap={1}>
                          <Typography 
                            variant="body2" 
                            fontWeight={isCurrentTrack ? 'bold' : 'normal'}
                            noWrap
                          >
                            {item.title}
                          </Typography>
                          {item.explicit && (
                            <Chip 
                              label="E" 
                              size="small" 
                              variant="outlined"
                              color="warning"
                              sx={{ minWidth: 'auto', height: 18, fontSize: '0.625rem' }}
                            />
                          )}
                        </Box>
                      }
                      secondary={
                        <Box display="flex" alignItems="center" gap={2}>
                          <Typography variant="caption" color="text.secondary">
                            {item.artist} • {formatDuration(item.duration)}
                          </Typography>
                          {item.addedBy && (
                            <Chip
                              size="small"
                              label={item.addedBy.name}
                              avatar={
                                <Avatar 
                                  src={item.addedBy.avatar}
                                  sx={{ width: 16, height: 16 }}
                                >
                                  {item.addedBy.name[0]}
                                </Avatar>
                              }
                              sx={{ height: 20, fontSize: '0.6rem' }}
                            />
                          )}
                          {item.addedAt && (
                            <Typography variant="caption" color="text.secondary">
                              {formatTimeAgo(item.addedAt)}
                            </Typography>
                          )}
                        </Box>
                      }
                    />

                    {/* Actions */}
                    <ListItemSecondaryAction>
                      <Box display="flex" alignItems="center" gap={1}>
                        {/* Voting */}
                        {showVoting && canVote && (
                          <>
                            <Tooltip title="Vote up">
                              <span>
                                <IconButton
                                  size="small"
                                  onClick={(e) => handleVoteUp(item.queueId, e)}
                                  disabled={item.userVote === 'up'}
                                  color={item.userVote === 'up' ? 'primary' : 'default'}
                                >
                                  <ThumbUp fontSize="small" />
                                </IconButton>
                              </span>
                            </Tooltip>
                            
                            <Typography variant="caption" sx={{ minWidth: 20, textAlign: 'center' }}>
                              {item.votes || 0}
                            </Typography>
                            
                            <Tooltip title="Vote down">
                              <span>
                                <IconButton
                                  size="small"
                                  onClick={(e) => handleVoteDown(item.queueId, e)}
                                  disabled={item.userVote === 'down'}
                                  color={item.userVote === 'down' ? 'error' : 'default'}
                                >
                                  <ThumbDown fontSize="small" />
                                </IconButton>
                              </span>
                            </Tooltip>
                          </>
                        )}

                        {/* Menu */}
                        <IconButton
                          size="small"
                          onClick={(e) => handleMenuOpen(e, item)}
                        >
                          <MoreVert fontSize="small" />
                        </IconButton>
                      </Box>
                    </ListItemSecondaryAction>
                  </ListItem>
                  
                  {index < sortedQueue.length - 1 && <Divider />}
                </React.Fragment>
              )
            })}
          </List>
        )}

        {/* Voting Summary */}
        {votingSummary && showVoting && (
          <Box mt={2} p={2} bgcolor="action.hover" borderRadius={1}>
            <Typography variant="caption" color="text.secondary" gutterBottom>
              Current Track Voting
            </Typography>
            <Box display="flex" alignItems="center" gap={2}>
              <Box display="flex" alignItems="center" gap={0.5}>
                <TrendingUp color="success" fontSize="small" />
                <Typography variant="body2">{votingSummary.upVotes}</Typography>
              </Box>
              <Box display="flex" alignItems="center" gap={0.5}>
                <TrendingDown color="error" fontSize="small" />
                <Typography variant="body2">{votingSummary.downVotes}</Typography>
              </Box>
              <Typography variant="caption" color="text.secondary">
                Skip: {votingSummary.skipVotes}/{votingSummary.skipThreshold}
              </Typography>
            </Box>
          </Box>
        )}
      </CardContent>

      {/* Context Menu */}
      <Menu
        anchorEl={menuAnchorEl}
        open={Boolean(menuAnchorEl)}
        onClose={handleMenuClose}
      >
        <MenuItem onClick={handleRemoveTrack} sx={{ color: 'error.main' }}>
          <Remove sx={{ mr: 1 }} />
          Remove from queue
        </MenuItem>
      </Menu>

      {/* Add Track Dialog */}
      <Dialog
        open={searchDialogOpen}
        onClose={handleSearchClose}
        maxWidth="md"
        fullWidth
      >
        <DialogTitle>Add Track to Queue</DialogTitle>
        <DialogContent>
          <TextField
            fullWidth
            placeholder="Search for tracks..."
            value={searchQuery}
            onChange={handleSearchChange}
            InputProps={{
              startAdornment: (
                <InputAdornment position="start">
                  <Search />
                </InputAdornment>
              ),
              endAdornment: searchQuery && (
                <InputAdornment position="end">
                  <IconButton
                    size="small"
                    onClick={() => setSearchQuery('')}
                  >
                    <Clear />
                  </IconButton>
                </InputAdornment>
              ),
            }}
            sx={{ mb: 2 }}
          />

          {isSearching && <LinearProgress sx={{ mb: 2 }} />}

          <List sx={{ maxHeight: 300, overflow: 'auto' }}>
            {searchResults.map((track) => (
              <ListItem key={track.id}>
                <ListItemAvatar>
                  <Avatar src={track.albumArt}>
                    <MusicNote />
                  </Avatar>
                </ListItemAvatar>
                <ListItemText
                  primary={track.title}
                  secondary={`${track.artist} • ${formatDuration(track.duration)}`}
                />
                <ListItemSecondaryAction>
                  <Button
                    size="small"
                    startIcon={<Add />}
                    onClick={() => handleAddTrack(track)}
                    disabled={queueIsFull}
                  >
                    Add
                  </Button>
                </ListItemSecondaryAction>
              </ListItem>
            ))}
          </List>

          {searchQuery.length >= 2 && searchResults.length === 0 && !isSearching && (
            <Box textAlign="center" py={4}>
              <Typography variant="body2" color="text.secondary">
                No tracks found for "{searchQuery}"
              </Typography>
            </Box>
          )}
        </DialogContent>
        <DialogActions>
          <Button onClick={handleSearchClose}>
            Close
          </Button>
        </DialogActions>
      </Dialog>

      {/* Add Track FAB */}
      {showAddButton && canAddTracks && sortedQueue.length > 0 && (
        <Fab
          size="small"
          color="primary"
          onClick={handleSearchOpen}
          sx={{
            position: 'absolute',
            bottom: 16,
            right: 16,
          }}
        >
          <Add />
        </Fab>
      )}
    </Card>
  )
}

export default CollaborativeQueue