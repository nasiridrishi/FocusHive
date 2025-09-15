import React from 'react'
import {render, screen, within} from '@testing-library/react'
import {describe, expect, it, vi} from 'vitest'
import ForumReplyThread from '../ForumReplyThread'
import {ForumReply} from '../../types'

// Mock the API module
vi.mock('../../services/forumApi', () => ({
  forumApi: {
    likeReply: vi.fn(),
    unlikeReply: vi.fn(),
    createReply: vi.fn(),
    updateReply: vi.fn()
  }
}))

describe('ForumReplyThread XSS Security', () => {
  const mockReply: ForumReply = {
    id: 1,
    content: '<script>alert("XSS Attack")</script><img src=x onerror="alert(\'XSS\')"><p onclick="alert(\'XSS\')">Normal text</p>',
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
    postId: 1,
    createdAt: '2024-01-01T00:00:00Z',
    updatedAt: '2024-01-01T00:00:00Z',
    likeCount: 0,
    dislikeCount: 0,
    childReplies: [],
    isHidden: false,
    isModeratorReply: false
  }

  it('should be vulnerable to XSS attacks through reply content (FAILING TEST)', () => {
    // This test SHOULD FAIL initially to demonstrate the vulnerability
    // After implementing DOMPurify, this test should PASS

    render(
      <ForumReplyThread
        replies={[mockReply]}
        postId={1}
        onReplyUpdate={() => {}}
      />
    )

    // Check that dangerous scripts are NOT executed
    // Currently this will FAIL because the content is rendered as raw HTML
    const replyContent = screen.getByText(/Normal text/, {selector: 'p'})

    // The script tags should be removed or sanitized
    expect(replyContent.innerHTML).not.toContain('<script>')
    expect(replyContent.innerHTML).not.toContain('onerror')
    expect(replyContent.innerHTML).not.toContain('onclick')
  })

  it('should properly sanitize malicious HTML attributes', () => {
    const maliciousReply: ForumReply = {
      ...mockReply,
      id: 2,
      content: '<a href="javascript:void(0)" onclick="alert(\'XSS\')">Click me</a><div style="background: url(javascript:alert(\'XSS\'))">Styled content</div>'
    }

    render(
      <ForumReplyThread
        replies={[maliciousReply]}
        postId={1}
        onReplyUpdate={() => {}}
      />
    )

    // Links with javascript: protocol should be sanitized
    const links = screen.queryAllByRole('link')
    links.forEach(link => {
      expect(link.getAttribute('href')).not.toContain('javascript:')
    })

    // onclick handlers should be removed
    const allElements = screen.getAllByText(/Click me|Styled content/)
    // Check that none of the elements contain onclick or javascript:
    allElements.forEach(element => {
      const parent = element.closest('p') || element.parentElement
      if (parent) {
        expect(parent.innerHTML).not.toContain('onclick')
        expect(parent.innerHTML).not.toContain('javascript:')
      }
    })
  })

  it('should sanitize iframe and embed tags', () => {
    const iframeReply: ForumReply = {
      ...mockReply,
      id: 3,
      content: '<iframe src="https://malicious.com"></iframe><embed src="malicious.swf"><object data="malicious.pdf"></object>Safe text'
    }

    render(
      <ForumReplyThread
        replies={[iframeReply]}
        postId={1}
        onReplyUpdate={() => {}}
      />
    )

    // Dangerous tags should be removed
    const container = screen.getByText(/Safe text/)
    expect(container.innerHTML).not.toContain('<iframe')
    expect(container.innerHTML).not.toContain('<embed')
    expect(container.innerHTML).not.toContain('<object')
  })

  it('should allow safe HTML tags and attributes', () => {
    const safeReply: ForumReply = {
      ...mockReply,
      id: 4,
      content: '<p><strong>Bold text</strong></p><em>Italic text</em><a href="https://example.com">Safe link</a><ul><li>List item</li></ul>'
    }

    render(
      <ForumReplyThread
        replies={[safeReply]}
        postId={1}
        onReplyUpdate={() => {}}
      />
    )

    // Safe HTML should be preserved
    expect(screen.getByText('Bold text')).toBeInTheDocument()
    expect(screen.getByText('Italic text')).toBeInTheDocument()
    expect(screen.getByText('Safe link')).toBeInTheDocument()
    expect(screen.getByText('List item')).toBeInTheDocument()

    // Safe link should maintain its href
    const safeLink = screen.getByText('Safe link')
    expect(safeLink.closest('a')?.getAttribute('href')).toBe('https://example.com')
  })

  it('should handle nested replies with XSS attempts', () => {
    const nestedReply: ForumReply = {
      ...mockReply,
      childReplies: [
        {
          ...mockReply,
          id: 5,
          content: '<script>alert("Nested XSS")</script>Nested reply content',
          childReplies: []
        }
      ]
    }

    render(
      <ForumReplyThread
        replies={[nestedReply]}
        postId={1}
        onReplyUpdate={() => {}}
      />
    )

    // Check both parent and nested replies are sanitized
    const nestedContent = screen.getByText(/Nested reply content/)
    expect(nestedContent.innerHTML).not.toContain('<script>')
  })

  it('should sanitize SVG-based XSS attacks', () => {
    const svgReply: ForumReply = {
      ...mockReply,
      id: 6,
      content: '<svg onload="alert(\'XSS\')"><script>alert("SVG XSS")</script></svg><svg><use href="data:image/svg+xml;base64,PHN2ZyBpZD0ieCIgeG1sbnM9Imh0dHA6Ly93d3cudzMub3JnLzIwMDAvc3ZnIj48c2NyaXB0PmFsZXJ0KCJYU1MiKTwvc2NyaXB0Pjwvc3ZnPg==#x"></use></svg>Normal content'
    }

    render(
      <ForumReplyThread
        replies={[svgReply]}
        postId={1}
        onReplyUpdate={() => {}}
      />
    )

    // SVG with malicious content should be sanitized
    const content = screen.getByText(/Normal content/)
    expect(content.innerHTML).not.toContain('onload')
    expect(content.innerHTML).not.toContain('<script>')
  })

  it('should sanitize data URIs and protocols', () => {
    const dataUriReply: ForumReply = {
      ...mockReply,
      id: 7,
      content: '<a href="data:text/html,<script>alert(\'XSS\')</script>">Data URI</a><img src="data:text/html,<script>alert(\'XSS\')</script>">Text content'
    }

    render(
      <ForumReplyThread
        replies={[dataUriReply]}
        postId={1}
        onReplyUpdate={() => {}}
      />
    )

    // Data URIs with scripts should be sanitized
    const content = screen.getByText(/Text content/)
    const links = content.querySelectorAll('a')
    links.forEach(link => {
      const href = link.getAttribute('href')
      if (href?.startsWith('data:')) {
        expect(href).not.toContain('<script>')
      }
    })
  })

  it('should handle encoded XSS attempts', () => {
    const encodedReply: ForumReply = {
      ...mockReply,
      id: 8,
      content: '&lt;script&gt;alert("XSS")&lt;/script&gt;<div>&#60;script&#62;alert("XSS")&#60;/script&#62;</div>Normal text'
    }

    render(
      <ForumReplyThread
        replies={[encodedReply]}
        postId={1}
        onReplyUpdate={() => {}}
      />
    )

    // Even encoded script attempts should not execute
    const content = screen.getByText(/Normal text/)
    // After sanitization, encoded entities are safe and won't execute
    // The important thing is that there are no actual script tags
    const parent = content.closest('p') || content.parentElement
    if (parent) {
      expect(parent.innerHTML).not.toContain('<script>')
      expect(parent.innerHTML).not.toContain('</script>')
    }
  })
})