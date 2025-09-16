import React from 'react';
import {styled} from '@mui/material/styles';

const StyledSkipLink = styled('a')(({theme}) => ({
  position: 'absolute',
  top: '-40px',
  left: 0,
  background: theme.palette.primary.main,
  color: theme.palette.primary.contrastText,
  padding: theme.spacing(1, 2),
  textDecoration: 'none',
  zIndex: 100000,
  borderRadius: '0 0 4px 0',
  '&:focus': {
    top: 0,
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