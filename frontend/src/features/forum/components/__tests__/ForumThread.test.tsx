import React from 'react';
import { render, screen, fireEvent, waitFor, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { vi, describe, it, expect, beforeEach, afterEach } from 'vitest';
import { ThemeProvider } from '@mui/material/styles';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import theme from '@shared/theme/theme';
import { ForumThread } from '../ForumThread';
import type { ForumPost as ForumThreadType, ForumReply } from '@features/forum/types';

// Mock data
const mockThread: ForumThreadType = {
  id: '1',
  title: 'How to stay focused during long study sessions?',
  slug: 'how-to-stay-focused-during-long-study-sessions',
  content: 'I find it difficult to maintain focus for more than an hour. Any tips?',
  authorId: 'user1',
  author: {
    id: 'user1',
    username: 'john_doe',
    avatar: '/avatar1.jpg',
    role: 'member',
    joinDate: '2024-01-01T00:00:00Z',
    postCount: 42,
    reputation: 150
  },
  categoryId: 'productivity',
  category: {
    id: 'productivity',
    name: 'Productivity',
    slug: 'productivity',
    description: 'Tips and tricks for productivity',
    color: '#4CAF50',
    icon: 'rocket',
    postCount: 100,
    topicCount: 25,
    isLocked: false,
    isPrivate: false,
    order: 1,
    createdAt: '2024-01-01T00:00:00Z',
    updatedAt: '2024-01-01T00:00:00Z'
  },
  createdAt: '2024-01-15T10:00:00Z',
  updatedAt: '2024-01-15T10:00:00Z',
  viewCount: 234,
  likeCount: 23,
  dislikeCount: 2,
  replyCount: 5,
  tags: ['focus', 'study', 'productivity'],
  isPinned: false,
  isLocked: false,
  isHidden: false,
  lastReplyAt: '2024-01-16T15:30:00Z',
  replies: [
    {
      id: 'reply1',
      content: 'Try the Pomodoro technique! 25 minutes of focus, then 5 minute break.',
      authorId: 'user2',
      author: {
        id: 'user2',
        username: 'jane_smith',
        avatar: '/avatar2.jpg',
        role: 'member',
        joinDate: '2024-01-02T00:00:00Z',
        postCount: 30,
        reputation: 100
      },
      postId: '1',
      parentReplyId: null,
      createdAt: '2024-01-15T11:00:00Z',
      updatedAt: '2024-01-15T11:00:00Z',
      likeCount: 10,
      dislikeCount: 0,
      isHidden: false,
      isModeratorReply: false,
      childReplies: [
        {
          id: 'reply2',
          content: 'Pomodoro works great! I also use background music.',
          authorId: 'user3',
          author: {
            id: 'user3',
            username: 'bob_wilson',
            avatar: '/avatar3.jpg',
            role: 'member',
            joinDate: '2024-01-03T00:00:00Z',
            postCount: 15,
            reputation: 50
          },
          postId: '1',
          parentReplyId: 'reply1',
          createdAt: '2024-01-15T12:00:00Z',
          updatedAt: '2024-01-15T12:00:00Z',
          likeCount: 5,
          dislikeCount: 0,
          isHidden: false,
          isModeratorReply: false,
          childReplies: []
        }
      ]
    },
    {
      id: 'reply3',
      content: 'Make sure to stay hydrated and take regular breaks!',
      authorId: 'user4',
      author: {
        id: 'user4',
        username: 'alice_jones',
        avatar: '/avatar4.jpg',
        role: 'MODERATOR',
        joinDate: '2024-01-01T00:00:00Z',
        postCount: 100,
        reputation: 500
      },
      postId: '1',
      parentReplyId: null,
      createdAt: '2024-01-16T15:30:00Z',
      updatedAt: '2024-01-16T15:30:00Z',
      likeCount: 15,
      dislikeCount: 1,
      isHidden: false,
      isModeratorReply: true,
      childReplies: []
    }
  ]
};

const mockEmptyThread: ForumThreadType = {
  ...mockThread,
  replies: []
};

// Test utilities
const renderWithProviders = (component: React.ReactElement) => {
  const queryClient = new QueryClient({
    defaultOptions: {
      queries: { retry: false },
      mutations: { retry: false }
    }
  });

  return render(
    <QueryClientProvider client={queryClient}>
      <ThemeProvider theme={theme}>
        {component}
      </ThemeProvider>
    </QueryClientProvider>
  );
};

describe('ForumThread Component', () => {
  const mockOnReply = vi.fn();
  const mockOnVote = vi.fn();
  const mockOnEdit = vi.fn();
  const mockOnDelete = vi.fn();
  const mockOnReport = vi.fn();

  beforeEach(() => {
    vi.clearAllMocks();
  });

  afterEach(() => {
    vi.clearAllMocks();
  });

  describe('Rendering Tests', () => {
    it('should render thread with title and content', () => {
      renderWithProviders(
        <ForumThread thread={mockThread} />
      );

      expect(screen.getByText('How to stay focused during long study sessions?')).toBeInTheDocument();
      expect(screen.getByText(/I find it difficult to maintain focus/)).toBeInTheDocument();
    });

    it('should display author information', () => {
      renderWithProviders(
        <ForumThread thread={mockThread} />
      );

      expect(screen.getByText('john_doe')).toBeInTheDocument();
      expect(screen.getByAltText('john_doe')).toHaveAttribute('src', '/avatar1.jpg');
    });

    it('should show timestamp and metadata', () => {
      renderWithProviders(
        <ForumThread thread={mockThread} />
      );

      expect(screen.getByTestId('thread-metadata')).toBeInTheDocument();
      expect(screen.getByText(/234 views/)).toBeInTheDocument();
      expect(screen.getByText(/5 replies/)).toBeInTheDocument();
    });

    it('should render nested replies correctly', () => {
      renderWithProviders(
        <ForumThread thread={mockThread} />
      );

      expect(screen.getByText(/Try the Pomodoro technique/)).toBeInTheDocument();
      expect(screen.getByText(/Pomodoro works great/)).toBeInTheDocument();
      expect(screen.getByText(/Make sure to stay hydrated/)).toBeInTheDocument();
    });

    it('should handle empty replies gracefully', () => {
      renderWithProviders(
        <ForumThread thread={mockEmptyThread} />
      );

      expect(screen.getByText('No replies yet')).toBeInTheDocument();
      expect(screen.getByText('Be the first to reply!')).toBeInTheDocument();
    });

    it('should show moderator badge for moderator replies', () => {
      renderWithProviders(
        <ForumThread thread={mockThread} />
      );

      const moderatorReply = screen.getByText(/Make sure to stay hydrated/).closest('[data-testid^="reply-"]');
      expect(within(moderatorReply as HTMLElement).getByTestId('moderator-badge')).toBeInTheDocument();
    });

    it('should display tags', () => {
      renderWithProviders(
        <ForumThread thread={mockThread} />
      );

      expect(screen.getByText('focus')).toBeInTheDocument();
      expect(screen.getByText('study')).toBeInTheDocument();
      expect(screen.getByText('productivity')).toBeInTheDocument();
    });

    it('should show thread lines connecting nested replies', () => {
      renderWithProviders(
        <ForumThread thread={mockThread} />
      );

      const nestedReply = screen.getByText(/Pomodoro works great/).closest('[data-testid^="reply-"]');
      expect(nestedReply).toHaveClass('nested-reply');
      expect(nestedReply?.querySelector('.thread-line')).toBeInTheDocument();
    });

    it('should indicate when thread is locked', () => {
      const lockedThread = { ...mockThread, isLocked: true };
      renderWithProviders(
        <ForumThread thread={lockedThread} />
      );

      expect(screen.getByTestId('thread-locked-indicator')).toBeInTheDocument();
      expect(screen.getByText('This thread is locked')).toBeInTheDocument();
    });

    it('should show pinned indicator for pinned threads', () => {
      const pinnedThread = { ...mockThread, isPinned: true };
      renderWithProviders(
        <ForumThread thread={pinnedThread} />
      );

      expect(screen.getByTestId('thread-pinned-indicator')).toBeInTheDocument();
    });
  });

  describe('Interaction Tests', () => {
    it('should toggle collapse/expand thread functionality', async () => {
      const user = userEvent.setup();
      renderWithProviders(
        <ForumThread thread={mockThread} />
      );

      const collapseButton = screen.getByTestId('collapse-thread-button');
      expect(screen.getByText(/Try the Pomodoro technique/)).toBeVisible();

      await user.click(collapseButton);
      await waitFor(() => {
        expect(screen.queryByText(/Try the Pomodoro technique/)).not.toBeVisible();
      });

      await user.click(collapseButton);
      await waitFor(() => {
        expect(screen.getByText(/Try the Pomodoro technique/)).toBeVisible();
      });
    });

    it('should open reply form when reply button is clicked', async () => {
      const user = userEvent.setup();
      renderWithProviders(
        <ForumThread
          thread={mockThread}
          currentUserId="user1"
          onReply={mockOnReply}
        />
      );

      const replyButton = screen.getByTestId('reply-button-1');
      await user.click(replyButton);

      expect(screen.getByTestId('reply-form-1')).toBeInTheDocument();
      expect(screen.getByPlaceholderText('Write your reply...')).toBeInTheDocument();
    });

    it('should submit reply with correct data', async () => {
      const user = userEvent.setup();
      mockOnReply.mockResolvedValue(undefined);

      renderWithProviders(
        <ForumThread
          thread={mockThread}
          currentUserId="user1"
          onReply={mockOnReply}
        />
      );

      const replyButton = screen.getByTestId('reply-button-1');
      await user.click(replyButton);

      const replyInput = screen.getByPlaceholderText('Write your reply...');
      await user.type(replyInput, 'This is my reply');

      const submitButton = screen.getByTestId('submit-reply-button');
      await user.click(submitButton);

      expect(mockOnReply).toHaveBeenCalledWith('1', 'This is my reply');
    });

    it('should show edit button for own posts', () => {
      renderWithProviders(
        <ForumThread
          thread={mockThread}
          currentUserId="user1"
          onEdit={mockOnEdit}
        />
      );

      expect(screen.getByTestId('edit-button-1')).toBeInTheDocument();
      expect(screen.queryByTestId('edit-button-reply1')).not.toBeInTheDocument();
    });

    it('should handle edit action', async () => {
      const user = userEvent.setup();
      mockOnEdit.mockResolvedValue(undefined);

      renderWithProviders(
        <ForumThread
          thread={mockThread}
          currentUserId="user1"
          onEdit={mockOnEdit}
        />
      );

      const editButton = screen.getByTestId('edit-button-1');
      await user.click(editButton);

      const editInput = screen.getByDisplayValue(/I find it difficult to maintain focus/);
      await user.clear(editInput);
      await user.type(editInput, 'Updated content');

      const saveButton = screen.getByTestId('save-edit-button');
      await user.click(saveButton);

      expect(mockOnEdit).toHaveBeenCalledWith('1', 'Updated content');
    });

    it('should show delete button for own posts', () => {
      renderWithProviders(
        <ForumThread
          thread={mockThread}
          currentUserId="user1"
          onDelete={mockOnDelete}
        />
      );

      expect(screen.getByTestId('delete-button-1')).toBeInTheDocument();
    });

    it('should handle delete action with confirmation', async () => {
      const user = userEvent.setup();
      mockOnDelete.mockResolvedValue(undefined);

      renderWithProviders(
        <ForumThread
          thread={mockThread}
          currentUserId="user1"
          onDelete={mockOnDelete}
        />
      );

      const deleteButton = screen.getByTestId('delete-button-1');
      await user.click(deleteButton);

      expect(screen.getByText('Are you sure you want to delete this post?')).toBeInTheDocument();

      const confirmButton = screen.getByTestId('confirm-delete-button');
      await user.click(confirmButton);

      expect(mockOnDelete).toHaveBeenCalledWith('1');
    });

    it('should handle upvote functionality', async () => {
      const user = userEvent.setup();
      mockOnVote.mockResolvedValue(undefined);

      renderWithProviders(
        <ForumThread
          thread={mockThread}
          currentUserId="user5"
          onVote={mockOnVote}
        />
      );

      const upvoteButton = screen.getByTestId('upvote-button-1');
      expect(upvoteButton).toHaveTextContent('23');

      await user.click(upvoteButton);
      expect(mockOnVote).toHaveBeenCalledWith('1', 'up');
    });

    it('should handle downvote functionality', async () => {
      const user = userEvent.setup();
      mockOnVote.mockResolvedValue(undefined);

      renderWithProviders(
        <ForumThread
          thread={mockThread}
          currentUserId="user5"
          onVote={mockOnVote}
        />
      );

      const downvoteButton = screen.getByTestId('downvote-button-1');
      expect(downvoteButton).toHaveTextContent('2');

      await user.click(downvoteButton);
      expect(mockOnVote).toHaveBeenCalledWith('1', 'down');
    });

    it('should handle report functionality', async () => {
      const user = userEvent.setup();
      mockOnReport.mockResolvedValue(undefined);

      renderWithProviders(
        <ForumThread
          thread={mockThread}
          currentUserId="user5"
          onReport={mockOnReport}
        />
      );

      const moreButton = screen.getByTestId('more-actions-button-1');
      await user.click(moreButton);

      const reportButton = screen.getByText('Report');
      await user.click(reportButton);

      expect(screen.getByTestId('report-dialog')).toBeInTheDocument();

      const reasonInput = screen.getByPlaceholderText('Explain why you are reporting this...');
      await user.type(reasonInput, 'Spam content');

      const submitReportButton = screen.getByTestId('submit-report-button');
      await user.click(submitReportButton);

      expect(mockOnReport).toHaveBeenCalledWith('1', 'Spam content');
    });
  });

  describe('Sorting Tests', () => {
    it('should sort by newest by default', () => {
      renderWithProviders(
        <ForumThread thread={mockThread} sortBy="newest" />
      );

      const replies = screen.getAllByTestId(/^reply-/);
      expect(replies[0]).toHaveTextContent('Make sure to stay hydrated');
      expect(replies[1]).toHaveTextContent('Try the Pomodoro technique');
    });

    it('should sort by top (most upvotes)', () => {
      renderWithProviders(
        <ForumThread thread={mockThread} sortBy="top" />
      );

      const replies = screen.getAllByTestId(/^reply-/);
      expect(replies[0]).toHaveTextContent('Make sure to stay hydrated'); // 15 likes
      expect(replies[1]).toHaveTextContent('Try the Pomodoro technique'); // 10 likes
    });

    it('should sort by controversial (most activity)', () => {
      renderWithProviders(
        <ForumThread thread={mockThread} sortBy="controversial" />
      );

      const replies = screen.getAllByTestId(/^reply-/);
      // Controversial = high engagement (likes + dislikes + replies)
      expect(replies[0]).toHaveTextContent('Try the Pomodoro technique'); // Has child replies
    });

    it('should maintain sort when new replies are added', async () => {
      const { rerender } = renderWithProviders(
        <ForumThread thread={mockThread} sortBy="newest" />
      );

      const newReply: ForumReply = {
        id: 'reply4',
        content: 'New reply just added',
        authorId: 'user6',
        author: {
          id: 'user6',
          username: 'new_user',
          avatar: '/avatar6.jpg',
          role: 'member',
          joinDate: '2024-01-17T00:00:00Z',
          postCount: 1,
          reputation: 0
        },
        postId: '1',
        parentReplyId: null,
        createdAt: '2024-01-17T10:00:00Z',
        updatedAt: '2024-01-17T10:00:00Z',
        likeCount: 0,
        dislikeCount: 0,
        isHidden: false,
        isModeratorReply: false,
        childReplies: []
      };

      const updatedThread = {
        ...mockThread,
        replies: [...mockThread.replies!, newReply]
      };

      rerender(
        <ThemeProvider theme={theme}>
          <ForumThread thread={updatedThread} sortBy="newest" />
        </ThemeProvider>
      );

      const replies = screen.getAllByTestId(/^reply-/);
      expect(replies[0]).toHaveTextContent('New reply just added');
    });
  });

  describe('Nested Reply Tests', () => {
    it('should render replies up to 5 levels deep', () => {
      const deepThread: ForumThreadType = {
        ...mockThread,
        replies: [{
          ...mockThread.replies![0],
          childReplies: [{
            ...mockThread.replies![0].childReplies![0],
            childReplies: [{
              id: 'level3',
              content: 'Level 3 reply',
              authorId: 'user7',
              author: {
                id: 'user7',
                username: 'level3_user',
                avatar: '',
                role: 'member',
                joinDate: '2024-01-01T00:00:00Z',
                postCount: 1,
                reputation: 0
              },
              postId: '1',
              parentReplyId: 'reply2',
              createdAt: '2024-01-15T13:00:00Z',
              updatedAt: '2024-01-15T13:00:00Z',
              likeCount: 0,
              dislikeCount: 0,
              isHidden: false,
              isModeratorReply: false,
              childReplies: [{
                id: 'level4',
                content: 'Level 4 reply',
                authorId: 'user8',
                author: {
                  id: 'user8',
                  username: 'level4_user',
                  avatar: '',
                  role: 'member',
                  joinDate: '2024-01-01T00:00:00Z',
                  postCount: 1,
                  reputation: 0
                },
                postId: '1',
                parentReplyId: 'level3',
                createdAt: '2024-01-15T14:00:00Z',
                updatedAt: '2024-01-15T14:00:00Z',
                likeCount: 0,
                dislikeCount: 0,
                isHidden: false,
                isModeratorReply: false,
                childReplies: [{
                  id: 'level5',
                  content: 'Level 5 reply',
                  authorId: 'user9',
                  author: {
                    id: 'user9',
                    username: 'level5_user',
                    avatar: '',
                    role: 'member',
                    joinDate: '2024-01-01T00:00:00Z',
                    postCount: 1,
                    reputation: 0
                  },
                  postId: '1',
                  parentReplyId: 'level4',
                  createdAt: '2024-01-15T15:00:00Z',
                  updatedAt: '2024-01-15T15:00:00Z',
                  likeCount: 0,
                  dislikeCount: 0,
                  isHidden: false,
                  isModeratorReply: false,
                  childReplies: []
                }]
              }]
            }]
          }]
        }]
      };

      renderWithProviders(
        <ForumThread thread={deepThread} maxDepth={5} />
      );

      expect(screen.getByText('Level 3 reply')).toBeInTheDocument();
      expect(screen.getByText('Level 4 reply')).toBeInTheDocument();
      expect(screen.getByText('Level 5 reply')).toBeInTheDocument();
    });

    it('should show "Continue thread" link for deeper nesting beyond max depth', () => {
      const deepThread: ForumThreadType = {
        ...mockThread,
        replies: [{
          ...mockThread.replies![0],
          childReplies: [{
            ...mockThread.replies![0].childReplies![0],
            childReplies: [{
              id: 'level3',
              content: 'Level 3 reply',
              authorId: 'user7',
              author: {
                id: 'user7',
                username: 'level3_user',
                avatar: '',
                role: 'member',
                joinDate: '2024-01-01T00:00:00Z',
                postCount: 1,
                reputation: 0
              },
              postId: '1',
              parentReplyId: 'reply2',
              createdAt: '2024-01-15T13:00:00Z',
              updatedAt: '2024-01-15T13:00:00Z',
              likeCount: 0,
              dislikeCount: 0,
              isHidden: false,
              isModeratorReply: false,
              childReplies: [{
                id: 'level4',
                content: 'Level 4 reply - should show continue',
                authorId: 'user8',
                author: {
                  id: 'user8',
                  username: 'level4_user',
                  avatar: '',
                  role: 'member',
                  joinDate: '2024-01-01T00:00:00Z',
                  postCount: 1,
                  reputation: 0
                },
                postId: '1',
                parentReplyId: 'level3',
                createdAt: '2024-01-15T14:00:00Z',
                updatedAt: '2024-01-15T14:00:00Z',
                likeCount: 0,
                dislikeCount: 0,
                isHidden: false,
                isModeratorReply: false,
                childReplies: []
              }]
            }]
          }]
        }]
      };

      renderWithProviders(
        <ForumThread thread={deepThread} maxDepth={3} />
      );

      expect(screen.getByText('Level 3 reply')).toBeInTheDocument();
      expect(screen.queryByText('Level 4 reply - should show continue')).not.toBeInTheDocument();
      expect(screen.getByText('Continue thread →')).toBeInTheDocument();
    });

    it('should have proper indentation for each nesting level', () => {
      renderWithProviders(
        <ForumThread thread={mockThread} />
      );

      const level1Reply = screen.getByText(/Try the Pomodoro technique/).closest('[data-testid^="reply-"]');
      const level2Reply = screen.getByText(/Pomodoro works great/).closest('[data-testid^="reply-"]');

      expect(level1Reply).toHaveStyle({ paddingLeft: '0px' });
      expect(level2Reply).toHaveStyle({ marginLeft: '24px' });
    });

    it('should display thread lines connecting nested replies', () => {
      renderWithProviders(
        <ForumThread thread={mockThread} />
      );

      const nestedReply = screen.getByText(/Pomodoro works great/).closest('[data-testid^="reply-"]');
      const threadLine = nestedReply?.querySelector('[data-testid="thread-line"]');
      expect(threadLine).toBeInTheDocument();
      expect(threadLine).toHaveStyle({
        borderLeft: '2px solid',
        position: 'absolute'
      });
    });
  });

  describe('Performance Tests', () => {
    it('should lazy load deep threads', async () => {
      const deepThreadWithMany = {
        ...mockThread,
        replies: Array.from({ length: 50 }, (_, i) => ({
          id: `reply${i}`,
          content: `Reply number ${i}`,
          authorId: 'user2',
          author: {
            id: 'user2',
            username: 'test_user',
            avatar: '',
            role: 'member' as const,
            joinDate: '2024-01-01T00:00:00Z',
            postCount: 10,
            reputation: 50
          },
          postId: '1',
          parentReplyId: null,
          createdAt: `2024-01-15T${10 + i}:00:00Z`,
          updatedAt: `2024-01-15T${10 + i}:00:00Z`,
          likeCount: i,
          dislikeCount: 0,
          isHidden: false,
          isModeratorReply: false,
          childReplies: []
        }))
      };

      renderWithProviders(
        <ForumThread thread={deepThreadWithMany} />
      );

      // Initially show limited replies
      const visibleReplies = screen.getAllByTestId(/^reply-/);
      expect(visibleReplies.length).toBeLessThanOrEqual(20);

      // Find and click load more button
      const loadMoreButton = screen.getByTestId('load-more-replies');
      fireEvent.click(loadMoreButton);

      await waitFor(() => {
        const allReplies = screen.getAllByTestId(/^reply-/);
        expect(allReplies.length).toBeGreaterThan(20);
      });
    });

    it('should debounce voting actions', async () => {
      const user = userEvent.setup();
      mockOnVote.mockResolvedValue(undefined);

      renderWithProviders(
        <ForumThread
          thread={mockThread}
          currentUserId="user5"
          onVote={mockOnVote}
        />
      );

      const upvoteButton = screen.getByTestId('upvote-button-1');

      // Rapid clicks
      await user.click(upvoteButton);
      await user.click(upvoteButton);
      await user.click(upvoteButton);

      // Wait for debounce
      await waitFor(() => {
        // Should only be called once due to debouncing
        expect(mockOnVote).toHaveBeenCalledTimes(1);
      }, { timeout: 500 });
    });

    it('should show optimistic UI updates', async () => {
      const user = userEvent.setup();
      mockOnVote.mockResolvedValue(undefined);

      renderWithProviders(
        <ForumThread
          thread={mockThread}
          currentUserId="user5"
          onVote={mockOnVote}
        />
      );

      const upvoteButton = screen.getByTestId('upvote-button-1');
      expect(upvoteButton).toHaveTextContent('23');

      await user.click(upvoteButton);

      // Optimistic update should show immediately
      expect(upvoteButton).toHaveTextContent('24');
      expect(upvoteButton).toHaveClass('vote-active');
    });
  });

  describe('Accessibility Tests', () => {
    it('should support keyboard navigation through threads', async () => {
      renderWithProviders(
        <ForumThread
          thread={mockThread}
          currentUserId="user1"
        />
      );

      const firstReply = screen.getByTestId('reply-reply1');
      firstReply.focus();
      expect(document.activeElement).toBe(firstReply);

      // Tab to next interactive element
      fireEvent.keyDown(document.activeElement!, { key: 'Tab' });
      expect(document.activeElement?.getAttribute('data-testid')).toContain('button');
    });

    it('should have proper ARIA labels for actions', () => {
      renderWithProviders(
        <ForumThread
          thread={mockThread}
          currentUserId="user1"
          onVote={mockOnVote}
        />
      );

      expect(screen.getByLabelText('Upvote thread')).toBeInTheDocument();
      expect(screen.getByLabelText('Downvote thread')).toBeInTheDocument();
      expect(screen.getByLabelText('Reply to thread')).toBeInTheDocument();
      expect(screen.getByLabelText('More actions')).toBeInTheDocument();
    });

    it('should announce screen reader updates for vote changes', async () => {
      const user = userEvent.setup();
      mockOnVote.mockResolvedValue(undefined);

      renderWithProviders(
        <ForumThread
          thread={mockThread}
          currentUserId="user5"
          onVote={mockOnVote}
        />
      );

      const upvoteButton = screen.getByTestId('upvote-button-1');
      await user.click(upvoteButton);

      // Check for screen reader announcement
      const announcement = screen.getByRole('status');
      expect(announcement).toHaveTextContent('Vote recorded');
    });

    it('should manage focus properly when opening reply form', async () => {
      const user = userEvent.setup();
      renderWithProviders(
        <ForumThread
          thread={mockThread}
          currentUserId="user1"
          onReply={mockOnReply}
        />
      );

      const replyButton = screen.getByTestId('reply-button-1');
      await user.click(replyButton);

      const replyInput = screen.getByPlaceholderText('Write your reply...');
      expect(document.activeElement).toBe(replyInput);
    });

    it('should have proper heading hierarchy', () => {
      renderWithProviders(
        <ForumThread thread={mockThread} />
      );

      const headings = screen.getAllByRole('heading');
      expect(headings[0]).toHaveProperty('tagName', 'H2'); // Thread title

      // Reply author names should be h3
      const replyHeadings = screen.getAllByTestId(/^reply-author-/);
      replyHeadings.forEach(heading => {
        expect(heading.tagName).toBe('H3');
      });
    });
  });

  describe('Error Handling Tests', () => {
    it('should handle network error on voting', async () => {
      const user = userEvent.setup();
      mockOnVote.mockRejectedValue(new Error('Network error'));

      renderWithProviders(
        <ForumThread
          thread={mockThread}
          currentUserId="user5"
          onVote={mockOnVote}
        />
      );

      const upvoteButton = screen.getByTestId('upvote-button-1');
      await user.click(upvoteButton);

      await waitFor(() => {
        expect(screen.getByText('Failed to record vote. Please try again.')).toBeInTheDocument();
      });

      // Should revert optimistic update
      expect(upvoteButton).toHaveTextContent('23');
    });

    it('should handle failed reply submission', async () => {
      const user = userEvent.setup();
      mockOnReply.mockRejectedValue(new Error('Failed to post reply'));

      renderWithProviders(
        <ForumThread
          thread={mockThread}
          currentUserId="user1"
          onReply={mockOnReply}
        />
      );

      const replyButton = screen.getByTestId('reply-button-1');
      await user.click(replyButton);

      const replyInput = screen.getByPlaceholderText('Write your reply...');
      await user.type(replyInput, 'Test reply');

      const submitButton = screen.getByTestId('submit-reply-button');
      await user.click(submitButton);

      await waitFor(() => {
        expect(screen.getByText('Failed to post reply. Please try again.')).toBeInTheDocument();
      });

      // Form should remain open with content preserved
      expect(replyInput).toHaveValue('Test reply');
    });

    it('should handle deleted parent thread gracefully', () => {
      const threadWithDeletedParent = {
        ...mockThread,
        replies: [{
          ...mockThread.replies![0],
          isHidden: true,
          content: '[Deleted]'
        }]
      };

      renderWithProviders(
        <ForumThread thread={threadWithDeletedParent} />
      );

      expect(screen.getByText('[Deleted]')).toBeInTheDocument();
      expect(screen.getByTestId('reply-reply1')).toHaveClass('reply-deleted');
    });

    it('should show rate limiting feedback', async () => {
      const user = userEvent.setup();
      mockOnReply.mockRejectedValue(new Error('Rate limited: Please wait before posting again'));

      renderWithProviders(
        <ForumThread
          thread={mockThread}
          currentUserId="user1"
          onReply={mockOnReply}
        />
      );

      const replyButton = screen.getByTestId('reply-button-1');
      await user.click(replyButton);

      const replyInput = screen.getByPlaceholderText('Write your reply...');
      await user.type(replyInput, 'Test');

      const submitButton = screen.getByTestId('submit-reply-button');
      await user.click(submitButton);

      await waitFor(() => {
        expect(screen.getByText(/Please wait before posting again/)).toBeInTheDocument();
      });
    });
  });

  describe('Loading States', () => {
    it('should show loading skeleton when isLoading is true', () => {
      renderWithProviders(
        <ForumThread thread={mockThread} isLoading={true} />
      );

      expect(screen.getAllByTestId('skeleton-loader')).toHaveLength(3);
    });

    it('should show loading indicator when submitting reply', async () => {
      const user = userEvent.setup();
      mockOnReply.mockImplementation(() => new Promise(resolve => setTimeout(resolve, 1000)));

      renderWithProviders(
        <ForumThread
          thread={mockThread}
          currentUserId="user1"
          onReply={mockOnReply}
        />
      );

      const replyButton = screen.getByTestId('reply-button-1');
      await user.click(replyButton);

      const replyInput = screen.getByPlaceholderText('Write your reply...');
      await user.type(replyInput, 'Test reply');

      const submitButton = screen.getByTestId('submit-reply-button');
      await user.click(submitButton);

      expect(screen.getByTestId('reply-loading-spinner')).toBeInTheDocument();
      expect(submitButton).toBeDisabled();
    });
  });

  describe('Edge Cases', () => {
    it('should handle thread with no author gracefully', () => {
      const threadNoAuthor = {
        ...mockThread,
        author: null
      };

      renderWithProviders(
        <ForumThread thread={threadNoAuthor as any} />
      );

      expect(screen.getByText('[Deleted User]')).toBeInTheDocument();
    });

    it('should handle very long content with expand/collapse', () => {
      const longContent = 'Lorem ipsum '.repeat(100);
      const threadWithLongContent = {
        ...mockThread,
        content: longContent
      };

      renderWithProviders(
        <ForumThread thread={threadWithLongContent} />
      );

      expect(screen.getByText('Show more')).toBeInTheDocument();

      fireEvent.click(screen.getByText('Show more'));
      expect(screen.getByText('Show less')).toBeInTheDocument();
    });

    it('should handle special characters in content', () => {
      const specialContent = {
        ...mockThread,
        content: '<script>alert("XSS")</script> & special < > characters'
      };

      renderWithProviders(
        <ForumThread thread={specialContent} />
      );

      // Should be escaped/sanitized
      expect(screen.queryByText('alert')).not.toBeInTheDocument();
      expect(screen.getByText(/special < > characters/)).toBeInTheDocument();
    });
  });
});