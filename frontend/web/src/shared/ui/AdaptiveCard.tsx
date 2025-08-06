/**
 * Adaptive Card Component
 * 
 * Intelligent card component that adapts its layout, content density,
 * and interactions based on screen size and container dimensions
 */

import React, { forwardRef } from 'react'
import {
  Card,
  CardProps,
  CardContent,
  CardActions,
  CardMedia,
  Typography,
  IconButton,
  Button,
  Chip,
  Box,
  Skeleton,
  useTheme,
} from '@mui/material'
import { styled } from '@mui/material/styles'
import { useContainerQuery, useResponsive, useLazyImage } from '../hooks'

// Styled adaptive card with container queries
const StyledCard = styled(Card, {
  shouldForwardProp: (prop) => !['density', 'interactive', 'adaptiveLayout'].includes(prop as string),
})<{
  density?: 'compact' | 'normal' | 'spacious'
  interactive?: boolean
  adaptiveLayout?: boolean
}>(({ theme, density = 'normal', interactive = false, adaptiveLayout = false }) => {
  const densityMap = {
    compact: { padding: theme.spacing(1.5), borderRadius: theme.shape.borderRadius * 0.75 },
    normal: { padding: theme.spacing(2), borderRadius: theme.shape.borderRadius },
    spacious: { padding: theme.spacing(3), borderRadius: theme.shape.borderRadius * 1.25 },
  }
  
  return {
    transition: 'all 0.3s cubic-bezier(0.4, 0, 0.2, 1)',
    position: 'relative',
    overflow: 'hidden',
    
    // Container query support
    ...(adaptiveLayout && {
      containerType: 'inline-size',
      
      // Adapt layout based on container width
      '@container (max-width: 300px)': {
        '& .card-media': {
          height: '120px',
        },
        '& .card-content': {
          padding: theme.spacing(1.5),
        },
        '& .card-title': {
          fontSize: '1rem',
          lineHeight: 1.3,
        },
        '& .card-description': {
          fontSize: '0.875rem',
          lineHeight: 1.4,
          WebkitLineClamp: 2,
        },
      },
      
      '@container (min-width: 300px) and (max-width: 500px)': {
        '& .card-media': {
          height: '160px',
        },
        '& .card-content': {
          padding: theme.spacing(2),
        },
        '& .card-title': {
          fontSize: '1.125rem',
          lineHeight: 1.4,
        },
        '& .card-description': {
          fontSize: '0.875rem',
          lineHeight: 1.5,
          WebkitLineClamp: 3,
        },
      },
      
      '@container (min-width: 500px)': {
        '& .card-media': {
          height: '200px',
        },
        '& .card-content': {
          padding: theme.spacing(2.5),
        },
        '& .card-title': {
          fontSize: '1.25rem',
          lineHeight: 1.4,
        },
        '& .card-description': {
          fontSize: '1rem',
          lineHeight: 1.6,
          WebkitLineClamp: 4,
        },
      },
    }),
    
    // Interactive states
    ...(interactive && {
      cursor: 'pointer',
      '&:hover': {
        transform: 'translateY(-4px)',
        boxShadow: theme.shadows[8],
        '& .card-media': {
          transform: 'scale(1.05)',
        },
      },
      '&:active': {
        transform: 'translateY(-2px)',
        boxShadow: theme.shadows[4],
      },
    }),
    
    // Density-based styling
    ...densityMap[density],
  }
})

// Props interfaces
interface AdaptiveCardProps extends Omit<CardProps, 'title'> {
  // Content props
  title?: string
  subtitle?: string
  description?: string
  image?: string
  imageAlt?: string
  
  // Layout props
  variant?: 'standard' | 'horizontal' | 'minimal' | 'featured'
  density?: 'compact' | 'normal' | 'spacious'
  aspectRatio?: string
  
  // Behavior props
  interactive?: boolean
  loading?: boolean
  adaptiveLayout?: boolean
  
  // Action props
  actions?: React.ReactNode
  primaryAction?: {
    label: string
    onClick: () => void
    disabled?: boolean
  }
  secondaryActions?: Array<{
    icon: React.ReactNode
    label: string
    onClick: () => void
  }>
  
  // Badge/chip props
  badge?: {
    label: string
    color?: 'primary' | 'secondary' | 'success' | 'warning' | 'error'
    variant?: 'filled' | 'outlined'
  }
  
  // Event handlers
  onCardClick?: () => void
  onImageClick?: () => void
}

// Main AdaptiveCard component
export const AdaptiveCard = forwardRef<HTMLDivElement, AdaptiveCardProps>(
  ({
    title,
    subtitle,
    description,
    image,
    imageAlt,
    variant = 'standard',
    density = 'normal',
    aspectRatio = '16/9',
    interactive = false,
    loading = false,
    adaptiveLayout = true,
    actions,
    primaryAction,
    secondaryActions,
    badge,
    onCardClick,
    onImageClick,
    children,
    ...props
  }, ref) => {
    const theme = useTheme()
    const { isMobile } = useResponsive()
    const { containerRef, currentBreakpoint } = useContainerQuery()
    const { src: imageSrc, isLoaded: imageLoaded, elementRef: imageRef } = useLazyImage(image || '')
    
    // Combine refs for container queries
    const combinedRef = React.useCallback((node: HTMLDivElement) => {
      if (containerRef) containerRef.current = node
      if (ref) {
        if (typeof ref === 'function') ref(node)
        else ref.current = node
      }
    }, [containerRef, ref])
    
    // Adaptive density based on screen size
    const adaptiveDensity = React.useMemo(() => {
      if (isMobile && density === 'spacious') return 'normal'
      if (currentBreakpoint === 'xs' && density !== 'compact') return 'compact'
      return density
    }, [isMobile, currentBreakpoint, density])
    
    // Loading skeleton
    if (loading) {
      return (
        <StyledCard
          ref={combinedRef}
          density={adaptiveDensity}
          adaptiveLayout={adaptiveLayout}
          {...props}
        >
          {image && (
            <Skeleton
              variant="rectangular"
              height={variant === 'horizontal' ? 140 : 200}
              className="card-media"
            />
          )}
          <CardContent className="card-content">
            <Skeleton variant="text" height={28} width="80%" />
            {subtitle && <Skeleton variant="text" height={20} width="60%" />}
            <Skeleton variant="text" height={60} />
          </CardContent>
        </StyledCard>
      )
    }
    
    // Render content based on variant
    const renderCardContent = () => {
      switch (variant) {
        case 'horizontal':
          return (
            <Box sx={{ display: 'flex', flexDirection: { xs: 'column', sm: 'row' } }}>
              {image && (
                <CardMedia
                  ref={imageRef}
                  component="img"
                  image={imageSrc}
                  alt={imageAlt || title}
                  className="card-media"
                  onClick={onImageClick}
                  sx={{
                    width: { xs: '100%', sm: 200 },
                    height: { xs: 160, sm: 140 },
                    objectFit: 'cover',
                    cursor: onImageClick ? 'pointer' : 'default',
                    transition: 'transform 0.3s ease',
                    opacity: imageLoaded ? 1 : 0,
                  }}
                />
              )}
              <Box sx={{ flex: 1 }}>
                <CardContent className="card-content">
                  {renderCardText()}
                </CardContent>
                {renderCardActions()}
              </Box>
            </Box>
          )
        
        case 'minimal':
          return (
            <>
              <CardContent className="card-content">
                {renderCardText()}
              </CardContent>
              {renderCardActions()}
            </>
          )
        
        case 'featured':
          return (
            <>
              {image && (
                <Box sx={{ position: 'relative' }}>
                  <CardMedia
                    ref={imageRef}
                    component="img"
                    image={imageSrc}
                    alt={imageAlt || title}
                    className="card-media"
                    onClick={onImageClick}
                    sx={{
                      height: 240,
                      objectFit: 'cover',
                      cursor: onImageClick ? 'pointer' : 'default',
                      transition: 'transform 0.3s ease',
                      opacity: imageLoaded ? 1 : 0,
                    }}
                  />
                  {badge && (
                    <Chip
                      label={badge.label}
                      color={badge.color}
                      variant={badge.variant}
                      size="small"
                      sx={{
                        position: 'absolute',
                        top: theme.spacing(1),
                        right: theme.spacing(1),
                      }}
                    />
                  )}
                </Box>
              )}
              <CardContent className="card-content">
                {renderCardText()}
              </CardContent>
              {renderCardActions()}
            </>
          )
        
        default: // standard
          return (
            <>
              {image && (
                <CardMedia
                  ref={imageRef}
                  component="img"
                  image={imageSrc}
                  alt={imageAlt || title}
                  className="card-media"
                  onClick={onImageClick}
                  sx={{
                    aspectRatio,
                    objectFit: 'cover',
                    cursor: onImageClick ? 'pointer' : 'default',
                    transition: 'transform 0.3s ease',
                    opacity: imageLoaded ? 1 : 0,
                  }}
                />
              )}
              <CardContent className="card-content">
                {renderCardText()}
              </CardContent>
              {renderCardActions()}
            </>
          )
      }
    }
    
    const renderCardText = () => (
      <>
        {badge && variant !== 'featured' && (
          <Box sx={{ mb: 1 }}>
            <Chip
              label={badge.label}
              color={badge.color}
              variant={badge.variant}
              size="small"
            />
          </Box>
        )}
        
        {title && (
          <Typography
            variant="h6"
            component="h3"
            className="card-title"
            sx={{
              fontWeight: 600,
              marginBottom: subtitle ? 0.5 : 1,
              display: '-webkit-box',
              WebkitBoxOrient: 'vertical',
              overflow: 'hidden',
              WebkitLineClamp: 2,
            }}
          >
            {title}
          </Typography>
        )}
        
        {subtitle && (
          <Typography
            variant="body2"
            color="text.secondary"
            sx={{ marginBottom: 1 }}
          >
            {subtitle}
          </Typography>
        )}
        
        {description && (
          <Typography
            variant="body2"
            color="text.secondary"
            className="card-description"
            sx={{
              display: '-webkit-box',
              WebkitBoxOrient: 'vertical',
              overflow: 'hidden',
              WebkitLineClamp: 3,
            }}
          >
            {description}
          </Typography>
        )}
        
        {children}
      </>
    )
    
    const renderCardActions = () => {
      if (!actions && !primaryAction && !secondaryActions) return null
      
      return (
        <CardActions sx={{ padding: theme.spacing(1, 2, 2) }}>
          {primaryAction && (
            <Button
              size="small"
              variant="contained"
              onClick={primaryAction.onClick}
              disabled={primaryAction.disabled}
            >
              {primaryAction.label}
            </Button>
          )}
          
          {secondaryActions?.map((action, index) => (
            <IconButton
              key={index}
              size="small"
              onClick={action.onClick}
              aria-label={action.label}
            >
              {action.icon}
            </IconButton>
          ))}
          
          {actions}
        </CardActions>
      )
    }
    
    return (
      <StyledCard
        ref={combinedRef}
        density={adaptiveDensity}
        interactive={interactive}
        adaptiveLayout={adaptiveLayout}
        onClick={interactive ? onCardClick : undefined}
        {...props}
      >
        {renderCardContent()}
      </StyledCard>
    )
  }
)

AdaptiveCard.displayName = 'AdaptiveCard'

// Pre-configured card variants
export const ProductCard: React.FC<Omit<AdaptiveCardProps, 'variant' | 'badge'> & {
  price?: string
  originalPrice?: string
  discount?: string
}> = ({ price, originalPrice, discount, ...props }) => (
  <AdaptiveCard
    variant="featured"
    badge={discount ? { label: discount, color: 'error' } : undefined}
    {...props}
  >
    {price && (
      <Box sx={{ display: 'flex', alignItems: 'center', gap: 1, mt: 1 }}>
        <Typography variant="h6" color="primary" fontWeight={600}>
          {price}
        </Typography>
        {originalPrice && (
          <Typography
            variant="body2"
            color="text.secondary"
            sx={{ textDecoration: 'line-through' }}
          >
            {originalPrice}
          </Typography>
        )}
      </Box>
    )}
  </AdaptiveCard>
)

export const ArticleCard: React.FC<Omit<AdaptiveCardProps, 'variant'> & {
  author?: string
  publishDate?: string
  readTime?: string
  category?: string
}> = ({ author, publishDate, readTime, category, ...props }) => (
  <AdaptiveCard
    variant="standard"
    badge={category ? { label: category, color: 'primary', variant: 'outlined' } : undefined}
    {...props}
  >
    {(author || publishDate || readTime) && (
      <Box sx={{ display: 'flex', alignItems: 'center', gap: 2, mt: 2 }}>
        {author && (
          <Typography variant="caption" color="text.secondary">
            By {author}
          </Typography>
        )}
        {publishDate && (
          <Typography variant="caption" color="text.secondary">
            {publishDate}
          </Typography>
        )}
        {readTime && (
          <Typography variant="caption" color="text.secondary">
            {readTime} read
          </Typography>
        )}
      </Box>
    )}
  </AdaptiveCard>
)

export const ProfileCard: React.FC<Omit<AdaptiveCardProps, 'variant' | 'image'> & {
  avatar?: string
  status?: 'online' | 'offline' | 'away' | 'busy'
  role?: string
  stats?: Array<{ label: string; value: string | number }>
}> = ({ avatar, status, role, stats, title, ...props }) => (
  <AdaptiveCard
    variant="minimal"
    {...props}
  >
    <Box sx={{ display: 'flex', alignItems: 'center', gap: 2, mb: 2 }}>
      <Box sx={{ position: 'relative' }}>
        <img
          src={avatar}
          alt={title}
          style={{
            width: 56,
            height: 56,
            borderRadius: '50%',
            objectFit: 'cover',
          }}
        />
        {status && (
          <Box
            sx={{
              position: 'absolute',
              bottom: 0,
              right: 0,
              width: 16,
              height: 16,
              borderRadius: '50%',
              border: '2px solid white',
              backgroundColor: 
                status === 'online' ? 'success.main' :
                status === 'away' ? 'warning.main' :
                status === 'busy' ? 'error.main' : 'action.disabled',
            }}
          />
        )}
      </Box>
      <Box sx={{ flex: 1, minWidth: 0 }}>
        <Typography variant="subtitle1" fontWeight={600} noWrap>
          {title}
        </Typography>
        {role && (
          <Typography variant="body2" color="text.secondary" noWrap>
            {role}
          </Typography>
        )}
      </Box>
    </Box>
    
    {stats && (
      <Box sx={{ display: 'flex', gap: 2 }}>
        {stats.map((stat, index) => (
          <Box key={index} sx={{ textAlign: 'center', flex: 1 }}>
            <Typography variant="h6" fontWeight={600}>
              {stat.value}
            </Typography>
            <Typography variant="caption" color="text.secondary">
              {stat.label}
            </Typography>
          </Box>
        ))}
      </Box>
    )}
  </AdaptiveCard>
)

export default AdaptiveCard