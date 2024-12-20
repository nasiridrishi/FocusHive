import React from 'react';
import {styled} from '@mui/material/styles';

const StyledSkipLink = styled('a')(({theme}) => ({
  position: 'absolute',
  top: '-100px', // Move further up when hidden
  left: theme.spacing(2), // Add some left margin
  background: theme.palette.primary.main,
  color: theme.palette.primary.contrastText,
  padding: theme.spacing(1.5, 3),
  textDecoration: 'none',
  zIndex: 100000,
  borderRadius: theme.shape.borderRadius,
  boxShadow: theme.shadows[4],
  fontWeight: 500,
  transition: 'top 0.2s ease-in-out',
  '&:focus': {
    top: theme.spacing(2), // Position it properly within the viewport
    outline: `3px solid ${theme.palette.primary.light}`,
    outlineOffset: '2px',
  },
}));

interface SkipLinkProps {
  href?: string;
  children: React.ReactNode;
}

export const SkipLink: React.FC<SkipLinkProps> = ({
                                                    href = '#main-content',
                                                    children = 'Skip to main content'
                                                  }) => {
  return (
      <StyledSkipLink href={href} className="skip-link">
        {children}
      </StyledSkipLink>
  );
};

export default SkipLink;