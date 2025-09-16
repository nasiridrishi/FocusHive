/**
 * Color Contrast Utilities for WCAG 2.1 AA/AAA Compliance
 *
 * Provides utilities for calculating and validating color contrast ratios
 * according to WCAG guidelines.
 */

/**
 * Convert hex color to RGB values
 */
function hexToRgb(hex: string): [number, number, number] | null {
  const result = /^#?([a-f\d]{2})([a-f\d]{2})([a-f\d]{2})$/i.exec(hex);
  return result ? [
    parseInt(result[1], 16),
    parseInt(result[2], 16),
    parseInt(result[3], 16)
  ] : null;
}

/**
 * Convert RGB to relative luminance according to WCAG formula
 */
function getRelativeLuminance(r: number, g: number, b: number): number {
  // Convert to sRGB
  const [rs, gs, bs] = [r, g, b].map(c => {
    c = c / 255;
    return c <= 0.03928 ? c / 12.92 : Math.pow((c + 0.055) / 1.055, 2.4);
  });

  // Apply luminance formula
  return 0.2126 * rs + 0.7152 * gs + 0.0722 * bs;
}

/**
 * Calculate contrast ratio between two colors
 */
export function calculateContrastRatio(color1: string, color2: string): number {
  const rgb1 = hexToRgb(color1);
  const rgb2 = hexToRgb(color2);

  if (!rgb1 || !rgb2) {
    // console.warn('Invalid color format provided to calculateContrastRatio');
    return 1;
  }

  const l1 = getRelativeLuminance(...rgb1);
  const l2 = getRelativeLuminance(...rgb2);

  const lighter = Math.max(l1, l2);
  const darker = Math.min(l1, l2);

  return (lighter + 0.05) / (darker + 0.05);
}

/**
 * WCAG contrast requirements
 */
export const WCAG_CONTRAST_RATIOS = {
  AA_NORMAL: 4.5,
  AA_LARGE: 3,
  AAA_NORMAL: 7,
  AAA_LARGE: 4.5,
} as const;

export type WCAGLevel = 'AA' | 'AAA';
export type TextSize = 'normal' | 'large';

/**
 * Check if contrast ratio meets WCAG requirements
 */
export function meetsContrastRequirement(
    foreground: string,
    background: string,
    level: WCAGLevel = 'AA',
    textSize: TextSize = 'normal'
): boolean {
  const ratio = calculateContrastRatio(foreground, background);

  const requirement = level === 'AA'
      ? (textSize === 'normal' ? WCAG_CONTRAST_RATIOS.AA_NORMAL : WCAG_CONTRAST_RATIOS.AA_LARGE)
      : (textSize === 'normal' ? WCAG_CONTRAST_RATIOS.AAA_NORMAL : WCAG_CONTRAST_RATIOS.AAA_LARGE);

  return ratio >= requirement;
}

/**
 * Get the best contrast color (black or white) for a given background
 */
export function getBestContrastColor(backgroundColor: string): '#000000' | '#ffffff' {
  const whiteContrast = calculateContrastRatio('#ffffff', backgroundColor);
  const blackContrast = calculateContrastRatio('#000000', backgroundColor);

  return whiteContrast > blackContrast ? '#ffffff' : '#000000';
}

/**
 * Validate if a color palette meets accessibility standards
 */
export interface ColorPaletteValidation {
  isValid: boolean;
  issues: string[];
  suggestions: string[];
}

export function validateColorPalette(
    foregroundColor: string,
    backgroundColor: string,
    level: WCAGLevel = 'AA'
): ColorPaletteValidation {
  const issues: string[] = [];
  const suggestions: string[] = [];

  const normalTextRatio = calculateContrastRatio(foregroundColor, backgroundColor);
  const meetsNormal = meetsContrastRequirement(foregroundColor, backgroundColor, level, 'normal');
  const meetsLarge = meetsContrastRequirement(foregroundColor, backgroundColor, level, 'large');

  if (!meetsNormal) {
    issues.push(`Normal text contrast ratio ${normalTextRatio.toFixed(2)}:1 does not meet ${level} standards (${level === 'AA' ? '4.5' : '7'}:1 required)`);

    if (meetsLarge) {
      suggestions.push('Use larger text (18pt+ or 14pt+ bold) to meet accessibility standards');
    } else {
      const bestContrast = getBestContrastColor(backgroundColor);
      suggestions.push(`Consider using ${bestContrast} text on this background for better contrast`);
    }
  }

  if (!meetsLarge) {
    issues.push(`Large text contrast ratio ${normalTextRatio.toFixed(2)}:1 does not meet ${level} standards for large text (${level === 'AA' ? '3' : '4.5'}:1 required)`);
  }

  return {
    isValid: meetsNormal,
    issues,
    suggestions
  };
}

/**
 * Generate accessible color variations
 */
export function generateAccessibleColorVariations(
    baseColor: string,
    targetBackground: string = '#ffffff',
    level: WCAGLevel = 'AA'
): string[] {
  const rgb = hexToRgb(baseColor);
  if (!rgb) return [];

  const variations: string[] = [];
  const [r, g, b] = rgb;

  // Try different luminance adjustments
  for (let adjustment = 0.1; adjustment <= 0.9; adjustment += 0.1) {
    // Darken
    const darkerR = Math.max(0, Math.floor(r * (1 - adjustment)));
    const darkerG = Math.max(0, Math.floor(g * (1 - adjustment)));
    const darkerB = Math.max(0, Math.floor(b * (1 - adjustment)));
    const darkerHex = `#${darkerR.toString(16).padStart(2, '0')}${darkerG.toString(16).padStart(2, '0')}${darkerB.toString(16).padStart(2, '0')}`;

    if (meetsContrastRequirement(darkerHex, targetBackground, level)) {
      variations.push(darkerHex);
    }

    // Lighten
    const lighterR = Math.min(255, Math.floor(r * (1 + adjustment)));
    const lighterG = Math.min(255, Math.floor(g * (1 + adjustment)));
    const lighterB = Math.min(255, Math.floor(b * (1 + adjustment)));
    const lighterHex = `#${lighterR.toString(16).padStart(2, '0')}${lighterG.toString(16).padStart(2, '0')}${lighterB.toString(16).padStart(2, '0')}`;

    if (meetsContrastRequirement(lighterHex, targetBackground, level)) {
      variations.push(lighterHex);
    }
  }

  return Array.from(new Set(variations)); // Remove duplicates
}

/**
 * Check if a color is considered "large text" according to WCAG
 */
export function isLargeText(fontSize: number, fontWeight: number | string): boolean {
  // 18pt or larger is always considered large
  if (fontSize >= 18) return true;

  // 14pt or larger with bold weight (700+) is considered large
  const weight = typeof fontWeight === 'string'
      ? (fontWeight === 'bold' ? 700 : 400)
      : fontWeight;

  return fontSize >= 14 && weight >= 700;
}

/**
 * Accessibility color constants based on WCAG guidelines
 */
export const ACCESSIBLE_COLORS = {
  // High contrast pairs (7:1 ratio for AAA compliance)
  HIGH_CONTRAST: [
    {foreground: '#000000', background: '#ffffff', ratio: 21},
    {foreground: '#ffffff', background: '#000000', ratio: 21},
    {foreground: '#ffffff', background: '#1f1f1f', ratio: 18.5},
    {foreground: '#000000', background: '#f0f0f0', ratio: 16.8},
  ],

  // Standard contrast pairs (4.5:1 ratio for AA compliance)
  STANDARD_CONTRAST: [
    {foreground: '#333333', background: '#ffffff', ratio: 12.6},
    {foreground: '#666666', background: '#ffffff', ratio: 5.7},
    {foreground: '#ffffff', background: '#333333', ratio: 12.6},
    {foreground: '#ffffff', background: '#666666', ratio: 5.7},
  ],

  // Focus indicator colors with high contrast
  FOCUS_INDICATORS: [
    '#005fcc', // Blue focus ring
    '#0066cc', // Slightly lighter blue
    '#cc0000', // Red for errors
    '#cc6600', // Orange for warnings
    '#006600', // Green for success
  ]
} as const;