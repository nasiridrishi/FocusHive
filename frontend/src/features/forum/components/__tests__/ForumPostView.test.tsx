import React from 'react'
import {render, screen, waitFor} from '@testing-library/react'
import {describe, expect, it, vi, beforeEach} from 'vitest'
import {MemoryRouter, Route, Routes} from 'react-router-dom'
import ForumPostView from '../ForumPostView'
import {ForumPost} from '../../types'

// Mock the API module
vi.mock('../../services/forumApi', () => ({
  forumApi: {
    getPostBySlug: vi.fn(),
    getReplies: vi.fn(),
    likePost: vi.fn(),
    unlikePost: vi.fn(),
    createReply: vi.fn()
  }
}))

// Import the mocked module to set up responses
import {forumApi} from '../../services/forumApi'

describe('ForumPostView XSS Security', () => {
  const mockPost: ForumPost = {
    id: 1,
    title: 'Test Post',
    slug: 'test-post',
    content: '<script>alert("XSS Attack")</script><img src=x onerror="alert(\'XSS\')"><p onclick="alert(\'XSS\')">Normal post content</p>',
    categoryId: 1,
    authorId: 1,
    author: {
      id: 1,
      username: 'testuser',
      avatar: '',
      role: 'USER',
      joinDate: '2024-01-01T00:00:00Z',
      postCount: 10,
      reputation: 100
    },
    category: {
      id: 1,
      name: 'General',
      slug: 'general',
      description: 'General discussion',
      postCount: 100,
      topicCount: 50,
      icon: 'forum',
      isLocked: false,
      isPrivate: false,
      order: 0,
      createdAt: '2024-01-01T00:00:00Z',
      updatedAt: '2024-01-01T00:00:00Z'
    },
    createdAt: '2024-01-01T00:00:00Z',
    updatedAt: '2024-01-01T00:00:00Z',
    viewCount: 100,
    likeCount: 10,
    dislikeCount: 2,
    replyCount: 5,
    tags: ['test', 'xss'],
    isPinned: false,
    isLocked: false,
    isHidden: false
  }

  beforeEach(() => {
    vi.clearAllMocks()
  })

  const renderPostView = () => {
    return render(
      <MemoryRouter initialEntries={['/forum/posts/test-post']}>
        <Routes>
          <Route path="/forum/posts/:postSlug" element={<ForumPostView />} />
        </Routes>
      </MemoryRouter>
    )
  }

  it('should be vulnerable to XSS attacks through post content (FAILING TEST)', async () => {
    // This test SHOULD FAIL initially to demonstrate the vulnerability
    // After implementing DOMPurify, this test should PASS

    vi.mocked(forumApi.getPostBySlug).mockResolvedValue(mockPost)
    vi.mocked(forumApi.getReplies).mockResolvedValue({
      replies: [],
      currentPage: 1,
      totalPages: 1,
      totalReplies: 0
    })

    renderPostView()

    await waitFor(() => {
      expect(screen.getAllByText('Test Post')[0]).toBeInTheDocument()
    })

    // Check that dangerous scripts are NOT executed
    const postContent = screen.getByText(/Normal post content/)

    // The script tags should be removed or sanitized
    expect(postContent.innerHTML).not.toContain('<script>')
    expect(postContent.innerHTML).not.toContain('onerror')
    expect(postContent.innerHTML).not.toContain('onclick')
  })

  it('should properly sanitize malicious HTML in post content', async () => {
    const maliciousPost: ForumPost = {
      ...mockPost,
      content: '<a href="javascript:void(0)" onclick="alert(\'XSS\')">Malicious link</a><form action="https://evil.com"><input name="password"></form>Safe content'
    }

    vi.mocked(forumApi.getPostBySlug).mockResolvedValue(maliciousPost)
    vi.mocked(forumApi.getReplies).mockResolvedValue({
      replies: [],
      currentPage: 1,
      totalPages: 1,
      totalReplies: 0
    })

    renderPostView()

    await waitFor(() => {
      expect(screen.getAllByText('Test Post')[0]).toBeInTheDocument()
    })

    // Forms should be removed
    expect(screen.queryByRole('form')).not.toBeInTheDocument()

    // JavaScript protocols should be sanitized
    const content = screen.getByText(/Safe content/)
    expect(content.innerHTML).not.toContain('javascript:')
    expect(content.innerHTML).not.toContain('onclick')
  })

  it('should sanitize iframe, embed and object tags in posts', async () => {
    const embedPost: ForumPost = {
      ...mockPost,
      content: '<iframe src="https://malicious.com"></iframe><embed src="malicious.swf"><object data="malicious.pdf"></object><p>Clean content</p>'
    }

    vi.mocked(forumApi.getPostBySlug).mockResolvedValue(embedPost)
    vi.mocked(forumApi.getReplies).mockResolvedValue({
      replies: [],
      currentPage: 1,
      totalPages: 1,
      totalReplies: 0
    })

    renderPostView()

    await waitFor(() => {
      expect(screen.getAllByText('Test Post')[0]).toBeInTheDocument()
    })

    // Dangerous tags should be removed
    const content = screen.getByText(/Clean content/)
    expect(content.parentElement?.innerHTML).not.toContain('<iframe')
    expect(content.parentElement?.innerHTML).not.toContain('<embed')
    expect(content.parentElement?.innerHTML).not.toContain('<object')
  })

  it('should preserve safe HTML formatting in posts', async () => {
    const safePost: ForumPost = {
      ...mockPost,
      content: '<h2>Heading</h2><p><strong>Bold text</strong></p><em>Italic text</em><a href="https://example.com">Safe link</a><ul><li>List item</li></ul><blockquote>Quote</blockquote>'
    }

    vi.mocked(forumApi.getPostBySlug).mockResolvedValue(safePost)
    vi.mocked(forumApi.getReplies).mockResolvedValue({
      replies: [],
      currentPage: 1,
      totalPages: 1,
      totalReplies: 0
    })

    renderPostView()

    await waitFor(() => {
      expect(screen.getAllByText('Test Post')[0]).toBeInTheDocument()
    })

    // Safe HTML should be preserved
    expect(screen.getByText('Heading')).toBeInTheDocument()
    expect(screen.getByText('Bold text')).toBeInTheDocument()
    expect(screen.getByText('Italic text')).toBeInTheDocument()
    expect(screen.getByText('Safe link')).toBeInTheDocument()
    expect(screen.getByText('List item')).toBeInTheDocument()
    expect(screen.getByText('Quote')).toBeInTheDocument()

    // Safe link should maintain its href
    const safeLink = screen.getByText('Safe link')
    expect(safeLink.closest('a')?.getAttribute('href')).toBe('https://example.com')
  })

  it('should sanitize style attributes with javascript', async () => {
    const stylePost: ForumPost = {
      ...mockPost,
      content: '<div style="background: url(javascript:alert(\'XSS\'))">Styled div</div><p style="color: red; background-image: url(\'javascript:alert(1)\')">Colored text</p>'
    }

    vi.mocked(forumApi.getPostBySlug).mockResolvedValue(stylePost)
    vi.mocked(forumApi.getReplies).mockResolvedValue({
      replies: [],
      currentPage: 1,
      totalPages: 1,
      totalReplies: 0
    })

    renderPostView()

    await waitFor(() => {
      expect(screen.getAllByText('Test Post')[0]).toBeInTheDocument()
    })

    // JavaScript in styles should be removed - in fact all style attributes should be removed
    const elements = screen.getAllByText(/Styled div|Colored text/)
    elements.forEach(element => {
      // Verify that style attributes are completely removed
      expect(element.hasAttribute('style')).toBe(false)
      const parent = element.parentElement
      if (parent) {
        expect(parent.innerHTML).not.toContain('javascript:')
        expect(parent.innerHTML).not.toContain('url(javascript')
      }
    })
  })

  it('should sanitize event handlers in HTML', async () => {
    const eventPost: ForumPost = {
      ...mockPost,
      content: '<div onmouseover="alert(\'XSS\')" onload="alert(\'Load\')" onerror="alert(\'Error\')">Hover me</div><button onclick="alert(\'Click\')">Click me</button>'
    }

    vi.mocked(forumApi.getPostBySlug).mockResolvedValue(eventPost)
    vi.mocked(forumApi.getReplies).mockResolvedValue({
      replies: [],
      currentPage: 1,
      totalPages: 1,
      totalReplies: 0
    })

    renderPostView()

    await waitFor(() => {
      expect(screen.getAllByText('Test Post')[0]).toBeInTheDocument()
    })

    // Event handlers should be removed
    const elements = screen.getAllByText(/Hover me|Click me/)
    elements.forEach(element => {
      const parent = element.parentElement
      if (parent) {
        expect(parent.innerHTML).not.toContain('onmouseover')
        expect(parent.innerHTML).not.toContain('onload')
        expect(parent.innerHTML).not.toContain('onerror')
        expect(parent.innerHTML).not.toContain('onclick')
      }
    })
  })

  it('should handle meta refresh and meta tags', async () => {
    const metaPost: ForumPost = {
      ...mockPost,
      content: '<meta http-equiv="refresh" content="0;url=https://evil.com"><meta charset="evil">Normal content here'
    }

    vi.mocked(forumApi.getPostBySlug).mockResolvedValue(metaPost)
    vi.mocked(forumApi.getReplies).mockResolvedValue({
      replies: [],
      currentPage: 1,
      totalPages: 1,
      totalReplies: 0
    })

    renderPostView()

    await waitFor(() => {
      expect(screen.getAllByText('Test Post')[0]).toBeInTheDocument()
    })

    // Meta tags should be removed
    const content = screen.getByText(/Normal content here/)
    expect(content.innerHTML).not.toContain('<meta')
    expect(content.innerHTML).not.toContain('http-equiv')
  })

  it('should sanitize base64 encoded javascript in links', async () => {
    const base64Post: ForumPost = {
      ...mockPost,
      content: '<a href="data:text/html;base64,PHNjcmlwdD5hbGVydCgnWFNTJyk8L3NjcmlwdD4=">Base64 link</a><img src="data:image/svg+xml;base64,PHN2ZyBvbmxvYWQ9ImFsZXJ0KCdYU1MnKSI+">Text'
    }

    vi.mocked(forumApi.getPostBySlug).mockResolvedValue(base64Post)
    vi.mocked(forumApi.getReplies).mockResolvedValue({
      replies: [],
      currentPage: 1,
      totalPages: 1,
      totalReplies: 0
    })

    renderPostView()

    await waitFor(() => {
      expect(screen.getAllByText('Test Post')[0]).toBeInTheDocument()
    })

    // Base64 encoded scripts should be sanitized
    const content = screen.getByText(/Text/)
    const links = content.parentElement?.querySelectorAll('a[href*="data:"]') || []
    links.forEach(link => {
      const href = link.getAttribute('href')
      if (href && href.includes('base64')) {
        // The dangerous base64 content should be sanitized
        expect(href).not.toContain('PHNjcmlwdD5hbGVydCgnWFNTJyk8L3NjcmlwdD4')
      }
    })
  })

  it('should handle complex nested XSS attempts', async () => {
    const complexPost: ForumPost = {
      ...mockPost,
      content: `
        <div>
          <svg><foreignObject><iframe src="javascript:alert('XSS')"></iframe></foreignObject></svg>
          <math><mtext><iframe src="javascript:alert('XSS')"></iframe></mtext></math>
          <table background="javascript:alert('XSS')">
            <tr><td>Table content</td></tr>
          </table>
          Safe paragraph content
        </div>
      `
    }

    vi.mocked(forumApi.getPostBySlug).mockResolvedValue(complexPost)
    vi.mocked(forumApi.getReplies).mockResolvedValue({
      replies: [],
      currentPage: 1,
      totalPages: 1,
      totalReplies: 0
    })

    renderPostView()

    await waitFor(() => {
      expect(screen.getAllByText('Test Post')[0]).toBeInTheDocument()
    })

    // Complex nested attacks should be sanitized
    const content = screen.getByText(/Safe paragraph content/)
    expect(content.innerHTML).not.toContain('iframe')
    expect(content.innerHTML).not.toContain('javascript:')
    expect(content.innerHTML).not.toContain('foreignObject')
  })
})