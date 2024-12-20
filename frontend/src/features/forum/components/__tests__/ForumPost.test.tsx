import React from 'react'
import { describe, expect, it, vi, beforeEach } from 'vitest'
import { screen, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { renderWithProviders } from '../../../../test-utils/test-utils'
import ForumPost from '../ForumPost'
import { ForumPost as ForumPostType } from '../../types'

const mockAuthor = {
  id: 1,
  username: 'testuser',
  avatar: '/avatars/testuser.jpg',
  role: 'USER' as const,
  joinDate: '2024-01-01T00:00:00Z',
  postCount: 25,
  reputation: 150,
  badges: ['Early Adopter', 'Helpful']
}

const mockCategory = {
  id: 1,
  name: 'General Discussion',
  slug: 'general',
  description: 'General discussion topics',
  postCount: 100,
  topicCount: 50,
  icon: 'forum',
  color: '#1976d2',
  isLocked: false,
  isPrivate: false,
  order: 1,
  createdAt: '2024-01-01T00:00:00Z',
  updatedAt: '2024-01-01T00:00:00Z'
}

const mockPost: ForumPostType = {
  id: 1,
  title: 'How to improve focus during long study sessions?',
  content: '<p>Looking for tips and strategies to maintain focus during extended study sessions. What techniques work best for you?</p>',
  slug: 'how-to-improve-focus-study-sessions',
  categoryId: 1,
  category: mockCategory,
  authorId: 1,
  author: mockAuthor,
  isPinned: false,
  isLocked: false,
  isHidden: false,
  viewCount: 125,
  replyCount: 8,
  likeCount: 15,
  dislikeCount: 2,
  lastReplyAt: '2024-01-15T14:30:00Z',
  tags: ['study-tips', 'focus', 'productivity'],
  attachments: [],
  createdAt: '2024-01-10T09:15:00Z',
  updatedAt: '2024-01-10T09:15:00Z'
}

describe('ForumPost', () => {
  const defaultProps = {
    post: mockPost,
    currentUserId: 2,
    onVote: vi.fn(),
    onEdit: vi.fn(),
    onShare: vi.fn(),
    onView: vi.fn(),
  }

  beforeEach(() => {
    vi.clearAllMocks()
  })

  describe('Rendering', () => {
    it('should render forum post container', () => {
      renderWithProviders(<ForumPost {...defaultProps} />)

      expect(screen.getByTestId('forum-post')).toBeInTheDocument()
    })

    it('should display post title', () => {
      renderWithProviders(<ForumPost {...defaultProps} />)

      expect(screen.getByText('How to improve focus during long study sessions?')).toBeInTheDocument()
    })

    it('should display post content', () => {
      renderWithProviders(<ForumPost {...defaultProps} />)

      expect(screen.getByText(/Looking for tips and strategies to maintain focus/)).toBeInTheDocument()
    })

    it('should display view count', () => {
      renderWithProviders(<ForumPost {...defaultProps} />)

      expect(screen.getByTestId('view-count')).toHaveTextContent('125')
    })

    it('should display category chip', () => {
      renderWithProviders(<ForumPost {...defaultProps} />)

      expect(screen.getByTestId('category-chip')).toHaveTextContent('General Discussion')
    })

    it('should display creation timestamp', () => {
      renderWithProviders(<ForumPost {...defaultProps} />)

      expect(screen.getByTestId('post-timestamp')).toBeInTheDocument()
    })

    it('should display tags when provided', () => {
      renderWithProviders(<ForumPost {...defaultProps} />)

      const tagsContainer = screen.getByTestId('post-tags')
      expect(within(tagsContainer).getByText('study-tips')).toBeInTheDocument()
      expect(within(tagsContainer).getByText('focus')).toBeInTheDocument()
      expect(within(tagsContainer).getByText('productivity')).toBeInTheDocument()
    })

    it('should hide tags container when no tags provided', () => {
      const postWithoutTags = { ...mockPost, tags: [] }
      renderWithProviders(<ForumPost {...defaultProps} post={postWithoutTags} />)

      expect(screen.queryByTestId('post-tags')).not.toBeInTheDocument()
    })
  })

  describe('Author Information', () => {
    it('should display author username', () => {
      renderWithProviders(<ForumPost {...defaultProps} />)

      expect(screen.getByTestId('author-username')).toHaveTextContent('testuser')
    })

    it('should display author avatar when provided', () => {
      renderWithProviders(<ForumPost {...defaultProps} />)

      const avatar = screen.getByRole('img', { name: /testuser avatar/i })
      expect(avatar).toHaveAttribute('src', '/avatars/testuser.jpg')
    })

    it('should display default avatar when not provided', () => {
      const authorWithoutAvatar = { ...mockAuthor, avatar: undefined }
      const postWithoutAuthorAvatar = { ...mockPost, author: authorWithoutAvatar }
      renderWithProviders(<ForumPost {...defaultProps} post={postWithoutAuthorAvatar} />)

      expect(screen.getByTestId('default-avatar')).toBeInTheDocument()
    })

    it('should display author post count', () => {
      renderWithProviders(<ForumPost {...defaultProps} />)

      expect(screen.getByTestId('author-post-count')).toHaveTextContent('25 posts')
    })

    it('should display author badges', () => {
      renderWithProviders(<ForumPost {...defaultProps} />)

      const badgesContainer = screen.getByTestId('author-badges')
      expect(within(badgesContainer).getByText('Early Adopter')).toBeInTheDocument()
      expect(within(badgesContainer).getByText('Helpful')).toBeInTheDocument()
    })

    it('should hide badges when author has none', () => {
      const authorWithoutBadges = { ...mockAuthor, badges: [] }
      const postWithoutBadges = { ...mockPost, author: authorWithoutBadges }
      renderWithProviders(<ForumPost {...defaultProps} post={postWithoutBadges} />)

      expect(screen.queryByTestId('author-badges')).not.toBeInTheDocument()
    })
  })

  describe('Voting Functionality', () => {
    it('should display vote count', () => {
      renderWithProviders(<ForumPost {...defaultProps} />)

      expect(screen.getByTestId('vote-count')).toHaveTextContent('13') // 15 - 2
    })

    it('should display upvote button', () => {
      renderWithProviders(<ForumPost {...defaultProps} />)

      expect(screen.getByRole('button', { name: /upvote/i })).toBeInTheDocument()
    })

    it('should display downvote button', () => {
      renderWithProviders(<ForumPost {...defaultProps} />)

      expect(screen.getByRole('button', { name: /downvote/i })).toBeInTheDocument()
    })

    it('should call onVote with upvote when upvote button is clicked', async () => {
      const user = userEvent.setup()
      const onVote = vi.fn()
      renderWithProviders(<ForumPost {...defaultProps} onVote={onVote} />)

      const upvoteButton = screen.getByRole('button', { name: /upvote/i })
      await user.click(upvoteButton)

      expect(onVote).toHaveBeenCalledWith(1, 'upvote')
    })

    it('should call onVote with downvote when downvote button is clicked', async () => {
      const user = userEvent.setup()
      const onVote = vi.fn()
      renderWithProviders(<ForumPost {...defaultProps} onVote={onVote} />)

      const downvoteButton = screen.getByRole('button', { name: /downvote/i })
      await user.click(downvoteButton)

      expect(onVote).toHaveBeenCalledWith(1, 'downvote')
    })

    it('should show active upvote state when user has upvoted', () => {
      renderWithProviders(<ForumPost {...defaultProps} userVote="upvote" />)

      const upvoteButton = screen.getByRole('button', { name: /upvote/i })
      expect(upvoteButton).toHaveClass('vote-button--active')
    })

    it('should show active downvote state when user has downvoted', () => {
      renderWithProviders(<ForumPost {...defaultProps} userVote="downvote" />)

      const downvoteButton = screen.getByRole('button', { name: /downvote/i })
      expect(downvoteButton).toHaveClass('vote-button--active')
    })
  })

  describe('Comment Count Display', () => {
    it('should display comment count', () => {
      renderWithProviders(<ForumPost {...defaultProps} />)

      expect(screen.getByTestId('comment-count')).toHaveTextContent('8 replies')
    })

    it('should display singular form for single comment', () => {
      const postWithOneReply = { ...mockPost, replyCount: 1 }
      renderWithProviders(<ForumPost {...defaultProps} post={postWithOneReply} />)

      expect(screen.getByTestId('comment-count')).toHaveTextContent('1 reply')
    })

    it('should display zero comments appropriately', () => {
      const postWithNoReplies = { ...mockPost, replyCount: 0 }
      renderWithProviders(<ForumPost {...defaultProps} post={postWithNoReplies} />)

      expect(screen.getByTestId('comment-count')).toHaveTextContent('0 replies')
    })
  })

  describe('Edit Functionality', () => {
    it('should show edit button for post author', () => {
      renderWithProviders(<ForumPost {...defaultProps} currentUserId={1} />)

      expect(screen.getByRole('button', { name: /edit/i })).toBeInTheDocument()
    })

    it('should hide edit button for non-author', () => {
      renderWithProviders(<ForumPost {...defaultProps} currentUserId={2} />)

      expect(screen.queryByRole('button', { name: /edit/i })).not.toBeInTheDocument()
    })

    it('should call onEdit when edit button is clicked', async () => {
      const user = userEvent.setup()
      const onEdit = vi.fn()
      renderWithProviders(<ForumPost {...defaultProps} currentUserId={1} onEdit={onEdit} />)

      const editButton = screen.getByRole('button', { name: /edit/i })
      await user.click(editButton)

      expect(onEdit).toHaveBeenCalledWith(1)
    })

    it('should show edit indicator when post has been edited', () => {
      const editedPost = { ...mockPost, updatedAt: '2024-01-12T10:00:00Z' }
      renderWithProviders(<ForumPost {...defaultProps} post={editedPost} />)

      expect(screen.getByTestId('edit-indicator')).toBeInTheDocument()
    })
  })

  describe('Share Functionality', () => {
    it('should display share button', () => {
      renderWithProviders(<ForumPost {...defaultProps} />)

      expect(screen.getByRole('button', { name: /share/i })).toBeInTheDocument()
    })

    it('should call onShare when share button is clicked', async () => {
      const user = userEvent.setup()
      const onShare = vi.fn()
      renderWithProviders(<ForumPost {...defaultProps} onShare={onShare} />)

      const shareButton = screen.getByRole('button', { name: /share/i })
      await user.click(shareButton)

      expect(onShare).toHaveBeenCalledWith(1)
    })
  })

  describe('Post States', () => {
    it('should display pinned indicator for pinned posts', () => {
      const pinnedPost = { ...mockPost, isPinned: true }
      renderWithProviders(<ForumPost {...defaultProps} post={pinnedPost} />)

      expect(screen.getByTestId('pinned-indicator')).toBeInTheDocument()
      expect(screen.getByText(/pinned/i)).toBeInTheDocument()
    })

    it('should hide pinned indicator for non-pinned posts', () => {
      renderWithProviders(<ForumPost {...defaultProps} />)

      expect(screen.queryByTestId('pinned-indicator')).not.toBeInTheDocument()
    })

    it('should display locked indicator for locked posts', () => {
      const lockedPost = { ...mockPost, isLocked: true }
      renderWithProviders(<ForumPost {...defaultProps} post={lockedPost} />)

      expect(screen.getByTestId('locked-indicator')).toBeInTheDocument()
      expect(screen.getByText(/locked/i)).toBeInTheDocument()
    })

    it('should disable voting on locked posts', () => {
      const lockedPost = { ...mockPost, isLocked: true }
      renderWithProviders(<ForumPost {...defaultProps} post={lockedPost} />)

      const upvoteButton = screen.getByRole('button', { name: /upvote/i })
      const downvoteButton = screen.getByRole('button', { name: /downvote/i })

      expect(upvoteButton).toBeDisabled()
      expect(downvoteButton).toBeDisabled()
    })

    it('should display answered indicator when post is answered', () => {
      renderWithProviders(<ForumPost {...defaultProps} isAnswered />)

      expect(screen.getByTestId('answered-indicator')).toBeInTheDocument()
      expect(screen.getByText(/answered/i)).toBeInTheDocument()
    })

    it('should hide answered indicator when post is not answered', () => {
      renderWithProviders(<ForumPost {...defaultProps} />)

      expect(screen.queryByTestId('answered-indicator')).not.toBeInTheDocument()
    })

    it('should display multiple state indicators simultaneously', () => {
      const multiStatePost = { ...mockPost, isPinned: true, isLocked: true }
      renderWithProviders(<ForumPost {...defaultProps} post={multiStatePost} isAnswered />)

      expect(screen.getByTestId('pinned-indicator')).toBeInTheDocument()
      expect(screen.getByTestId('locked-indicator')).toBeInTheDocument()
      expect(screen.getByTestId('answered-indicator')).toBeInTheDocument()
    })
  })

  describe('User Interactions', () => {
    it('should call onView when post title is clicked', async () => {
      const user = userEvent.setup()
      const onView = vi.fn()
      renderWithProviders(<ForumPost {...defaultProps} onView={onView} />)

      const titleLink = screen.getByRole('link', { name: /how to improve focus/i })
      await user.click(titleLink)

      expect(onView).toHaveBeenCalledWith(1)
    })

    it('should call onView when post content is clicked', async () => {
      const user = userEvent.setup()
      const onView = vi.fn()
      renderWithProviders(<ForumPost {...defaultProps} onView={onView} />)

      const content = screen.getByTestId('post-content')
      await user.click(content)

      expect(onView).toHaveBeenCalledWith(1)
    })

    it('should not call onView when action buttons are clicked', async () => {
      const user = userEvent.setup()
      const onView = vi.fn()
      const onVote = vi.fn()
      renderWithProviders(<ForumPost {...defaultProps} onView={onView} onVote={onVote} />)

      const upvoteButton = screen.getByRole('button', { name: /upvote/i })
      await user.click(upvoteButton)

      expect(onVote).toHaveBeenCalled()
      expect(onView).not.toHaveBeenCalled()
    })
  })

  describe('Loading State', () => {
    it('should show skeleton when loading', () => {
      renderWithProviders(<ForumPost {...defaultProps} isLoading />)

      expect(screen.getByTestId('forum-post-skeleton')).toBeInTheDocument()
    })

    it('should hide actual content when loading', () => {
      renderWithProviders(<ForumPost {...defaultProps} isLoading />)

      expect(screen.queryByTestId('forum-post')).not.toBeInTheDocument()
    })
  })

  describe('Accessibility', () => {
    it('should have accessible post structure', () => {
      renderWithProviders(<ForumPost {...defaultProps} />)

      const post = screen.getByTestId('forum-post')
      expect(post).toHaveAttribute('role', 'article')
    })

    it('should have accessible vote buttons', () => {
      renderWithProviders(<ForumPost {...defaultProps} />)

      const upvoteButton = screen.getByRole('button', { name: /upvote/i })
      const downvoteButton = screen.getByRole('button', { name: /downvote/i })

      expect(upvoteButton).toHaveAttribute('aria-label', 'Upvote this post')
      expect(downvoteButton).toHaveAttribute('aria-label', 'Downvote this post')
    })

    it('should have accessible vote count', () => {
      renderWithProviders(<ForumPost {...defaultProps} />)

      const voteCount = screen.getByTestId('vote-count')
      expect(voteCount).toHaveAttribute('aria-label', 'Post score: 13 points')
    })

    it('should have keyboard navigation support for title', async () => {
      const user = userEvent.setup()
      const onView = vi.fn()
      renderWithProviders(<ForumPost {...defaultProps} onView={onView} />)

      const titleLink = screen.getByRole('link', { name: /how to improve focus/i })
      titleLink.focus()

      await user.keyboard('{Enter}')
      expect(onView).toHaveBeenCalled()
    })

    it('should have keyboard navigation support for voting', async () => {
      const user = userEvent.setup()
      const onVote = vi.fn()
      renderWithProviders(<ForumPost {...defaultProps} onVote={onVote} />)

      const upvoteButton = screen.getByRole('button', { name: /upvote/i })
      upvoteButton.focus()

      await user.keyboard('{Enter}')
      expect(onVote).toHaveBeenCalledWith(1, 'upvote')
    })

    it('should announce state changes to screen readers', () => {
      const pinnedPost = { ...mockPost, isPinned: true }
      renderWithProviders(<ForumPost {...defaultProps} post={pinnedPost} />)

      const pinnedIndicator = screen.getByTestId('pinned-indicator')
      expect(pinnedIndicator).toHaveAttribute('aria-label', 'This post is pinned')
    })
  })

  describe('Responsive Design', () => {
    it('should hide author details on compact layout', () => {
      renderWithProviders(<ForumPost {...defaultProps} compact />)

      expect(screen.queryByTestId('author-post-count')).not.toBeInTheDocument()
      expect(screen.queryByTestId('author-badges')).not.toBeInTheDocument()
    })

    it('should show simplified layout when compact', () => {
      renderWithProviders(<ForumPost {...defaultProps} compact />)

      const post = screen.getByTestId('forum-post')
      expect(post).toHaveClass('forum-post--compact')
    })

    it('should maintain essential functionality in compact mode', () => {
      renderWithProviders(<ForumPost {...defaultProps} compact />)

      expect(screen.getByText('How to improve focus during long study sessions?')).toBeInTheDocument()
      expect(screen.getByTestId('vote-count')).toBeInTheDocument()
      expect(screen.getByRole('button', { name: /upvote/i })).toBeInTheDocument()
    })
  })

  describe('Error Handling', () => {
    it('should handle missing author gracefully', () => {
      const postWithoutAuthor = { ...mockPost, author: undefined }
      renderWithProviders(<ForumPost {...defaultProps} post={postWithoutAuthor} />)

      expect(screen.getByTestId('unknown-author')).toHaveTextContent('Unknown User')
    })

    it('should handle missing category gracefully', () => {
      const postWithoutCategory = { ...mockPost, category: undefined }
      renderWithProviders(<ForumPost {...defaultProps} post={postWithoutCategory} />)

      expect(screen.queryByTestId('category-chip')).not.toBeInTheDocument()
    })

    it('should handle invalid vote counts', () => {
      const postWithNegativeVotes = { ...mockPost, likeCount: -5, dislikeCount: -2 }
      renderWithProviders(<ForumPost {...defaultProps} post={postWithNegativeVotes} />)

      expect(screen.getByTestId('vote-count')).toHaveTextContent('0')
    })
  })
})