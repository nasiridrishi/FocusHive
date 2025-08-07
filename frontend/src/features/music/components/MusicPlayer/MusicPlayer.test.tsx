import React from 'react'
import { render, screen, fireEvent, waitFor } from '@testing-library/react'
import { vi, describe, it, expect, beforeEach, afterEach } from 'vitest'
import { ThemeProvider, createTheme } from '@mui/material'
import MusicPlayer from './MusicPlayer'
import { MusicProvider } from '../../context'
import * as musicContextModule from '../../context'
import * as playbackControlModule from '../../hooks'

// Mock the contexts and hooks
const mockMusicState = {
  currentTrack: {
    id: 'track-1',
    title: 'Test Song',
    artist: 'Test Artist',
    album: 'Test Album',
    duration: 180,
    albumArt: 'https://example.com/album-art.jpg',
    explicit: false,
  },
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
  play: vi.fn(),
  skipNext: vi.fn(),
  skipPrevious: vi.fn(),
}

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

// Mock the hooks
vi.mock('../../context', () => ({
  useMusic: () => mockMusicContext,
  MusicProvider: ({ children }: { children: React.ReactNode }) => <div>{children}</div>,
}))

vi.mock('../../hooks', () => ({
  usePlaybackControl: () => mockPlaybackControl,
}))

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
  })

  afterEach(() => {
    vi.clearAllMocks()
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

      const playButton = screen.getByLabelText(/play/i)
      expect(playButton).toBeInTheDocument()
    })

    it('shows pause button when playing', () => {
      const playingState = {
        ...mockMusicState,
        playbackState: {
          ...mockMusicState.playbackState,
          isPlaying: true,
        },
      }

      vi.mocked(musicContextModule.useMusic).mockReturnValue({
        ...mockMusicContext,
        state: playingState,
      })

      render(
        <TestWrapper>
          <MusicPlayer mode="mini" />
        </TestWrapper>
      )

      const pauseButton = screen.getByLabelText(/pause/i)
      expect(pauseButton).toBeInTheDocument()
    })

    it('calls playback functions when controls are clicked', async () => {
      render(
        <TestWrapper>
          <MusicPlayer mode="mini" />
        </TestWrapper>
      )

      // Test play/pause
      const playButton = screen.getByLabelText(/play/i)
      fireEvent.click(playButton)

      await waitFor(() => {
        expect(mockPlaybackControl.playWithCrossfade).toHaveBeenCalledWith(mockMusicState.currentTrack)
      })

      // Test next
      const nextButton = screen.getByLabelText(/next/i)
      fireEvent.click(nextButton)

      await waitFor(() => {
        expect(mockPlaybackControl.skipNextEnhanced).toHaveBeenCalled()
      })

      // Test previous
      const previousButton = screen.getByLabelText(/previous/i)
      fireEvent.click(previousButton)

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
      const progressSlider = screen.getByRole('slider')
      expect(progressSlider).toHaveAttribute('aria-valuenow', '33.333333333333336')
    })

    it('can expand to full view', async () => {
      render(
        <TestWrapper>
          <MusicPlayer mode="mini" />
        </TestWrapper>
      )

      const expandButton = screen.getByLabelText(/expand/i)
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

      const shuffleButton = screen.getByLabelText(/shuffle off/i)
      const repeatButton = screen.getByLabelText(/repeat: none/i)

      expect(shuffleButton).toBeInTheDocument()
      expect(repeatButton).toBeInTheDocument()

      fireEvent.click(shuffleButton)
      expect(mockPlaybackControl.toggleShuffle).toHaveBeenCalled()

      fireEvent.click(repeatButton)
      expect(mockPlaybackControl.toggleRepeat).toHaveBeenCalled()
    })

    it('shows volume control on hover', async () => {
      render(
        <TestWrapper>
          <MusicPlayer mode="full" />
        </TestWrapper>
      )

      const volumeButton = screen.getByLabelText(/volume/i)
      
      // Hover over volume area
      fireEvent.mouseEnter(volumeButton.parentElement!)

      await waitFor(() => {
        const volumeSlider = screen.getByRole('slider', { name: /volume/i })
        expect(volumeSlider).toBeVisible()
      })
    })

    it('handles seek interaction', async () => {
      render(
        <TestWrapper>
          <MusicPlayer mode="full" />
        </TestWrapper>
      )

      const progressSlider = screen.getAllByRole('slider')[0] // First slider is progress
      
      fireEvent.mouseDown(progressSlider)
      fireEvent.change(progressSlider, { target: { value: 50 } })

      await waitFor(() => {
        // Should call seekTo with 50% of 180 seconds = 90 seconds
        expect(mockPlaybackControl.seekTo).toHaveBeenCalledWith(90)
      })
    })
  })

  describe('Loading and Error States', () => {
    it('shows loading indicator when buffering', () => {
      const bufferingState = {
        ...mockMusicState,
        playbackState: {
          ...mockMusicState.playbackState,
          isBuffering: true,
        },
      }

      vi.mocked(musicContextModule.useMusic).mockReturnValue({
        ...mockMusicContext,
        state: bufferingState,
      })

      render(
        <TestWrapper>
          <MusicPlayer mode="full" />
        </TestWrapper>
      )

      expect(screen.getByRole('progressbar')).toBeInTheDocument()
    })

    it('handles no current track state', () => {
      const noTrackState = {
        ...mockMusicState,
        currentTrack: null,
      }

      vi.mocked(musicContextModule.useMusic).mockReturnValue({
        ...mockMusicContext,
        state: noTrackState,
      })

      render(
        <TestWrapper>
          <MusicPlayer mode="mini" />
        </TestWrapper>
      )

      expect(screen.getByText('No track selected')).toBeInTheDocument()
      expect(screen.getByText('Unknown artist')).toBeInTheDocument()
    })

    it('disables controls when no track is available', () => {
      const noTrackState = {
        ...mockMusicState,
        currentTrack: null,
      }

      vi.mocked(musicContextModule.useMusic).mockReturnValue({
        ...mockMusicContext,
        state: noTrackState,
      })

      render(
        <TestWrapper>
          <MusicPlayer mode="mini" />
        </TestWrapper>
      )

      const playButton = screen.getByLabelText(/play/i)
      const nextButton = screen.getByLabelText(/next/i)
      const previousButton = screen.getByLabelText(/previous/i)

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

      expect(screen.getByLabelText(/play/i)).toBeInTheDocument()
      expect(screen.getByLabelText(/next/i)).toBeInTheDocument()
      expect(screen.getByLabelText(/previous/i)).toBeInTheDocument()
      expect(screen.getByLabelText(/volume/i)).toBeInTheDocument()
      expect(screen.getByLabelText(/shuffle off/i)).toBeInTheDocument()
    })

    it('supports keyboard navigation', () => {
      render(
        <TestWrapper>
          <MusicPlayer mode="full" />
        </TestWrapper>
      )

      const playButton = screen.getByLabelText(/play/i)
      
      playButton.focus()
      expect(playButton).toHaveFocus()

      fireEvent.keyDown(playButton, { key: 'Enter' })
      expect(mockPlaybackControl.playWithCrossfade).toHaveBeenCalled()
    })
  })
})