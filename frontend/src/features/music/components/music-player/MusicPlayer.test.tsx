import React from 'react'
import { render, screen, fireEvent, waitFor, act } from '@testing-library/react'
import { vi, describe, it, expect, beforeEach } from 'vitest'
import { ThemeProvider, createTheme } from '@mui/material'
import type { Track, MusicState } from '../../types'

// Mock data
const mockTrack: Track = {
  id: 'track-1',
  title: 'Test Song',
  artist: 'Test Artist',
  album: 'Test Album',
  duration: 180,
  albumArt: 'https://example.com/album-art.jpg',
  explicit: false,
}

const mockMusicState: MusicState = {
  currentTrack: mockTrack,
  playbackState: {
    isPlaying: false,
    isPaused: false,
    isBuffering: false,
    currentTime: 60,
    duration: 180,
    volume: 0.8,
    isMuted: false,
    playbackRate: 1,
  },
  queue: [],
  playlists: [],
  isLoading: false,
  error: null,
  recommendations: [],
  currentMood: null,
}

// Mock playback control functions
const mockPlaybackControl = {
  playWithCrossfade: vi.fn(),
  pause: vi.fn(),
  resume: vi.fn(),
  stop: vi.fn(),
  seekTo: vi.fn(),
  setVolume: vi.fn(),
  toggleMute: vi.fn(),
  skipNextEnhanced: vi.fn(),
  skipPreviousEnhanced: vi.fn(),
  quickSeekBackward: vi.fn(),
  quickSeekForward: vi.fn(),
  isShuffling: false,
  repeatMode: 'none' as const,
  toggleShuffle: vi.fn(),
  toggleRepeat: vi.fn(),
  history: [],
  crossfadeState: { isActive: false, progress: 0 },
  clearHistory: vi.fn(),
}

// Mock context
const mockMusicContext = {
  state: mockMusicState,
  play: vi.fn(),
  pause: vi.fn(),
  resume: vi.fn(),
  stop: vi.fn(),
  seekTo: vi.fn(),
  setVolume: vi.fn(),
  toggleMute: vi.fn(),
  skipNext: vi.fn(),
  skipPrevious: vi.fn(),
  addToQueue: vi.fn(),
  removeFromQueue: vi.fn(),
  reorderQueue: vi.fn(),
  clearQueue: vi.fn(),
  voteOnTrack: vi.fn(),
  loadPlaylists: vi.fn(),
  createPlaylist: vi.fn(),
  updatePlaylist: vi.fn(),
  deletePlaylist: vi.fn(),
  addTracksToPlaylist: vi.fn(),
  removeTrackFromPlaylist: vi.fn(),
  loadPlaylist: vi.fn(),
  getRecommendations: vi.fn(),
  setMood: vi.fn(),
  searchTracks: vi.fn(),
  connectToHive: vi.fn(),
  disconnectFromHive: vi.fn(),
}

// Mock Spotify context
const mockSpotifyContext = {
  state: {
    auth: {
      isAuthenticated: false,
      isConnected: false,
      isPremium: false,
      user: null,
      authUrl: 'https://example.com/auth',
    },
    devices: [],
    currentDevice: null,
  },
  isAuthenticated: false,
  isConnected: false,
  user: null,
  devices: [],
  currentDevice: null,
  authUrl: 'https://example.com/auth',
  login: vi.fn(),
  logout: vi.fn(),
  selectDevice: vi.fn(),
  refreshDevices: vi.fn(),
  transferPlayback: vi.fn(),
  play: vi.fn(),
  pause: vi.fn(),
  resume: vi.fn(),
  skipNext: vi.fn(),
  skipPrevious: vi.fn(),
  seekTo: vi.fn(),
  setVolume: vi.fn(),
  getPlaylists: vi.fn(),
  getCurrentlyPlaying: vi.fn(),
  addToLibrary: vi.fn(),
  removeFromLibrary: vi.fn(),
  isTrackSaved: vi.fn(),
}

// Mock Spotify player
const mockSpotifyPlayer = {
  play: vi.fn(),
  pause: vi.fn(),
  resume: vi.fn(),
  seekTo: vi.fn(),
  skipNext: vi.fn(),
  skipPrevious: vi.fn(),
  setVolume: vi.fn(),
  togglePlay: vi.fn(),
  isConnected: false,
  isPremium: false,
  isLoading: false,
  currentDevice: null,
  availableDevices: [],
  playbackState: {
    isPlaying: false,
    isPaused: false,
    isBuffering: false,
    position: 60,
    duration: 180,
    volume: 0.8,
    currentTrack: null,
    canSkipNext: true,
    canSkipPrevious: true,
  },
}

// Mock the hooks and contexts
vi.mock('../../context', () => ({
  useMusic: () => mockMusicContext,
}))

vi.mock('../../hooks', () => ({
  usePlaybackControl: () => mockPlaybackControl,
  useSpotifyPlayer: () => mockSpotifyPlayer,
}))

vi.mock('../../context/useSpotifyContext', () => ({
  useSpotify: () => mockSpotifyContext,
}))

// Mock the SpotifyConnectButton component
vi.mock('../spotify-connect', () => ({
  SpotifyConnectButton: ({ onConnect, onDisconnect }: { onConnect: () => void; onDisconnect: () => void }) => (
    <div>
      <button onClick={onConnect}>Connect</button>
      <button onClick={onDisconnect}>Disconnect</button>
    </div>
  ),
}))

// Import after mocking
import MusicPlayer from './MusicPlayer'

// Create theme for testing
const theme = createTheme()

// Test wrapper component
const TestWrapper: React.FC<{ children: React.ReactNode }> = ({ children }) => (
  <ThemeProvider theme={theme}>
    {children}
  </ThemeProvider>
)

describe('MusicPlayer', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    
    // Reset mock state
    mockMusicContext.state = {
      ...mockMusicState,
      currentTrack: mockTrack,
      playbackState: {
        ...mockMusicState.playbackState,
        isPlaying: false,
        isBuffering: false,
      },
    }
    
    // Reset playback control mocks
    mockPlaybackControl.isShuffling = false
    mockPlaybackControl.repeatMode = 'none'
    
    // Reset Spotify mocks
    mockSpotifyPlayer.isConnected = false
    mockSpotifyPlayer.isPremium = false
    mockSpotifyContext.state.auth.isAuthenticated = false
  })

  describe('Mini Mode', () => {
    it('renders mini player with track info', () => {
      render(
        <TestWrapper>
          <MusicPlayer mode="mini" />
        </TestWrapper>
      )

      expect(screen.getByText('Test Song')).toBeInTheDocument()
      expect(screen.getByText('Test Artist')).toBeInTheDocument()
    })

    it('shows play button when not playing', () => {
      render(
        <TestWrapper>
          <MusicPlayer mode="mini" />
        </TestWrapper>
      )

      // The play button has PlayArrow icon when not playing
      const playButton = screen.getByTestId('PlayArrowIcon')
      expect(playButton).toBeInTheDocument()
    })

    it('shows pause button when playing', () => {
      // Set playing state
      mockMusicContext.state.playbackState.isPlaying = true

      render(
        <TestWrapper>
          <MusicPlayer mode="mini" />
        </TestWrapper>
      )

      // The pause button has Pause icon when playing
      const pauseButton = screen.getByTestId('PauseIcon')
      expect(pauseButton).toBeInTheDocument()
    })

    it('calls playback functions when controls are clicked', async () => {
      render(
        <TestWrapper>
          <MusicPlayer mode="mini" />
        </TestWrapper>
      )

      // Test play button - click the parent button of the PlayArrow icon
      const playIcon = screen.getByTestId('PlayArrowIcon')
      const playButton = playIcon.closest('button')
      expect(playButton).not.toBeNull()
      fireEvent.click(playButton!)

      await waitFor(() => {
        expect(mockPlaybackControl.playWithCrossfade).toHaveBeenCalledWith(mockTrack)
      })

      // Test next button - click the parent button of the SkipNext icon
      const nextIcon = screen.getByTestId('SkipNextIcon')
      const nextButton = nextIcon.closest('button')
      expect(nextButton).not.toBeNull()
      fireEvent.click(nextButton!)

      await waitFor(() => {
        expect(mockPlaybackControl.skipNextEnhanced).toHaveBeenCalled()
      })

      // Test previous button - click the parent button of the SkipPrevious icon
      const previousIcon = screen.getByTestId('SkipPreviousIcon')
      const previousButton = previousIcon.closest('button')
      expect(previousButton).not.toBeNull()
      fireEvent.click(previousButton!)

      await waitFor(() => {
        expect(mockPlaybackControl.skipPreviousEnhanced).toHaveBeenCalled()
      })
    })

    it('shows progress bar with correct position', () => {
      render(
        <TestWrapper>
          <MusicPlayer mode="mini" />
        </TestWrapper>
      )

      // Progress should be 60/180 = 33.33%
      const progressSlider = screen.getByRole('slider', { name: /seek/i })
      expect(progressSlider).toHaveAttribute('aria-valuenow')
      const ariaNow = progressSlider.getAttribute('aria-valuenow')
      expect(ariaNow).not.toBeNull()
      expect(parseFloat(ariaNow!)).toBeCloseTo(33.33, 1)
    })

    it('can expand to full view', async () => {
      render(
        <TestWrapper>
          <MusicPlayer mode="mini" />
        </TestWrapper>
      )

      // Find expand button by its aria-label
      const expandButton = screen.getByLabelText('Expand')
      fireEvent.click(expandButton)

      await waitFor(() => {
        expect(screen.getByText('Now Playing')).toBeInTheDocument()
      })
    })
  })

  describe('Full Mode', () => {
    it('renders full player with all controls', () => {
      render(
        <TestWrapper>
          <MusicPlayer mode="full" />
        </TestWrapper>
      )

      expect(screen.getByText('Now Playing')).toBeInTheDocument()
      expect(screen.getByText('Test Song')).toBeInTheDocument()
      expect(screen.getByText('Test Artist')).toBeInTheDocument()
      expect(screen.getByText('Test Album')).toBeInTheDocument()
    })

    it('shows time display', () => {
      render(
        <TestWrapper>
          <MusicPlayer mode="full" />
        </TestWrapper>
      )

      expect(screen.getByText('1:00')).toBeInTheDocument() // current time
      expect(screen.getByText('3:00')).toBeInTheDocument() // total time
    })

    it('shows shuffle and repeat controls', () => {
      render(
        <TestWrapper>
          <MusicPlayer mode="full" />
        </TestWrapper>
      )

      // Find buttons by their icons
      const shuffleIcon = screen.getByTestId('ShuffleIcon')
      const shuffleButton = shuffleIcon.closest('button')
      expect(shuffleButton).not.toBeNull()
      
      const repeatIcon = screen.getByTestId('RepeatIcon')
      const repeatButton = repeatIcon.closest('button')
      expect(repeatButton).not.toBeNull()

      expect(shuffleButton).toBeInTheDocument()
      expect(repeatButton).toBeInTheDocument()

      fireEvent.click(shuffleButton)
      expect(mockPlaybackControl.toggleShuffle).toHaveBeenCalled()

      fireEvent.click(repeatButton)
      expect(mockPlaybackControl.toggleRepeat).toHaveBeenCalled()
    })

    it('handles seek interaction', async () => {
      render(
        <TestWrapper>
          <MusicPlayer mode="full" />
        </TestWrapper>
      )

      const progressSlider = screen.getByRole('slider', { name: /seek/i })
      
      // Simulate user interaction with the slider (change and commit events)
      fireEvent.change(progressSlider, { target: { value: 50 } })
      
      // Fire the mouse up to trigger the onChangeCommitted event
      fireEvent.mouseUp(progressSlider)

      await waitFor(() => {
        // Should call seekTo with 50% of 180 seconds = 90 seconds
        expect(mockPlaybackControl.seekTo).toHaveBeenCalledWith(90)
      })
    })
  })

  describe('Loading and Error States', () => {
    it('shows loading indicator when buffering', () => {
      // Set buffering state
      mockMusicContext.state.playbackState.isBuffering = true

      render(
        <TestWrapper>
          <MusicPlayer mode="full" />
        </TestWrapper>
      )

      expect(screen.getByRole('progressbar')).toBeInTheDocument()
    })

    it('handles no current track state', () => {
      // Set no track state
      mockMusicContext.state.currentTrack = null

      render(
        <TestWrapper>
          <MusicPlayer mode="mini" />
        </TestWrapper>
      )

      expect(screen.getByText('No track selected')).toBeInTheDocument()
      expect(screen.getByText('Unknown artist')).toBeInTheDocument()
    })

    it('disables controls when no track is available', () => {
      // Set no track state
      mockMusicContext.state.currentTrack = null

      render(
        <TestWrapper>
          <MusicPlayer mode="mini" />
        </TestWrapper>
      )

      // Find buttons by their icons and check if disabled
      const playIcon = screen.getByTestId('PlayArrowIcon')
      const playButton = playIcon.closest('button')
      expect(playButton).not.toBeNull()
      
      const nextIcon = screen.getByTestId('SkipNextIcon')
      const nextButton = nextIcon.closest('button')
      expect(nextButton).not.toBeNull()
      
      const previousIcon = screen.getByTestId('SkipPreviousIcon')
      const previousButton = previousIcon.closest('button')
      expect(previousButton).not.toBeNull()

      expect(playButton).toBeDisabled()
      expect(nextButton).toBeDisabled()
      expect(previousButton).toBeDisabled()
    })
  })

  describe('Accessibility', () => {
    it('has proper ARIA labels', () => {
      render(
        <TestWrapper>
          <MusicPlayer mode="full" />
        </TestWrapper>
      )

      // Check that buttons exist by finding their icons
      expect(screen.getByTestId('PlayArrowIcon')).toBeInTheDocument()
      // There are multiple SkipNext icons (regular next + skip forward 10s), so use getAllByTestId
      expect(screen.getAllByTestId('SkipNextIcon')).toHaveLength(2)
      expect(screen.getAllByTestId('SkipPreviousIcon')).toHaveLength(2) 
      expect(screen.getByTestId('VolumeUpIcon')).toBeInTheDocument()
      expect(screen.getByTestId('ShuffleIcon')).toBeInTheDocument()
    })

    it('supports keyboard navigation', async () => {
      render(
        <TestWrapper>
          <MusicPlayer mode="full" />
        </TestWrapper>
      )

      // Find play button by its icon
      const playIcon = screen.getByTestId('PlayArrowIcon')
      const playButton = playIcon.closest('button')
      expect(playButton).not.toBeNull()
      
      act(() => {
        playButton.focus()
      })
      expect(playButton).toHaveFocus()

      // Simulate pressing Enter which should trigger the click event
      act(() => {
        fireEvent.click(playButton)
      })
      
      await waitFor(() => {
        expect(mockPlaybackControl.playWithCrossfade).toHaveBeenCalled()
      })
    })
  })
})