import React, { useState } from 'react'
import {
  FormControl,
  InputLabel,
  Select,
  MenuItem,
  Button,
  Menu,
  ListItemIcon,
  ListItemText,
  ButtonGroup,
  Tooltip,
  CircularProgress,
  Alert,
  SelectChangeEvent,
  Box,
  Typography,
} from '@mui/material'
import {
  Language as LanguageIcon,
  KeyboardArrowDown as ArrowDownIcon,
  Check as CheckIcon,
} from '@mui/icons-material'
import { useLanguageSwitcher, useTranslation } from '../../../hooks/useI18n'
import { getAvailableLanguages, supportedLanguages } from '../../../lib/i18n'
import type { LanguageSwitcherProps, SupportedLocale } from '../../../types/i18n'

/**
 * Language switcher component with multiple display variants
 */
export const LanguageSwitcher: React.FC<LanguageSwitcherProps> = ({
  variant = 'menu',
  size = 'medium',
  showFlags = true,
  showNativeNames = true,
  onLanguageChange,
  className,
  disabled = false,
}) => {
  const { t } = useTranslation('common')
  const { currentLanguage, switchLanguage, isLanguageLoading: _isLanguageLoading } = useLanguageSwitcher()
  const [anchorEl, setAnchorEl] = useState<null | HTMLElement>(null)
  const [error, setError] = useState<string | null>(null)
  const [loading, setLoading] = useState(false)

  const availableLanguages = getAvailableLanguages()
  const isMenuOpen = Boolean(anchorEl)

  const handleLanguageChange = async (languageCode: SupportedLocale) => {
    if (languageCode === currentLanguage.code || loading) return

    setLoading(true)
    setError(null)

    try {
      await switchLanguage(languageCode)
      onLanguageChange?.(languageCode)
      setAnchorEl(null)
    } catch (err) {
      const errorMessage = err instanceof Error ? err.message : 'Failed to change language'
      setError(errorMessage)
      console.error('Language change failed:', err)
    } finally {
      setLoading(false)
    }
  }

  const handleMenuOpen = (event: React.MouseEvent<HTMLElement>) => {
    if (disabled || loading) return
    setAnchorEl(event.currentTarget)
  }

  const handleMenuClose = () => {
    setAnchorEl(null)
  }

  const handleSelectChange = (event: SelectChangeEvent<string>) => {
    const languageCode = event.target.value as SupportedLocale
    handleLanguageChange(languageCode)
  }

  const renderLanguageItem = (language: typeof supportedLanguages[SupportedLocale]) => (
    <Box display="flex" alignItems="center" gap={1}>
      {showFlags && (
        <Typography component="span" sx={{ fontSize: '1.2em' }}>
          {language.flag}
        </Typography>
      )}
      <Box>
        <Typography variant="body2" component="div">
          {language.name}
        </Typography>
        {showNativeNames && language.nativeName !== language.name && (
          <Typography variant="caption" color="text.secondary" component="div">
            {language.nativeName}
          </Typography>
        )}
      </Box>
    </Box>
  )

  if (variant === 'select') {
    return (
      <Box className={className}>
        <FormControl size={size as 'small' | 'medium'} disabled={disabled || loading} fullWidth>
          <InputLabel id="language-select-label">
            {t('language.select')}
          </InputLabel>
          <Select
            labelId="language-select-label"
            value={currentLanguage.code}
            onChange={handleSelectChange}
            label={t('language.select')}
            startAdornment={
              loading ? (
                <CircularProgress size={16} sx={{ mr: 1 }} />
              ) : (
                <LanguageIcon sx={{ mr: 1 }} />
              )
            }
          >
            {availableLanguages.map((language) => (
              <MenuItem key={language.code} value={language.code}>
                {renderLanguageItem(language)}
              </MenuItem>
            ))}
          </Select>
        </FormControl>
        {error && (
          <Alert severity="error" sx={{ mt: 1 }}>
            {error}
          </Alert>
        )}
      </Box>
    )
  }

  if (variant === 'buttons') {
    return (
      <Box className={className}>
        <ButtonGroup
          variant="outlined"
          size={size}
          disabled={disabled || loading}
          orientation="horizontal"
        >
          {availableLanguages.map((language) => (
            <Tooltip key={language.code} title={language.name}>
              <Button
                variant={language.code === currentLanguage.code ? 'contained' : 'outlined'}
                onClick={() => handleLanguageChange(language.code as SupportedLocale)}
                disabled={loading}
                startIcon={loading && language.code === currentLanguage.code ? (
                  <CircularProgress size={16} />
                ) : showFlags ? (
                  <span style={{ fontSize: '1.1em' }}>{language.flag}</span>
                ) : null}
              >
                {showNativeNames ? language.nativeName : language.name}
                {language.code === currentLanguage.code && (
                  <CheckIcon sx={{ ml: 0.5, fontSize: 16 }} />
                )}
              </Button>
            </Tooltip>
          ))}
        </ButtonGroup>
        {error && (
          <Alert severity="error" sx={{ mt: 1 }}>
            {error}
          </Alert>
        )}
      </Box>
    )
  }

  // Default: menu variant
  return (
    <Box className={className}>
      <Tooltip title={t('language.change')}>
        <Button
          aria-controls={isMenuOpen ? 'language-menu' : undefined}
          aria-haspopup="true"
          aria-expanded={isMenuOpen ? 'true' : undefined}
          onClick={handleMenuOpen}
          disabled={disabled || loading}
          size={size}
          startIcon={
            loading ? (
              <CircularProgress size={16} />
            ) : (
              <LanguageIcon />
            )
          }
          endIcon={<ArrowDownIcon />}
          variant="outlined"
          sx={{
            minWidth: 120,
            justifyContent: 'space-between',
          }}
        >
          <Box display="flex" alignItems="center" gap={0.5}>
            {showFlags && (
              <span style={{ fontSize: '1.1em' }}>
                {currentLanguage.flag}
              </span>
            )}
            {showNativeNames ? currentLanguage.nativeName : currentLanguage.name}
          </Box>
        </Button>
      </Tooltip>

      <Menu
        id="language-menu"
        anchorEl={anchorEl}
        open={isMenuOpen}
        onClose={handleMenuClose}
        MenuListProps={{
          'aria-labelledby': 'language-button',
          role: 'listbox',
        }}
        PaperProps={{
          sx: {
            minWidth: 200,
          },
        }}
      >
        {availableLanguages.map((language) => (
          <MenuItem
            key={language.code}
            selected={language.code === currentLanguage.code}
            onClick={() => handleLanguageChange(language.code as SupportedLocale)}
            disabled={loading}
          >
            <ListItemIcon>
              {language.code === currentLanguage.code ? (
                <CheckIcon color="primary" />
              ) : (
                <Box width={24} />
              )}
            </ListItemIcon>
            <ListItemText>
              {renderLanguageItem(language)}
            </ListItemText>
          </MenuItem>
        ))}
      </Menu>

      {error && (
        <Alert severity="error" sx={{ mt: 1 }}>
          {error}
        </Alert>
      )}
    </Box>
  )
}

/**
 * Compact language switcher for use in headers/navigation
 */
export const CompactLanguageSwitcher: React.FC<
  Omit<LanguageSwitcherProps, 'variant' | 'showNativeNames'>
> = (props) => (
  <LanguageSwitcher
    {...props}
    variant="menu"
    showNativeNames={false}
    size="small"
  />
)

/**
 * Language switcher with full language names
 */
export const DetailedLanguageSwitcher: React.FC<
  Omit<LanguageSwitcherProps, 'variant' | 'showFlags'>
> = (props) => (
  <LanguageSwitcher
    {...props}
    variant="select"
    showFlags={true}
    showNativeNames={true}
  />
)

export default LanguageSwitcher