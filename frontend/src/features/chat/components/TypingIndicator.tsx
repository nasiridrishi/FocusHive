import React from 'react'
import {alpha, Avatar, Box, keyframes, styled, Typography, useTheme,} from '@mui/material'
import {TypingIndicator as TypingIndicatorType} from '../../../shared/types/chat'

const typingAnimation = keyframes`
  0%, 60%, 100% {
    opacity: 0.3;
    transform: scale(0.8);
  }
  30% {
    opacity: 1;
    transform: scale(1);
  }
`

const TypingContainer = styled(Box)(({theme}) => ({
  display: 'flex',
  alignItems: 'center',
  gap: theme.spacing(1),
  padding: theme.spacing(1, 2),
  borderRadius: theme.shape.borderRadius * 2,
  backgroundColor: alpha(theme.palette.background.paper, 0.8),
  border: `1px solid ${alpha(theme.palette.divider, 0.2)}`,
  maxWidth: 'fit-content',
}))

const TypingDotsContainer = styled(Box)(({theme}) => ({
  display: 'flex',
  gap: theme.spacing(0.5),
  alignItems: 'center',
}))

const TypingDot = styled(Box)<{ delay: number }>(({theme, delay}) => ({
  width: 6,
  height: 6,
  borderRadius: '50%',
  backgroundColor: theme.palette.text.secondary,
  animation: `${typingAnimation} 1.4s infinite`,
  animationDelay: `${delay}s`,
}))

const UserAvatars = styled(Box)(({theme}) => ({
  display: 'flex',
  marginRight: theme.spacing(0.5),
  '& .MuiAvatar-root': {
    width: 24,
    height: 24,
    marginLeft: theme.spacing(-0.5),
    border: `2px solid ${theme.palette.background.paper}`,
    fontSize: '0.7rem',
    '&:first-of-type': {
      marginLeft: 0,
    },
  },
}))

interface TypingIndicatorProps {
  typingUsers: TypingIndicatorType[]
  maxVisibleUsers?: number
  showAvatars?: boolean
  variant?: 'compact' | 'full'
  className?: string
}

const TypingIndicator: React.FC<TypingIndicatorProps> = ({
                                                           typingUsers,
                                                           maxVisibleUsers = 3,
                                                           showAvatars = true,
                                                           variant = 'full',
                                                           className,
                                                         }) => {
  const theme = useTheme()

  if (typingUsers.length === 0) {
    return null
  }

  const visibleUsers = typingUsers.slice(0, maxVisibleUsers)
  const remainingCount = typingUsers.length - maxVisibleUsers

  const getTypingText = (): string => {
    const names = visibleUsers.map(user => user.user.name)

    if (names.length === 1) {
      return `${names[0]} is typing`
    } else if (names.length === 2) {
      return `${names[0]} and ${names[1]} are typing`
    } else if (names.length === 3 && remainingCount === 0) {
      return `${names[0]}, ${names[1]} and ${names[2]} are typing`
    } else {
      const displayNames = names.slice(0, 2).join(', ')
      const totalRemaining = remainingCount + (names.length > 2 ? names.length - 2 : 0)
      return `${displayNames} and ${totalRemaining} other${totalRemaining > 1 ? 's' : ''} are typing`
    }
  }

  const getInitials = (name: string): string => {
    return name
    .split(' ')
    .map(word => word[0])
    .join('')
    .toUpperCase()
    .slice(0, 2)
  }

  if (variant === 'compact') {
    return (
        <Box className={className} sx={{display: 'flex', alignItems: 'center', gap: 1}}>
          <TypingDotsContainer>
            <TypingDot delay={0}/>
            <TypingDot delay={0.2}/>
            <TypingDot delay={0.4}/>
          </TypingDotsContainer>
          <Typography variant="caption" color="text.secondary">
            {typingUsers.length} typing
          </Typography>
        </Box>
    )
  }

  return (
      <TypingContainer className={className}>
        {showAvatars && (
            <UserAvatars>
              {visibleUsers.map((user, index) => (
                  <Avatar
                      key={user.userId}
                      src={user.user.avatar}
                      sx={{
                        zIndex: visibleUsers.length - index,
                        fontSize: '0.7rem',
                      }}
                  >
                    {!user.user.avatar && getInitials(user.user.name)}
                  </Avatar>
              ))}
              {remainingCount > 0 && (
                  <Avatar
                      sx={{
                        backgroundColor: alpha(theme.palette.primary.main, 0.2),
                        color: theme.palette.primary.main,
                        fontSize: '0.6rem',
                        fontWeight: 600,
                      }}
                  >
                    +{remainingCount}
                  </Avatar>
              )}
            </UserAvatars>
        )}

        <Box sx={{display: 'flex', alignItems: 'center', gap: 1}}>
          <Typography variant="body2" color="text.secondary" sx={{fontStyle: 'italic'}}>
            {getTypingText()}
          </Typography>

          <TypingDotsContainer>
            <TypingDot delay={0}/>
            <TypingDot delay={0.2}/>
            <TypingDot delay={0.4}/>
          </TypingDotsContainer>
        </Box>
      </TypingContainer>
  )
}

export default React.memo(TypingIndicator)

// Simplified dot-only indicator for inline use
export const TypingDots: React.FC<{ size?: number; className?: string }> = ({
                                                                              size = 6,
                                                                              className
                                                                            }) => {
  return (
      <Box className={className} sx={{display: 'flex', gap: 0.5, alignItems: 'center'}}>
        <TypingDot delay={0} sx={{width: size, height: size}}/>
        <TypingDot delay={0.2} sx={{width: size, height: size}}/>
        <TypingDot delay={0.4} sx={{width: size, height: size}}/>
      </Box>
  )
}