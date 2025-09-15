# FocusHive Logo and Icon Guide

## Overview
This guide explains how to manage and update logos and icons throughout the FocusHive application.

## Logo Files Structure

### Source Files Location
- **Pixelmator Files**: `/logo/`
  - `focushive app icon.pxd` - Square app icon for favicons and PWA
  - `focushive logo.pxd` - Full logo with text

### Required Export Formats

#### For Web Application
Export these files to `/public/logo/`:
- `logo.svg` - Full logo with text (preferred format)
- `logo.png` - Full logo with text (512px width, transparent background)
- `icon.svg` - Square icon only (no text)
- `icon.png` - Square icon (512x512px, transparent background)

#### For PWA and Favicons
Export to `/logo/` directory first, then run the icon generation script:
- `icon.png` - 512x512px square icon (this will be used to generate all sizes)

## Generating Icons

### Prerequisites
- Install ImageMagick: `brew install imagemagick`

### Steps to Generate All Icon Sizes

1. **Export the icon from Pixelmator**:
   ```bash
   # Open focushive app icon.pxd in Pixelmator
   # Export as PNG at 512x512px
   # Save as: /logo/icon.png
   ```

2. **Run the generation script**:
   ```bash
   cd /Users/nasir/uol/focushive/frontend
   ./scripts/generate-icons.sh
   ```

3. **Verify generated icons**:
   ```bash
   ls -la public/icons/
   ```

## Logo Usage in Components

### Logo Component
The application uses a centralized Logo component located at:
`/src/shared/components/Logo.tsx`

### Usage Examples

```tsx
// Basic usage
import { Logo } from '@shared/components/Logo'

// Full logo with text
<Logo variant="full" height={40} />

// Icon only
<Logo variant="icon" width={32} height={32} />

// Logo with link
import { LogoLink } from '@shared/components/Logo'

<LogoLink 
  variant="full"
  height={35}
  onClick={() => navigate('/dashboard')}
/>
```

## Where Logos Are Used

### 1. Application Header
- **File**: `/src/shared/layout/Header.tsx`
- **Usage**: Full logo in desktop view
- **Size**: height=32px

### 2. Application Sidebar
- **File**: `/src/shared/layout/AppLayout.tsx`
- **Usage**: Full logo in app bar
- **Size**: height=35px

### 3. Login Page
- **File**: `/src/features/auth/components/LoginForm.tsx`
- **Usage**: Full logo above login form
- **Size**: height=48px

### 4. Registration Page
- **File**: `/src/features/auth/components/RegisterForm.tsx`
- **Usage**: Full logo above registration form
- **Size**: height=48px

### 5. PWA Manifest
- **File**: `/vite.config.ts` (manifest configuration)
- **Icons**: All sizes from 72x72 to 512x512

### 6. HTML Meta Tags
- **File**: `/index.html`
- **Icons**: favicon.ico, apple-touch-icon, various sizes

## Updating Logos

### To update all logos and icons:

1. **Update source files**:
   - Edit the Pixelmator files in `/logo/`
   - Export new versions as described above

2. **Export for web**:
   ```bash
   # Export to public/logo/
   - logo.svg (full logo)
   - logo.png (full logo, 512px width)
   - icon.svg (icon only)
   - icon.png (icon only, 512x512)
   ```

3. **Generate favicon sizes**:
   ```bash
   # Export icon.png to /logo/ directory
   ./scripts/generate-icons.sh
   ```

4. **Clear browser cache**:
   - Hard refresh: Cmd+Shift+R (Mac) or Ctrl+Shift+R (Windows/Linux)
   - Clear application cache in DevTools

5. **Test PWA installation**:
   - Uninstall existing PWA if installed
   - Reinstall to see new icons

## Icon Sizes Reference

| Size | Usage |
|------|-------|
| 16x16 | Browser favicon (small) |
| 32x32 | Browser favicon (standard) |
| 72x72 | PWA icon (small) |
| 96x96 | PWA icon |
| 128x128 | PWA icon |
| 144x144 | PWA icon |
| 152x152 | PWA icon (iOS) |
| 180x180 | Apple touch icon |
| 192x192 | PWA icon (Android) |
| 384x384 | PWA icon (large) |
| 512x512 | PWA icon (extra large) |

## Troubleshooting

### Icons not updating in browser
1. Clear browser cache
2. Check DevTools Network tab for 404 errors
3. Verify files exist in `/public/icons/`

### PWA icons not updating
1. Uninstall the PWA
2. Clear browser cache
3. Reinstall the PWA
4. Check manifest.webmanifest is being served correctly

### Logo not showing in components
1. Check browser console for 404 errors
2. Verify Logo component import path
3. Check that logo files exist in `/public/logo/`

## Best Practices

1. **Always use SVG when possible** for better scalability
2. **Keep PNG fallbacks** for compatibility
3. **Use transparent backgrounds** for flexibility
4. **Optimize file sizes** - use tools like SVGO for SVG files
5. **Test on multiple devices** - especially mobile for PWA
6. **Version control** - commit logo changes with descriptive messages

## Color Scheme
- Primary color: `#1976d2` (Material Blue)
- Use this for mask-icon color and theme-color meta tags