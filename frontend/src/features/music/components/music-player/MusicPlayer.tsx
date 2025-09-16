import React, {useCallback, useEffect, useMemo, useRef, useState} from 'react'
import {
  Alert,
  alpha,
  Avatar,
  Box,
  Button,
  Card,
  CardContent,
  Chip,
  Collapse,
  Fade,
  IconButton,
  LinearProgress,
  Slider,
  Tooltip,
  Typography,
  useTheme,
} from '@mui/material'
import {
  CheckCircle as ConnectedIcon,
  ExpandLess,
  ExpandMore,
  Favorite,
  FavoriteBorder,
  MoreVert,
  MusicNote as SpotifyIcon,
  Pause,
  PlayArrow,
  QueueMusic,
  Repeat,
  RepeatOne,
  Share,
  Shuffle,
  SkipNext,
  SkipPrevious,
  VolumeDown,
  VolumeOff,
  VolumeUp,
  Warning as WarningIcon,
} from '@mui/icons-material'
import {useMusic} from '../../context'
import {usePlaybackControl, useSpotifyPlayer} from '../../hooks'
import {useSpotify} from '../../context/useSpotifyContext'
import {SpotifyConnectButton} from '../spotify-connect'
import {MusicPlayerProps} from '../../types'

// Utility functions
const formatTime = (seconds: number): string => {
  const mins = Math.floor(seconds / 60)
  const secs = Math.floor(seconds % 60)
  return `${mins}:${secs.toString().padStart(2, '0')}`
}

const MusicPlayer: React.FC<MusicPlayerProps> = ({
                                                   mode = 'mini',
                                                   className,
                                                   showQueue = false,
                                                   showLyrics = false,
                                                 }) => {
  // TODO: Implement lyrics display feature
  void showLyrics; // Mark as intentionally used for future feature
  const theme = useTheme()
  const musicContext = useMusic()
  const spotify = useSpotify()
  const spotifyPlayer = useSpotifyPlayer()
  const {
    playWithCrossfade,
    pause,
    resume,
    skipNextEnhanced,
    skipPreviousEnhanced,
    quickSeekBackward,
    quickSeekForward,
    seekTo,
    setVolume,
    toggleMute,
    isShuffling,
    repeatMode,
    toggleShuffle,
    toggleRepeat,
  } = usePlaybackControl()

  const [isExpanded, setIsExpanded] = useState(mode === 'full')
  const [showVolumeSlider, setShowVolumeSlider] = useState(false)
  const [isFavorite, setIsFavorite] = useState(false)
  const [isDragging, setIsDragging] = useState(false)
  const [tempPosition, setTempPosition] = useState(0)
  const [showSpotifyConnect, setShowSpotifyConnect] = useState(false)

  const volumeTimeoutRef = useRef<NodeJS.Timeout>()

  const {currentTrack, playbackState, isLoading} = musicContext.state

  // Determine which playback source to use
  const isUsingSpotify = spotifyPlayer.isConnected && spotifyPlayer.isPremium
  const effectivePlaybackState = isUsingSpotify ? spotifyPlayer.playbackState : playbackState
  const effectiveCurrentTrack = isUsingSpotify ? spotifyPlayer.playbackState.currentTrack : currentTrack

  // Auto-expand for full mode
  useEffect(() => {
    if (mode === 'full') {
      setIsExpanded(true)
    }
  }, [mode])

  // Handle volume slider timeout
  useEffect(() => {
    return () => {
      if (volumeTimeoutRef.current) {
        clearTimeout(volumeTimeoutRef.current)
      }
    }
  }, [])

  const handlePlayPause = useCallback(async () => {
    try {
      if (isUsingSpotify) {
        await spotifyPlayer.togglePlay()
      } else {
        if (effectivePlaybackState?.isPlaying) {
          pause()
        } else if (effectivePlaybackState?.isPaused) {
          resume()
        } else if (effectiveCurrentTrack) {
          playWithCrossfade(effectiveCurrentTrack)
        }
      }
    } catch {
      // console.error('Failed to toggle playback:', error);
    }
  }, [isUsingSpotify, spotifyPlayer, effectivePlaybackState?.isPlaying, effectivePlaybackState?.isPaused, effectiveCurrentTrack, pause, resume, playWithCrossfade])

  const handleVolumeChange = useCallback(async (_: Event, newValue: number | number[]) => {
    const volume = Array.isArray(newValue) ? newValue[0] : newValue
    const normalizedVolume = volume / 100

    try {
      if (isUsingSpotify) {
        await spotifyPlayer.setVolume(normalizedVolume)
      } else {
        setVolume(normalizedVolume)
      }
    } catch {
      // console.error('Failed to set volume:', error);
    }
  }, [isUsingSpotify, spotifyPlayer, setVolume])

  const handleSkipNext = useCallback(async () => {
    try {
      if (isUsingSpotify) {
        await spotifyPlayer.skipNext()
      } else {
        skipNextEnhanced()
      }
    } catch {
      // console.error('Failed to skip next:', error);
    }
  }, [isUsingSpotify, spotifyPlayer, skipNextEnhanced])

  const handleSkipPrevious = useCallback(async () => {
    try {
      if (isUsingSpotify) {
        await spotifyPlayer.skipPrevious()
      } else {
        skipPreviousEnhanced()
      }
    } catch {
      // console.error('Failed to skip previous:', error);
    }
  }, [isUsingSpotify, spotifyPlayer, skipPreviousEnhanced])

  const handleVolumeHover = useCallback(() => {
    setShowVolumeSlider(true)
    if (volumeTimeoutRef.current) {
      clearTimeout(volumeTimeoutRef.current)
    }
  }, [])

  const handleVolumeLeave = useCallback(() => {
    volumeTimeoutRef.current = setTimeout(() => {
      setShowVolumeSlider(false)
    }, 2000)
  }, [])

  const handlePositionChange = useCallback((_: Event, newValue: number | number[]) => {
    const position = Array.isArray(newValue) ? newValue[0] : newValue
    setTempPosition(position)
  }, [])

  const handlePositionCommit = useCallback(async (_: Event, value: number | number[]) => {
    const position = Array.isArray(value) ? value[0] : value
    const timeInSeconds = (position / 100) * (effectivePlaybackState?.duration || 0)

    try {
      if (isUsingSpotify) {
        await spotifyPlayer.seekTo(timeInSeconds)
      } else {
        seekTo(timeInSeconds)
      }
    } catch {
      // console.error('Failed to seek:', error);
    }
    setIsDragging(false)
  }, [effectivePlaybackState?.duration, isUsingSpotify, spotifyPlayer, seekTo])

  const handlePositionMouseDown = useCallback(() => {
    setIsDragging(true)
  }, [])

  const handleToggleExpanded = useCallback(() => {
    if (mode !== 'full') {
      setIsExpanded(prev => !prev)
    }
  }, [mode])

  const handleToggleFavorite = useCallback(() => {
    setIsFavorite(prev => !prev)
    // TODO: Implement favorite functionality via API
  }, [])

  const currentPosition = useMemo(() => {
    if (isDragging) {
      return tempPosition || 0
    }
    const position = effectivePlaybackState?.currentTime || 0
    const duration = effectivePlaybackState?.duration || 0

    return duration > 0 && !isNaN(position) && !isNaN(duration)
        ? (position / duration) * 100
        : 0
  }, [isDragging, tempPosition, effectivePlaybackState?.currentTime, effectivePlaybackState?.duration])

  const repeatIcon = useMemo(() => {
    switch (repeatMode) {
      case 'one':
        return <RepeatOne/>
      case 'all':
        return <Repeat/>
      default:
        return <Repeat/>
    }
  }, [repeatMode])

  const volumeIcon = useMemo(() => {
    const volume = effectivePlaybackState?.volume || 0
    if (volume === 0 || isNaN(volume)) {
      return <VolumeOff/>
    } else if (volume < 0.5) {
      return <VolumeDown/>
    } else {
      return <VolumeUp/>
    }
  }, [effectivePlaybackState?.volume])

  // Spotify connection status component
  const _spotifyStatus = (): React.ReactElement | null => {
    if (!spotify.state.auth.isAuthenticated) {
      return (
          <Alert
              severity="info"
              action={
                <Button
                    color="inherit"
                    size="small"
                    onClick={() => setShowSpotifyConnect(true)}
                    startIcon={<SpotifyIcon/>}
                >
                  Connect
                </Button>
              }
          >
            Connect Spotify for enhanced music experience
          </Alert>
      )
    }

    if (spotify.state.auth.isAuthenticated && !spotify.state.auth.isPremium) {
      return (
          <Alert severity="warning" icon={<WarningIcon/>}>
            Spotify Premium required for full playback control
          </Alert>
      )
    }

    if (isUsingSpotify) {
      return (
          <Alert
              severity="success"
              icon={<ConnectedIcon/>}
              sx={{backgroundColor: alpha(theme.palette.success.main, 0.1)}}
          >
            Playing via Spotify Premium
          </Alert>
      )
    }

    return null
  }

  // Mini player render
  if (mode === 'mini' && !isExpanded) {
    return (
        <Card
            className={className}
            sx={{
              position: 'fixed',
              bottom: 16,
              right: 16,
              width: 320,
              boxShadow: theme.shadows[8],
              backgroundColor: alpha(theme.palette.background.paper, 0.95),
              backdropFilter: 'blur(10px)',
              borderRadius: 2,
              overflow: 'visible',
              zIndex: theme.zIndex.speedDial,
            }}
        >
          {(effectivePlaybackState.isBuffering || (isUsingSpotify && spotifyPlayer.isLoading)) && (
              <LinearProgress
                  sx={{
                    position: 'absolute',
                    top: 0,
                    left: 0,
                    right: 0,
                    borderRadius: '8px 8px 0 0'
                  }}
              />
          )}

          <CardContent sx={{p: 2, '&:last-child': {pb: 2}}}>
            <Box display="flex" alignItems="center" gap={1}>
              {/* Album Art */}
              <Avatar
                  src={effectiveCurrentTrack?.albumArt}
                  sx={{
                    width: 48,
                    height: 48,
                    borderRadius: 1,
                    bgcolor: theme.palette.primary.main
                  }}
              >
                {effectiveCurrentTrack?.title?.[0] || '♪'}
              </Avatar>

              {/* Track Info */}
              <Box flex={1} minWidth={0}>
                <Typography
                    variant="body2"
                    fontWeight="medium"
                    noWrap
                    sx={{cursor: 'pointer'}}
                    onClick={handleToggleExpanded}
                >
                  {effectiveCurrentTrack?.title || 'No track selected'}
                </Typography>
                <Typography
                    variant="caption"
                    color="text.secondary"
                    noWrap
                >
                  {effectiveCurrentTrack?.artist || 'Unknown artist'}
                </Typography>
                {isUsingSpotify && (
                    <Box display="flex" alignItems="center" gap={0.5} mt={0.5}>
                      <SpotifyIcon sx={{fontSize: 12, color: '#1DB954'}}/>
                      <Typography variant="caption" color="#1DB954">
                        Spotify
                      </Typography>
                    </Box>
                )}
              </Box>

              {/* Playback Controls */}
              <Box display="flex" alignItems="center" gap={0.5}>
                <Tooltip title="Previous">
                <span>
                  <IconButton
                      size="small"
                      onClick={handleSkipPrevious}
                      disabled={!effectiveCurrentTrack || (isUsingSpotify && !spotifyPlayer.playbackState.canSkipPrevious)}
                  >
                    <SkipPrevious/>
                  </IconButton>
                </span>
                </Tooltip>

                <Tooltip title={effectivePlaybackState.isPlaying ? 'Pause' : 'Play'}>
                <span>
                  <IconButton
                      onClick={handlePlayPause}
                      disabled={!effectiveCurrentTrack}
                      sx={{
                        backgroundColor: theme.palette.primary.main,
                        color: theme.palette.primary.contrastText,
                        '&:hover': {
                          backgroundColor: theme.palette.primary.dark,
                        },
                        '&:disabled': {
                          backgroundColor: theme.palette.action.disabledBackground,
                        }
                      }}
                  >
                    {effectivePlaybackState.isPlaying ? <Pause/> : <PlayArrow/>}
                  </IconButton>
                </span>
                </Tooltip>

                <Tooltip title="Next">
                <span>
                  <IconButton
                      size="small"
                      onClick={handleSkipNext}
                      disabled={!effectiveCurrentTrack || (isUsingSpotify && !spotifyPlayer.playbackState.canSkipNext)}
                  >
                    <SkipNext/>
                  </IconButton>
                </span>
                </Tooltip>

                <Tooltip title="Expand">
                  <IconButton
                      size="small"
                      onClick={handleToggleExpanded}
                  >
                    <ExpandMore/>
                  </IconButton>
                </Tooltip>
              </Box>
            </Box>

            {/* Progress Bar */}
            <Box mt={1}>
              <Slider
                  value={currentPosition}
                  onChange={handlePositionChange}
                  onChangeCommitted={handlePositionCommit}
                  onMouseDown={handlePositionMouseDown}
                  min={0}
                  max={100}
                  size="small"
                  aria-label="seek"
                  sx={{
                    color: theme.palette.primary.main,
                    height: 4,
                    '& .MuiSlider-thumb': {
                      width: 12,
                      height: 12,
                      '&:hover, &.Mui-focusVisible': {
                        boxShadow: `0px 0px 0px 8px ${alpha(theme.palette.primary.main, 0.16)}`,
                      },
                    },
                    '& .MuiSlider-rail': {
                      backgroundColor: theme.palette.divider,
                    },
                  }}
              />
            </Box>
          </CardContent>
        </Card>
    )
  }

  // Full/Expanded player render
  return (
      <Card
          className={className}
          sx={{
            width: mode === 'full' ? '100%' : 420,
            position: mode === 'mini' ? 'fixed' : 'relative',
            bottom: mode === 'mini' ? 16 : 'unset',
            right: mode === 'mini' ? 16 : 'unset',
            boxShadow: mode === 'mini' ? theme.shadows[12] : theme.shadows[2],
            backgroundColor: alpha(theme.palette.background.paper, 0.98),
            backdropFilter: mode === 'mini' ? 'blur(20px)' : 'none',
            borderRadius: 2,
            overflow: 'hidden',
            zIndex: mode === 'mini' ? theme.zIndex.speedDial : 'auto',
          }}
      >
        {(effectivePlaybackState.isBuffering || (isUsingSpotify && spotifyPlayer.isLoading)) && (
            <LinearProgress
                sx={{
                  position: 'absolute',
                  top: 0,
                  left: 0,
                  right: 0,
                  zIndex: 1
                }}
            />
        )}

        <CardContent sx={{p: 3}}>
          {/* Header with controls */}
          <Box display="flex" justifyContent="space-between" alignItems="center" mb={2}>
            <Typography variant="h6" fontWeight="bold">
              Now Playing
            </Typography>
            <Box display="flex" gap={1}>
              {mode === 'mini' && (
                  <Tooltip title="Collapse">
                    <IconButton size="small" onClick={handleToggleExpanded}>
                      <ExpandLess/>
                    </IconButton>
                  </Tooltip>
              )}
              <Tooltip title="Share">
                <IconButton size="small">
                  <Share/>
                </IconButton>
              </Tooltip>
              <Tooltip title="More options">
                <IconButton size="small">
                  <MoreVert/>
                </IconButton>
              </Tooltip>
            </Box>
          </Box>

          {/* Main content */}
          <Box display="flex" gap={3} mb={3}>
            {/* Album Art */}
            <Box position="relative">
              <Avatar
                  src={currentTrack?.albumArt}
                  sx={{
                    width: 120,
                    height: 120,
                    borderRadius: 2,
                    bgcolor: theme.palette.primary.main,
                    fontSize: '2rem'
                  }}
              >
                {currentTrack?.title?.[0] || '♪'}
              </Avatar>

              {/* Favorite button overlay */}
              <Fade in={!!currentTrack}>
                <IconButton
                    onClick={handleToggleFavorite}
                    sx={{
                      position: 'absolute',
                      top: 8,
                      right: 8,
                      backgroundColor: alpha(theme.palette.background.paper, 0.8),
                      '&:hover': {
                        backgroundColor: alpha(theme.palette.background.paper, 0.9),
                      },
                    }}
                    size="small"
                >
                  {isFavorite ? (
                      <Favorite color="error"/>
                  ) : (
                      <FavoriteBorder/>
                  )}
                </IconButton>
              </Fade>
            </Box>

            {/* Track Info */}
            <Box flex={1} minWidth={0}>
              <Typography
                  variant="h5"
                  fontWeight="bold"
                  gutterBottom
                  noWrap
              >
                {currentTrack?.title || 'No track selected'}
              </Typography>

              <Typography
                  variant="subtitle1"
                  color="text.secondary"
                  gutterBottom
                  noWrap
              >
                {currentTrack?.artist || 'Unknown artist'}
              </Typography>

              <Typography
                  variant="body2"
                  color="text.secondary"
                  gutterBottom
                  noWrap
              >
                {currentTrack?.album || 'Unknown album'}
              </Typography>

              {/* Tags/Chips */}
              <Box display="flex" gap={1} mt={1} flexWrap="wrap">
                {currentTrack?.explicit && (
                    <Chip
                        label="Explicit"
                        size="small"
                        variant="outlined"
                        color="warning"
                    />
                )}
                {currentTrack?.addedBy && (
                    <Chip
                        label={`Added by ${currentTrack.addedBy.name}`}
                        size="small"
                        variant="outlined"
                        avatar={
                          <Avatar
                              src={currentTrack.addedBy.avatar}
                              sx={{width: 16, height: 16}}
                          >
                            {currentTrack.addedBy.name[0]}
                          </Avatar>
                        }
                    />
                )}
              </Box>
            </Box>
          </Box>

          {/* Progress Section */}
          <Box mb={3}>
            <Box display="flex" justifyContent="space-between" alignItems="center" mb={1}>
              <Typography variant="caption" color="text.secondary">
                {formatTime(effectivePlaybackState.currentTime)}
              </Typography>
              <Typography variant="caption" color="text.secondary">
                {formatTime(effectivePlaybackState.duration)}
              </Typography>
            </Box>

            <Slider
                value={currentPosition}
                onChange={handlePositionChange}
                onChangeCommitted={handlePositionCommit}
                onMouseDown={handlePositionMouseDown}
                min={0}
                max={100}
                aria-label="seek"
                sx={{
                  color: theme.palette.primary.main,
                  height: 6,
                  '& .MuiSlider-thumb': {
                    width: 16,
                    height: 16,
                    '&:hover, &.Mui-focusVisible': {
                      boxShadow: `0px 0px 0px 8px ${alpha(theme.palette.primary.main, 0.16)}`,
                    },
                  },
                  '& .MuiSlider-rail': {
                    backgroundColor: theme.palette.divider,
                  },
                }}
            />
          </Box>

          {/* Main Controls */}
          <Box display="flex" justifyContent="center" alignItems="center" gap={1} mb={2}>
            <Tooltip title={isShuffling ? 'Shuffle on' : 'Shuffle off'}>
              <IconButton
                  onClick={toggleShuffle}
                  color={isShuffling ? 'primary' : 'default'}
              >
                <Shuffle/>
              </IconButton>
            </Tooltip>

            <Tooltip title="Previous">
            <span>
              <IconButton
                  onClick={skipPreviousEnhanced}
                  disabled={!currentTrack}
                  size="large"
              >
                <SkipPrevious/>
              </IconButton>
            </span>
            </Tooltip>

            <Tooltip title="Skip back 10s">
            <span>
              <IconButton
                  onClick={quickSeekBackward}
                  disabled={!currentTrack}
                  size="small"
              >
                <SkipPrevious fontSize="small"/>
              </IconButton>
            </span>
            </Tooltip>

            <Tooltip title={playbackState.isPlaying ? 'Pause' : 'Play'}>
            <span>
              <IconButton
                  onClick={handlePlayPause}
                  disabled={!currentTrack || isLoading}
                  sx={{
                    backgroundColor: theme.palette.primary.main,
                    color: theme.palette.primary.contrastText,
                    width: 56,
                    height: 56,
                    '&:hover': {
                      backgroundColor: theme.palette.primary.dark,
                    },
                    '&:disabled': {
                      backgroundColor: theme.palette.action.disabledBackground,
                    }
                  }}
              >
                {playbackState.isPlaying ? (
                    <Pause sx={{fontSize: '2rem'}}/>
                ) : (
                    <PlayArrow sx={{fontSize: '2rem'}}/>
                )}
              </IconButton>
            </span>
            </Tooltip>

            <Tooltip title="Skip forward 10s">
            <span>
              <IconButton
                  onClick={quickSeekForward}
                  disabled={!currentTrack}
                  size="small"
              >
                <SkipNext fontSize="small"/>
              </IconButton>
            </span>
            </Tooltip>

            <Tooltip title="Next">
            <span>
              <IconButton
                  onClick={skipNextEnhanced}
                  disabled={!currentTrack}
                  size="large"
              >
                <SkipNext/>
              </IconButton>
            </span>
            </Tooltip>

            <Tooltip title={`Repeat: ${repeatMode}`}>
              <IconButton
                  onClick={toggleRepeat}
                  color={repeatMode !== 'none' ? 'primary' : 'default'}
              >
                {repeatIcon}
              </IconButton>
            </Tooltip>
          </Box>

          {/* Secondary Controls */}
          <Box display="flex" justifyContent="space-between" alignItems="center">
            {/* Volume Control */}
            <Box
                display="flex"
                alignItems="center"
                gap={1}
                onMouseEnter={handleVolumeHover}
                onMouseLeave={handleVolumeLeave}
            >
              <Tooltip title="Volume">
                <IconButton onClick={toggleMute}>
                  {volumeIcon}
                </IconButton>
              </Tooltip>

              <Collapse in={showVolumeSlider} orientation="horizontal">
                <Box width={80}>
                  <Slider
                      value={(effectivePlaybackState?.volume || 0) * 100}
                      onChange={handleVolumeChange}
                      min={0}
                      max={100}
                      size="small"
                      aria-label="volume"
                      sx={{
                        color: theme.palette.primary.main,
                        '& .MuiSlider-thumb': {
                          width: 12,
                          height: 12,
                        },
                      }}
                  />
                </Box>
              </Collapse>
            </Box>

            {/* Queue Toggle */}
            {showQueue && (
                <Tooltip title="Show queue">
                  <IconButton>
                    <QueueMusic/>
                  </IconButton>
                </Tooltip>
            )}
          </Box>

          {/* Spotify Status */}
          <Box mt={2}>
            {_spotifyStatus()}
          </Box>

          {/* Spotify Connect Modal */}
          {showSpotifyConnect && (
              <Box
                  sx={{
                    position: 'fixed',
                    top: 0,
                    left: 0,
                    right: 0,
                    bottom: 0,
                    bgcolor: 'rgba(0, 0, 0, 0.5)',
                    display: 'flex',
                    alignItems: 'center',
                    justifyContent: 'center',
                    zIndex: theme.zIndex.modal,
                  }}
                  onClick={() => setShowSpotifyConnect(false)}
              >
                <Box onClick={(e) => e.stopPropagation()}>
                  <SpotifyConnectButton
                      variant="card"
                      showDetails={true}
                      onConnect={() => setShowSpotifyConnect(false)}
                      onDisconnect={() => setShowSpotifyConnect(false)}
                  />
                </Box>
              </Box>
          )}
        </CardContent>
      </Card>
  )
}

export default MusicPlayer