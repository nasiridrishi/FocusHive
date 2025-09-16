import DOMPurify from 'dompurify'

/**
 * Sanitizes HTML content to prevent XSS attacks while preserving safe HTML formatting
 *
 * @param html - The HTML string to sanitize
 * @returns Sanitized HTML string safe for rendering
 */
export function sanitizeHtml(html: string): string {
  // Configure DOMPurify with safe defaults
  const config = {
    // Allow these tags for rich text formatting
    ALLOWED_TAGS: [
      'p', 'br', 'span', 'div',
      'strong', 'b', 'em', 'i', 'u',
      'h1', 'h2', 'h3', 'h4', 'h5', 'h6',
      'a', 'img',
      'ul', 'ol', 'li',
      'blockquote', 'pre', 'code',
      'table', 'thead', 'tbody', 'tr', 'td', 'th',
      'hr'
    ],
    // Allow these attributes
    ALLOWED_ATTR: [
      'href', 'src', 'alt', 'title',
      'class', 'id',
      'target', 'rel',
      'width', 'height'
      // Removed 'style' to prevent CSS-based XSS attacks
    ],
    // Force all links to open in new window with security attributes
    ADD_ATTR: ['target', 'rel'],
    // Remove dangerous protocols
    ALLOWED_URI_REGEXP: /^(?:(?:(?:f|ht)tps?|mailto|tel|callto|cid|xmpp):|[^a-z]|[a-z+.\-]+(?:[^a-z+.\-:]|$))/i,
    // Keep text content even if tags are removed
    KEEP_CONTENT: true,
    // Return DOM elements for better performance
    RETURN_DOM: false,
    // Return DOM fragments for better performance
    RETURN_DOM_FRAGMENT: false,
    // Return the sanitized string
    RETURN_TRUSTED_TYPE: false,
    // Sanitize for safe DOM manipulation
    SAFE_FOR_TEMPLATES: true,
    // Remove <script> tags completely
    FORBID_TAGS: ['script', 'style', 'iframe', 'embed', 'object', 'form', 'input', 'button', 'select', 'textarea', 'meta', 'base'],
    // Remove dangerous attributes (event handlers are removed by default)
    FORBID_ATTR: ['style', 'onerror', 'onclick', 'onload', 'onmouseover', 'onfocus', 'onblur', 'onkeydown', 'onkeyup', 'onsubmit']
  }

  // Sanitize the HTML
  const clean = DOMPurify.sanitize(html, config) as string

  // Additional post-processing for links
  if (clean.includes('<a ')) {
    // Add security attributes to all links
    const tempDiv = document.createElement('div')
    tempDiv.innerHTML = clean

    const links = tempDiv.querySelectorAll('a')
    links.forEach(link => {
      // Ensure external links open in new tab with security
      const href = link.getAttribute('href')
      if (href && (href.startsWith('http://') || href.startsWith('https://'))) {
        link.setAttribute('target', '_blank')
        link.setAttribute('rel', 'noopener noreferrer')
      }
    })

    return tempDiv.innerHTML
  }

  return clean
}

/**
 * Sanitizes HTML with minimal formatting (text-only mode)
 * Useful for previews and summaries
 *
 * @param html - The HTML string to sanitize
 * @returns Plain text with minimal formatting
 */
export function sanitizeHtmlMinimal(html: string): string {
  const config = {
    ALLOWED_TAGS: ['b', 'i', 'em', 'strong', 'br'],
    ALLOWED_ATTR: [],
    KEEP_CONTENT: true
  }

  return DOMPurify.sanitize(html, config) as string
}

/**
 * Strips all HTML tags and returns plain text
 *
 * @param html - The HTML string to convert to plain text
 * @returns Plain text without any HTML
 */
export function htmlToPlainText(html: string): string {
  const config = {
    ALLOWED_TAGS: [],
    ALLOWED_ATTR: [],
    KEEP_CONTENT: true
  }

  return DOMPurify.sanitize(html, config) as string
}