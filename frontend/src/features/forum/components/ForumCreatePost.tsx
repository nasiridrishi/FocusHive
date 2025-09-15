import React, {useEffect, useState} from 'react'
import {
  Alert,
  Box,
  Breadcrumbs,
  Button,
  Card,
  CardContent,
  Chip,
  CircularProgress,
  Container,
  Divider,
  FormControl,
  FormControlLabel,
  Grid,
  IconButton,
  InputLabel,
  Link,
  MenuItem,
  Paper,
  Select,
  Switch,
  TextField,
  Typography,
} from '@mui/material'

// Grid component type workaround
import {
  Add as AddIcon,
  Article as ArticleIcon,
  Attachment as AttachmentIcon,
  Category as CategoryIcon,
  Delete as DeleteIcon,
  Home as HomeIcon,
  Preview as PreviewIcon,
  Save as SaveIcon
} from '@mui/icons-material'
import {Link as RouterLink, useNavigate, useParams} from 'react-router-dom'
import {forumApi} from '../services/forumApi'
import {ForumCategory, ForumCreatePostRequest} from '../types'

const ForumCreatePost: React.FC = () => {
  const navigate = useNavigate()
  const {categorySlug} = useParams<{ categorySlug?: string }>()

  const [loading, setLoading] = useState(false)
  const [categoriesLoading, setCategoriesLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)
  const [categories, setCategories] = useState<ForumCategory[]>([])
  const [selectedCategory, setSelectedCategory] = useState<ForumCategory | null>(null)
  const [previewMode, setPreviewMode] = useState(false)

  // Form data
  const [formData, setFormData] = useState({
    title: '',
    content: '',
    categoryId: 0 as number | string,
    tags: [] as string[],
    isPinned: false,
    isLocked: false
  })

  const [newTag, setNewTag] = useState('')
  const [attachments, setAttachments] = useState<File[]>([])

  const commonTags = [
    'Discussion', 'Question', 'Help', 'Tutorial', 'News', 'Announcement',
    'Beginner', 'Advanced', 'Tips', 'Resources', 'Bug Report', 'Feature Request',
    'Programming', 'Web Development', 'Mobile Development', 'Design',
    'Productivity', 'Study Tips', 'Career', 'Freelancing'
  ]

  useEffect(() => {
    loadCategories()
  }, [])

  useEffect(() => {
    if (categorySlug && categories.length > 0) {
      const category = categories.find(cat => cat.slug === categorySlug)
      if (category) {
        setSelectedCategory(category)
        setFormData(prev => ({...prev, categoryId: category.id}))
      }
    }
  }, [categorySlug, categories])

  const loadCategories = async () => {
    try {
      setCategoriesLoading(true)
      const categoriesData = await forumApi.getCategories()
      setCategories(categoriesData.filter(cat => !cat.isLocked)) // Only show unlocked categories
    } catch {
      // console.error('Error loading categories');
      setError('Failed to load categories')
    } finally {
      setCategoriesLoading(false)
    }
  }

  const handleInputChange = (field: keyof typeof formData) => (
      e: React.ChangeEvent<HTMLInputElement | HTMLTextAreaElement>
  ) => {
    setFormData(prev => ({
      ...prev,
      [field]: e.target.value
    }))
  }

  const handleCategoryChange = (categoryId: number | string): void => {
    const category = categories.find(cat => cat.id === categoryId)
    setSelectedCategory(category || null)
    setFormData(prev => ({...prev, categoryId}))
  }

  const handleAddTag = (): void => {
    const trimmedTag = newTag.trim()
    if (trimmedTag && !formData.tags.includes(trimmedTag)) {
      setFormData(prev => ({
        ...prev,
        tags: [...prev.tags, trimmedTag]
      }))
      setNewTag('')
    }
  }

  const handleRemoveTag = (tagToRemove: string): void => {
    setFormData(prev => ({
      ...prev,
      tags: prev.tags.filter(tag => tag !== tagToRemove)
    }))
  }

  const handleQuickAddTag = (tag: string): void => {
    if (!formData.tags.includes(tag)) {
      setFormData(prev => ({
        ...prev,
        tags: [...prev.tags, tag]
      }))
    }
  }

  const handleFileChange = (e: React.ChangeEvent<HTMLInputElement>): void => {
    const files = Array.from(e.target.files || [])
    setAttachments(prev => [...prev, ...files])
  }

  const handleRemoveAttachment = (index: number): void => {
    setAttachments(prev => prev.filter((_, i) => i !== index))
  }

  const validateForm = (): string | null => {
    if (!formData.title.trim()) {
      return 'Post title is required'
    }

    if (formData.title.trim().length < 5) {
      return 'Post title must be at least 5 characters long'
    }

    if (!formData.content.trim()) {
      return 'Post content is required'
    }

    if (formData.content.trim().length < 10) {
      return 'Post content must be at least 10 characters long'
    }

    if (!formData.categoryId || formData.categoryId === 0 || formData.categoryId === '0') {
      return 'Please select a category'
    }

    return null
  }

  const handleSubmit = async () => {
    const validationError = validateForm()
    if (validationError) {
      setError(validationError)
      return
    }

    setLoading(true)
    setError(null)

    try {
      const postData: ForumCreatePostRequest = {
        title: formData.title.trim(),
        content: formData.content.trim(),
        categoryId: formData.categoryId,
        tags: formData.tags.length > 0 ? formData.tags : undefined,
        isPinned: formData.isPinned,
        isLocked: formData.isLocked,
        attachments: attachments.length > 0 ? attachments : undefined
      }

      const newPost = await forumApi.createPost(postData)
      navigate(`/forum/posts/${newPost.slug}`)
    } catch (err) {
      const error = err as Error & { response?: { data?: { message?: string } } }
      setError(error.response?.data?.message || 'Failed to create post')
    } finally {
      setLoading(false)
    }
  }

  const handleCancel = (): void => {
    if (selectedCategory) {
      navigate(`/forum/categories/${selectedCategory.slug}`)
    } else {
      navigate('/forum')
    }
  }

  const formatFileSize = (bytes: number): string => {
    if (bytes === 0) return '0 Bytes'
    const k = 1024
    const sizes = ['Bytes', 'KB', 'MB', 'GB']
    const i = Math.floor(Math.log(bytes) / Math.log(k))
    return parseFloat((bytes / Math.pow(k, i)).toFixed(2)) + ' ' + sizes[i]
  }

  if (categoriesLoading) {
    return (
        <Container sx={{py: 4}}>
          <Box display="flex" justifyContent="center" alignItems="center" minHeight="400px">
            <CircularProgress/>
          </Box>
        </Container>
    )
  }

  return (
      <Container sx={{py: 4}}>
        {/* Breadcrumbs */}
        <Breadcrumbs sx={{mb: 3}}>
          <Link component={RouterLink} to="/forum" color="inherit">
            <Box display="flex" alignItems="center" gap={0.5}>
              <HomeIcon fontSize="small"/>
              Forum
            </Box>
          </Link>
          <Link component={RouterLink} to="/forum/categories" color="inherit">
            <Box display="flex" alignItems="center" gap={0.5}>
              <CategoryIcon fontSize="small"/>
              Categories
            </Box>
          </Link>
          {selectedCategory && (
              <Link component={RouterLink} to={`/forum/categories/${selectedCategory.slug}`}
                    color="inherit">
                {selectedCategory.name}
              </Link>
          )}
          <Typography color="textPrimary">
            Create New Post
          </Typography>
        </Breadcrumbs>

        <Paper sx={{p: 4}}>
          <Box display="flex" justifyContent="between" alignItems="center" mb={4}>
            <Typography variant="h4" gutterBottom
                        sx={{display: 'flex', alignItems: 'center', gap: 1}}>
              <ArticleIcon/>
              Create New Post
              {selectedCategory && (
                  <Chip label={selectedCategory.name} color="primary"/>
              )}
            </Typography>

            <Box display="flex" gap={1}>
              <Button
                  variant="outlined"
                  startIcon={<PreviewIcon/>}
                  onClick={() => setPreviewMode(!previewMode)}
              >
                {previewMode ? 'Edit' : 'Preview'}
              </Button>
            </Box>
          </Box>

          {error && (
              <Alert severity="error" sx={{mb: 3}} onClose={() => setError(null)}>
                {error}
              </Alert>
          )}

          <form onSubmit={(e) => {
            e.preventDefault();
            handleSubmit();
          }}>
            <Grid container spacing={3}>
              {/* Main Content */}
              <Grid item>
                {!previewMode ? (
                    <Box display="flex" flexDirection="column" gap={3}>
                      {/* Title */}
                      <TextField
                          label="Post Title"
                          value={formData.title}
                          onChange={handleInputChange('title')}
                          required
                          fullWidth
                          placeholder="Write a descriptive title for your post..."
                          helperText={`${formData.title.length}/100 characters`}
                          inputProps={{maxLength: 100}}
                      />

                      {/* Category Selection */}
                      <FormControl fullWidth required>
                        <InputLabel>Category</InputLabel>
                        <Select
                            value={formData.categoryId}
                            onChange={(e) => {
                              const value = e.target.value;
                              // Try to convert to number if it's a numeric string, otherwise keep as string
                              const categoryId = isNaN(Number(value)) ? value : Number(value);
                              handleCategoryChange(categoryId);
                            }}
                            label="Category"
                        >
                          <MenuItem value={0}>
                            <em>Select a category</em>
                          </MenuItem>
                          {categories.map((category) => (
                              <MenuItem key={category.id} value={category.id}>
                                {category.name} - {category.description}
                              </MenuItem>
                          ))}
                        </Select>
                      </FormControl>

                      {/* Content */}
                      <TextField
                          label="Post Content"
                          multiline
                          rows={12}
                          value={formData.content}
                          onChange={handleInputChange('content')}
                          required
                          fullWidth
                          placeholder="Write your post content here..."
                          helperText="Use Markdown formatting for rich text. Minimum 10 characters required."
                      />

                      {/* File Attachments */}
                      <Box>
                        <Typography variant="subtitle2" gutterBottom>
                          Attachments (optional)
                        </Typography>
                        <Button
                            variant="outlined"
                            component="label"
                            startIcon={<AttachmentIcon/>}
                            sx={{mb: 2}}
                        >
                          Add Files
                          <input
                              type="file"
                              hidden
                              multiple
                              accept="image/*,.pdf,.doc,.docx,.txt"
                              onChange={handleFileChange}
                          />
                        </Button>

                        {attachments.length > 0 && (
                            <Box>
                              {attachments.map((file, index) => (
                                  <Card key={index} variant="outlined" sx={{mb: 1}}>
                                    <CardContent sx={{py: 1}}>
                                      <Box display="flex" justifyContent="space-between"
                                           alignItems="center">
                                        <Box>
                                          <Typography variant="body2">
                                            {file.name}
                                          </Typography>
                                          <Typography variant="caption" color="textSecondary">
                                            {formatFileSize(file.size)}
                                          </Typography>
                                        </Box>
                                        <IconButton
                                            size="small"
                                            onClick={() => handleRemoveAttachment(index)}
                                        >
                                          <DeleteIcon/>
                                        </IconButton>
                                      </Box>
                                    </CardContent>
                                  </Card>
                              ))}
                            </Box>
                        )}
                      </Box>
                    </Box>
                ) : (
                    /* Preview Mode */
                    <Card>
                      <CardContent>
                        <Typography variant="h5" gutterBottom>
                          {formData.title || 'Your Post Title'}
                        </Typography>
                        <Typography
                            variant="body1"
                            sx={{
                              whiteSpace: 'pre-wrap',
                              mt: 2,
                              '& p': {mb: 1}
                            }}
                        >
                          {formData.content || 'Your post content will appear here...'}
                        </Typography>
                      </CardContent>
                    </Card>
                )}
              </Grid>

              {/* Sidebar */}
              <Grid item>
                <Box display="flex" flexDirection="column" gap={3}>
                  {/* Tags */}
                  <Card>
                    <CardContent>
                      <Typography variant="h6" gutterBottom>
                        Tags
                      </Typography>

                      {/* Current tags */}
                      {formData.tags.length > 0 && (
                          <Box display="flex" gap={0.5} flexWrap="wrap" mb={2}>
                            {formData.tags.map((tag) => (
                                <Chip
                                    key={tag}
                                    label={tag}
                                    size="small"
                                    onDelete={() => handleRemoveTag(tag)}
                                />
                            ))}
                          </Box>
                      )}

                      {/* Add new tag */}
                      <Box display="flex" gap={1} mb={2}>
                        <TextField
                            size="small"
                            placeholder="Add tag..."
                            value={newTag}
                            onChange={(e) => setNewTag(e.target.value)}
                            onKeyPress={(e) => e.key === 'Enter' && handleAddTag()}
                        />
                        <Button
                            size="small"
                            variant="outlined"
                            onClick={handleAddTag}
                            disabled={!newTag.trim()}
                        >
                          <AddIcon/>
                        </Button>
                      </Box>

                      {/* Common tags */}
                      <Typography variant="subtitle2" color="textSecondary" gutterBottom>
                        Suggested tags:
                      </Typography>
                      <Box display="flex" gap={0.5} flexWrap="wrap">
                        {commonTags
                        .filter(tag => !formData.tags.includes(tag))
                        .slice(0, 10)
                        .map((tag) => (
                            <Chip
                                key={tag}
                                label={tag}
                                size="small"
                                variant="outlined"
                                clickable
                                onClick={() => handleQuickAddTag(tag)}
                            />
                        ))}
                      </Box>
                    </CardContent>
                  </Card>

                  {/* Post Options */}
                  <Card>
                    <CardContent>
                      <Typography variant="h6" gutterBottom>
                        Post Options
                      </Typography>

                      <FormControlLabel
                          control={
                            <Switch
                                checked={formData.isPinned}
                                onChange={(e) => setFormData(prev => ({
                                  ...prev,
                                  isPinned: e.target.checked
                                }))}
                            />
                          }
                          label="Pin this post"
                      />

                      <FormControlLabel
                          control={
                            <Switch
                                checked={formData.isLocked}
                                onChange={(e) => setFormData(prev => ({
                                  ...prev,
                                  isLocked: e.target.checked
                                }))}
                            />
                          }
                          label="Lock comments"
                      />
                    </CardContent>
                  </Card>

                  {/* Category Info */}
                  {selectedCategory && (
                      <Card>
                        <CardContent>
                          <Typography variant="h6" gutterBottom>
                            Category: {selectedCategory.name}
                          </Typography>
                          <Typography variant="body2" color="textSecondary">
                            {selectedCategory.description}
                          </Typography>
                        </CardContent>
                      </Card>
                  )}
                </Box>
              </Grid>
            </Grid>

            {/* Action Buttons */}
            <Divider sx={{my: 4}}/>
            <Box display="flex" justifyContent="end" gap={2}>
              <Button
                  variant="outlined"
                  onClick={handleCancel}
                  disabled={loading}
              >
                Cancel
              </Button>
              <Button
                  type="submit"
                  variant="contained"
                  startIcon={loading ? <CircularProgress size={20}/> : <SaveIcon/>}
                  disabled={loading}
              >
                {loading ? 'Creating...' : 'Create Post'}
              </Button>
            </Box>
          </form>
        </Paper>
      </Container>
  )
}

export default ForumCreatePost