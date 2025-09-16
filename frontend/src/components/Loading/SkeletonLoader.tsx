import React from 'react';
import {
  Box,
  Card,
  CardContent,
  CardHeader,
  Skeleton,
  SkeletonProps,
  Stack,
  styled,
  useTheme,
} from '@mui/material';

export type SkeletonVariant =
    | 'text'
    | 'rectangular'
    | 'rounded'
    | 'circular'
    | 'list'
    | 'card'
    | 'avatar'
    | 'hive-card'
    | 'user-profile'
    | 'chat-message'
    | 'timer-display';

export interface SkeletonLoaderProps extends Omit<SkeletonProps, 'variant'> {
  /** Type of skeleton to render */
  variant?: SkeletonVariant;
  /** Number of skeleton items to render (for lists) */
  count?: number;
  /** Custom lines for text skeletons */
  lines?: number;
  /** Whether to show pulse animation */
  animated?: boolean;
  /** Custom spacing between items */
  spacing?: number;
}

// Styled components for better animations
const PulseContainer = styled(Box)(({theme}) => ({
  '& .MuiSkeleton-root': {
    '&::after': {
      animationDuration: '1.5s',
      background: `linear-gradient(90deg, transparent, ${theme.palette.action.hover}, transparent)`,
    },
  },
}));

/**
 * SkeletonLoader component with multiple predefined variants
 *
 * @example
 * ```tsx
 * // Simple text skeleton
 * <SkeletonLoader variant="text" lines={3} />
 *
 * // Hive card skeleton
 * <SkeletonLoader variant="hive-card" count={6} />
 *
 * // User profile skeleton
 * <SkeletonLoader variant="user-profile" />
 * ```
 */
export const SkeletonLoader: React.FC<SkeletonLoaderProps> = ({
                                                                variant = 'text',
                                                                count = 1,
                                                                lines = 1,
                                                                animated = true,
                                                                spacing = 2,
                                                                ...props
                                                              }) => {
  const _theme = useTheme();

  const renderSkeleton = (): React.ReactElement => {
    switch (variant) {
      case 'text':
        return (
            <Stack spacing={1}>
              {Array.from({length: lines}, (_, i) => (
                  <Skeleton
                      key={i}
                      variant="text"
                      width={i === lines - 1 ? '60%' : '100%'}
                      height={24}
                      animation={animated ? 'pulse' : false}
                      {...props}
                  />
              ))}
            </Stack>
        );

      case 'list':
        return (
            <Stack spacing={spacing}>
              {Array.from({length: count}, (_, i) => (
                  <Box key={i} display="flex" alignItems="center" gap={2}>
                    <Skeleton variant="circular" width={40} height={40}
                              animation={animated ? 'pulse' : false}/>
                    <Box flex={1}>
                      <Skeleton variant="text" width="80%" height={20}
                                animation={animated ? 'pulse' : false}/>
                      <Skeleton variant="text" width="60%" height={16}
                                animation={animated ? 'pulse' : false}/>
                    </Box>
                  </Box>
              ))}
            </Stack>
        );

      case 'card':
        return (
            <Stack spacing={spacing}>
              {Array.from({length: count}, (_, i) => (
                  <Card key={i} sx={{p: 2}}>
                    <CardHeader
                        avatar={<Skeleton variant="circular" width={40} height={40}
                                          animation={animated ? 'pulse' : false}/>}
                        title={<Skeleton variant="text" width="40%" height={24}
                                         animation={animated ? 'pulse' : false}/>}
                        subheader={<Skeleton variant="text" width="20%" height={16}
                                             animation={animated ? 'pulse' : false}/>}
                    />
                    <CardContent>
                      <Skeleton variant="rectangular" height={120}
                                animation={animated ? 'pulse' : false}/>
                      <Box mt={2}>
                        <Skeleton variant="text" width="100%" height={16}
                                  animation={animated ? 'pulse' : false}/>
                        <Skeleton variant="text" width="80%" height={16}
                                  animation={animated ? 'pulse' : false}/>
                      </Box>
                    </CardContent>
                  </Card>
              ))}
            </Stack>
        );

      case 'hive-card':
        return (
            <Stack spacing={spacing}>
              {Array.from({length: count}, (_, i) => (
                  <Card key={i} sx={{p: 3}}>
                    <Box display="flex" alignItems="center" justifyContent="space-between" mb={2}>
                      <Box flex={1}>
                        <Skeleton variant="text" width="70%" height={28}
                                  animation={animated ? 'pulse' : false}/>
                        <Skeleton variant="text" width="40%" height={20}
                                  animation={animated ? 'pulse' : false}/>
                      </Box>
                      <Skeleton variant="circular" width={56} height={56}
                                animation={animated ? 'pulse' : false}/>
                    </Box>

                    <Box display="flex" alignItems="center" gap={1} mb={2}>
                      {Array.from({length: 3}, (_, j) => (
                          <Skeleton key={j} variant="circular" width={32} height={32}
                                    animation={animated ? 'pulse' : false}/>
                      ))}
                      <Skeleton variant="text" width="80px" height={16}
                                animation={animated ? 'pulse' : false}/>
                    </Box>

                    <Stack direction="row" spacing={1} mb={2}>
                      <Skeleton variant="rounded" width={80} height={24}
                                animation={animated ? 'pulse' : false}/>
                      <Skeleton variant="rounded" width={100} height={24}
                                animation={animated ? 'pulse' : false}/>
                    </Stack>

                    <Box display="flex" justifyContent="space-between" alignItems="center">
                      <Skeleton variant="text" width="120px" height={16}
                                animation={animated ? 'pulse' : false}/>
                      <Skeleton variant="rectangular" width={120} height={36}
                                animation={animated ? 'pulse' : false}/>
                    </Box>
                  </Card>
              ))}
            </Stack>
        );

      case 'user-profile':
        return (
            <Box display="flex" alignItems="center" gap={2}>
              <Skeleton variant="circular" width={80} height={80}
                        animation={animated ? 'pulse' : false}/>
              <Box flex={1}>
                <Skeleton variant="text" width="60%" height={32}
                          animation={animated ? 'pulse' : false}/>
                <Skeleton variant="text" width="40%" height={20}
                          animation={animated ? 'pulse' : false}/>
                <Skeleton variant="text" width="30%" height={16}
                          animation={animated ? 'pulse' : false}/>
              </Box>
            </Box>
        );

      case 'avatar':
        return (
            <Stack direction="row" spacing={1}>
              {Array.from({length: count}, (_, i) => (
                  <Skeleton
                      key={i}
                      variant="circular"
                      width={40}
                      height={40}
                      animation={animated ? 'pulse' : false}
                      {...props}
                  />
              ))}
            </Stack>
        );

      case 'chat-message':
        return (
            <Stack spacing={spacing}>
              {Array.from({length: count}, (_, i) => (
                  <Box key={i} display="flex" gap={2} alignItems="flex-start">
                    <Skeleton variant="circular" width={32} height={32}
                              animation={animated ? 'pulse' : false}/>
                    <Box flex={1}>
                      <Skeleton variant="text" width="30%" height={16}
                                animation={animated ? 'pulse' : false}/>
                      <Skeleton variant="text" width="90%" height={20}
                                animation={animated ? 'pulse' : false}/>
                      <Skeleton variant="text" width="60%" height={20}
                                animation={animated ? 'pulse' : false}/>
                    </Box>
                  </Box>
              ))}
            </Stack>
        );

      case 'timer-display':
        return (
            <Box textAlign="center">
              <Skeleton variant="text" width="200px" height={64}
                        animation={animated ? 'pulse' : false} sx={{mx: 'auto', mb: 2}}/>
              <Skeleton variant="text" width="150px" height={24}
                        animation={animated ? 'pulse' : false} sx={{mx: 'auto', mb: 2}}/>
              <Box display="flex" justifyContent="center" gap={2}>
                <Skeleton variant="rectangular" width={100} height={40}
                          animation={animated ? 'pulse' : false}/>
                <Skeleton variant="rectangular" width={100} height={40}
                          animation={animated ? 'pulse' : false}/>
              </Box>
            </Box>
        );

      case 'circular':
        return (
            <Skeleton
                variant="circular"
                width={40}
                height={40}
                animation={animated ? 'pulse' : false}
                {...props}
            />
        );

      case 'rounded':
        return (
            <Skeleton
                variant="rounded"
                height={200}
                animation={animated ? 'pulse' : false}
                {...props}
            />
        );

      case 'rectangular':
      default:
        return (
            <Skeleton
                variant="rectangular"
                height={200}
                animation={animated ? 'pulse' : false}
                {...props}
            />
        );
    }
  };

  return (
      <PulseContainer>
        {renderSkeleton()}
      </PulseContainer>
  );
};

// Convenience components for common use cases
export const TextSkeleton: React.FC<Omit<SkeletonLoaderProps, 'variant'>> = (props) => (
    <SkeletonLoader variant="text" {...props} />
);

export const ListSkeleton: React.FC<Omit<SkeletonLoaderProps, 'variant'>> = (props) => (
    <SkeletonLoader variant="list" {...props} />
);

export const CardSkeleton: React.FC<Omit<SkeletonLoaderProps, 'variant'>> = (props) => (
    <SkeletonLoader variant="card" {...props} />
);

export const HiveCardSkeleton: React.FC<Omit<SkeletonLoaderProps, 'variant'>> = (props) => (
    <SkeletonLoader variant="hive-card" {...props} />
);

export const UserProfileSkeleton: React.FC<Omit<SkeletonLoaderProps, 'variant'>> = (props) => (
    <SkeletonLoader variant="user-profile" {...props} />
);

export const ChatMessageSkeleton: React.FC<Omit<SkeletonLoaderProps, 'variant'>> = (props) => (
    <SkeletonLoader variant="chat-message" {...props} />
);

export const TimerSkeleton: React.FC<Omit<SkeletonLoaderProps, 'variant'>> = (props) => (
    <SkeletonLoader variant="timer-display" {...props} />
);

// Higher-order component for conditional skeleton loading is available in './withSkeleton'

export default SkeletonLoader;