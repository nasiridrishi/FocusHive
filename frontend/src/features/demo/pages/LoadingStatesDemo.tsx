import React, { useState } from 'react'
import {
  Box,
  Typography,
  Paper,
  Button,
  
  Stack,
  Alert,
  TextField,
  FormControl,
  InputLabel,
  Select,
  MenuItem,
  Switch,
  FormControlLabel,
  Grid,
} from '@mui/material'

// Grid component type workaround
import {
  PlayArrow,
  Refresh,
  Save,
  Download,
  Upload,
  Add
} from '@mui/icons-material'
import {
  LoadingSpinner,
  LoadingBackdrop,
  LoadingSkeleton,
  LoadingButton,
  ContentSkeleton,
  TableSkeleton
} from '@shared/components/loading'
import { useAsync, useAsyncSubmit } from '@shared/hooks'

/**
 * Demo page showcasing all loading state components
 * and patterns available in FocusHive
 */
const LoadingStatesDemo: React.FC = () => {
  const [backdropOpen, setBackdropOpen] = useState(false)
  const [backdropProgress, setBackdropProgress] = useState<number | undefined>()
  const [backdropVariant, setBackdropVariant] = useState<'simple' | 'detailed'>('simple')
  const [skeletonLines, setSkeletonLines] = useState(3)
  const [skeletonAvatar, setSkeletonAvatar] = useState(false)
  const [skeletonActions, setSkeletonActions] = useState(false)
  const [contentType, setContentType] = useState<'card' | 'list' | 'form' | 'table' | 'chat' | 'hive'>('card')
  const [contentCount, setContentCount] = useState(3)

  // Demo async operations
  const dataFetch = useAsync(
    async () => {
      await new Promise(resolve => setTimeout(resolve, 2000))
      return { message: 'Data loaded successfully!' }
    }
  )

  const formSubmit = useAsyncSubmit(
    async (formData: unknown) => {
      await new Promise(resolve => setTimeout(resolve, 3000))
      console.log('Form submitted:', formData)
    }
  )

  const handleBackdropDemo = () => {
    setBackdropOpen(true)
    setTimeout(() => {
      setBackdropOpen(false)
      setBackdropProgress(undefined)
    }, 3000)
  }

  const handleProgressDemo = () => {
    setBackdropProgress(0)
    setBackdropOpen(true)
    
    const interval = setInterval(() => {
      setBackdropProgress(prev => {
        const next = (prev || 0) + 10
        if (next >= 100) {
          clearInterval(interval)
          setTimeout(() => {
            setBackdropOpen(false)
            setBackdropProgress(undefined)
          }, 500)
        }
        return next
      })
    }, 300)
  }

  const handleFormSubmit = (event: React.FormEvent) => {
    event.preventDefault()
    formSubmit.submit({ test: 'data' })
  }

  return (
    <Box sx={{ p: 3, maxWidth: 1200, mx: 'auto' }}>
      <Typography variant="h3" component="h1" gutterBottom>
        Loading States Demo
      </Typography>
      <Typography variant="body1" color="text.secondary" sx={{ mb: 4 }}>
        Comprehensive showcase of loading components and patterns used throughout FocusHive.
      </Typography>

      <Grid container spacing={4}>
        {/* Loading Spinners */}
        <Grid item>
          <Paper sx={{ p: 3, height: '100%' }}>
            <Typography variant="h5" gutterBottom>
              Loading Spinners
            </Typography>
            <Typography variant="body2" color="text.secondary" sx={{ mb: 3 }}>
              Flexible spinners for various loading scenarios
            </Typography>

            <Stack spacing={3}>
              <Box>
                <Typography variant="subtitle2" gutterBottom>
                  Basic Spinner
                </Typography>
                <LoadingSpinner size={24} />
              </Box>

              <Box>
                <Typography variant="subtitle2" gutterBottom>
                  Spinner with Text
                </Typography>
                <LoadingSpinner text="Loading data..." />
              </Box>

              <Box>
                <Typography variant="subtitle2" gutterBottom>
                  Inline Spinner
                </Typography>
                <Typography variant="body1">
                  Processing <LoadingSpinner inline size={16} /> please wait...
                </Typography>
              </Box>

              <Box>
                <Typography variant="subtitle2" gutterBottom>
                  Async Hook Integration
                </Typography>
                {dataFetch.isLoading ? (
                  <LoadingSpinner text="Fetching data..." />
                ) : dataFetch.data ? (
                  <Alert severity="success">{dataFetch.data.message}</Alert>
                ) : (
                  <Button onClick={() => dataFetch.execute()}>
                    Load Data
                  </Button>
                )}
              </Box>
            </Stack>
          </Paper>
        </Grid>

        {/* Loading Buttons */}
        <Grid item>
          <Paper sx={{ p: 3, height: '100%' }}>
            <Typography variant="h5" gutterBottom>
              Loading Buttons
            </Typography>
            <Typography variant="body2" color="text.secondary" sx={{ mb: 3 }}>
              Buttons with integrated loading states
            </Typography>

            <Stack spacing={2}>
              <LoadingButton
                variant="contained"
                startIcon={<Save />}
                loading={false}
              >
                Save
              </LoadingButton>

              <LoadingButton
                variant="contained"
                startIcon={<Download />}
                loading={true}
                loadingText="Downloading..."
              >
                Download
              </LoadingButton>

              <LoadingButton
                variant="outlined"
                startIcon={<Upload />}
                loading={true}
                loadingPosition="end"
              >
                Upload
              </LoadingButton>

              <form onSubmit={handleFormSubmit}>
                <LoadingButton
                  type="submit"
                  variant="contained"
                  color="primary"
                  loading={formSubmit.loading}
                  loadingText="Submitting..."
                  startIcon={<Add />}
                  fullWidth
                >
                  Submit Form
                </LoadingButton>
              </form>

              {formSubmit.error && (
                <Alert severity="error">{formSubmit.error}</Alert>
              )}
            </Stack>
          </Paper>
        </Grid>

        {/* Backdrop Loading */}
        <Grid item>
          <Paper sx={{ p: 3 }}>
            <Typography variant="h5" gutterBottom>
              Backdrop Loading
            </Typography>
            <Typography variant="body2" color="text.secondary" sx={{ mb: 3 }}>
              Full-screen loading overlays for major operations
            </Typography>

            <Grid container spacing={2}>
              <Grid item>
                <FormControl fullWidth size="small">
                  <InputLabel>Variant</InputLabel>
                  <Select
                    value={backdropVariant}
                    label="Variant"
                    onChange={(e) => setBackdropVariant(e.target.value as 'simple' | 'detailed')}
                  >
                    <MenuItem value="simple">Simple</MenuItem>
                    <MenuItem value="detailed">Detailed</MenuItem>
                  </Select>
                </FormControl>
              </Grid>
              <Grid item>
                <Button
                  variant="contained"
                  onClick={handleBackdropDemo}
                  disabled={backdropOpen}
                  fullWidth
                >
                  Show Backdrop
                </Button>
              </Grid>
              <Grid item>
                <Button
                  variant="outlined"
                  onClick={handleProgressDemo}
                  disabled={backdropOpen}
                  fullWidth
                >
                  Progress Demo
                </Button>
              </Grid>
            </Grid>
          </Paper>
        </Grid>

        {/* Skeleton Loading */}
        <Grid item>
          <Paper sx={{ p: 3, height: '100%' }}>
            <Typography variant="h5" gutterBottom>
              Skeleton Components
            </Typography>
            <Typography variant="body2" color="text.secondary" sx={{ mb: 3 }}>
              Placeholder loading states for content
            </Typography>

            <Stack spacing={2} sx={{ mb: 3 }}>
              <TextField
                label="Lines"
                type="number"
                value={skeletonLines}
                onChange={(e) => setSkeletonLines(Number(e.target.value))}
                size="small"
                inputProps={{ min: 1, max: 10 }}
              />
              <FormControlLabel
                control={
                  <Switch
                    checked={skeletonAvatar}
                    onChange={(e) => setSkeletonAvatar(e.target.checked)}
                  />
                }
                label="Show Avatar"
              />
              <FormControlLabel
                control={
                  <Switch
                    checked={skeletonActions}
                    onChange={(e) => setSkeletonActions(e.target.checked)}
                  />
                }
                label="Show Actions"
              />
            </Stack>

            <LoadingSkeleton
              lines={skeletonLines}
              avatar={skeletonAvatar}
              actions={skeletonActions}
              animation="wave"
            />
          </Paper>
        </Grid>

        {/* Content Skeletons */}
        <Grid item>
          <Paper sx={{ p: 3, height: '100%' }}>
            <Typography variant="h5" gutterBottom>
              Content Skeletons
            </Typography>
            <Typography variant="body2" color="text.secondary" sx={{ mb: 3 }}>
              Pre-built skeletons for specific content types
            </Typography>

            <Stack spacing={2} sx={{ mb: 3 }}>
              <FormControl fullWidth size="small">
                <InputLabel>Content Type</InputLabel>
                <Select
                  value={contentType}
                  label="Content Type"
                  onChange={(e) => setContentType(e.target.value as 'hive' | 'chat' | 'form' | 'table' | 'list' | 'card')}
                >
                  <MenuItem value="card">Card</MenuItem>
                  <MenuItem value="list">List</MenuItem>
                  <MenuItem value="form">Form</MenuItem>
                  <MenuItem value="table">Table</MenuItem>
                  <MenuItem value="chat">Chat</MenuItem>
                  <MenuItem value="hive">Hive</MenuItem>
                </Select>
              </FormControl>
              <TextField
                label="Count"
                type="number"
                value={contentCount}
                onChange={(e) => setContentCount(Number(e.target.value))}
                size="small"
                inputProps={{ min: 1, max: 5 }}
              />
            </Stack>

            <Box sx={{ maxHeight: 400, overflow: 'auto' }}>
              <ContentSkeleton
                type={contentType}
                count={contentCount}
                animation="wave"
              />
            </Box>
          </Paper>
        </Grid>

        {/* Table Skeleton */}
        <Grid item>
          <Paper sx={{ p: 3 }}>
            <Typography variant="h5" gutterBottom>
              Table Skeleton
            </Typography>
            <Typography variant="body2" color="text.secondary" sx={{ mb: 3 }}>
              Specialized skeleton for data tables
            </Typography>

            <Box sx={{ overflow: 'auto' }}>
              <TableSkeleton
                rows={5}
                columns={4}
                showHeader={true}
                showActions={true}
              />
            </Box>
          </Paper>
        </Grid>

        {/* API Integration Example */}
        <Grid item>
          <Paper sx={{ p: 3 }}>
            <Typography variant="h5" gutterBottom>
              Real API Integration Example
            </Typography>
            <Typography variant="body2" color="text.secondary" sx={{ mb: 3 }}>
              Example showing how loading states integrate with actual API calls using useAsync hooks
            </Typography>

            <Alert severity="info" sx={{ mb: 3 }}>
              This demonstrates the loading patterns you would use with actual API calls in FocusHive.
              Check the browser console for simulated API responses.
            </Alert>

            <Box sx={{ display: 'flex', gap: 2, flexWrap: 'wrap' }}>
              <LoadingButton
                variant="contained"
                startIcon={<PlayArrow />}
                loading={dataFetch.isLoading}
                onClick={() => dataFetch.execute()}
              >
                Fetch Data
              </LoadingButton>

              <LoadingButton
                variant="outlined"
                startIcon={<Refresh />}
                onClick={() => dataFetch.reset()}
                disabled={dataFetch.isLoading}
              >
                Reset
              </LoadingButton>
            </Box>

            {dataFetch.isLoading && (
              <Box sx={{ mt: 2 }}>
                <LoadingSpinner text="Fetching data from API..." />
              </Box>
            )}

            {dataFetch.error && (
              <Alert severity="error" sx={{ mt: 2 }}>
                Error: {dataFetch.error}
              </Alert>
            )}

            {dataFetch.data && (
              <Alert severity="success" sx={{ mt: 2 }}>
                Success: {dataFetch.data.message}
              </Alert>
            )}
          </Paper>
        </Grid>
      </Grid>

      {/* Backdrop Component */}
      <LoadingBackdrop
        open={backdropOpen}
        variant={backdropVariant}
        text="Processing your request..."
        progress={backdropProgress}
      />
    </Box>
  )
}

export default LoadingStatesDemo