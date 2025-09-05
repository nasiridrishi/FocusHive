/**
 * Accessible Navigation Components
 * 
 * WCAG 2.1 AA compliant navigation components with proper ARIA attributes,
 * keyboard navigation, focus management, and screen reader support.
 */

import React, { forwardRef } from 'react';
import {
  AppBar,
  AppBarProps,
  Toolbar,
  ToolbarProps,
  Breadcrumbs,
  BreadcrumbsProps,
  Link,
  LinkProps,
  Menu,
  MenuProps,
  MenuItem,
  MenuItemProps,
  MenuList,
  ListItemIcon,
  ListItemText,
  Divider,
  Typography,
  Box,
  Collapse,
  List,
  ListItem,
  ListItemButton,
  IconButton,
  Chip,
  Badge,
} from '@mui/material';
import {
  ExpandLess,
  ExpandMore,
  ArrowForward as ArrowForwardIcon,
  Home as HomeIcon,
  KeyboardArrowDown,
} from '@mui/icons-material';
import { styled } from '@mui/material/styles';
import { useKeyboardNavigation } from '../hooks/useKeyboardNavigation';
import { useAnnouncement } from '../hooks/useAnnouncement';
import { ScreenReaderOnly } from './ScreenReaderOnly';
import type { AccessibleProps, AriaRole } from '../types/accessibility';

// Enhanced AppBar with accessibility features
const StyledAccessibleAppBar = styled(AppBar)(({ theme }) => ({
  '& .skip-nav-container': {
    position: 'absolute',
    top: '-40px',
    left: 0,
    zIndex: 100,
    
    '&:focus-within': {
      position: 'static',
      top: 'auto',
    },
  },
  
  '& .skip-nav-link': {
    position: 'absolute',
    left: '-10000px',
    top: 'auto',
    width: '1px',
    height: '1px',
    overflow: 'hidden',
    
    '&:focus': {
      position: 'static',
      width: 'auto',
      height: 'auto',
      padding: theme.spacing(1, 2),
      backgroundColor: theme.palette.primary.main,
      color: theme.palette.primary.contrastText,
      textDecoration: 'none',
      borderRadius: theme.shape.borderRadius,
      outline: `2px solid ${theme.palette.common.white}`,
      outlineOffset: '2px',
    },
  },
}));

export interface NavigationItem {
  id: string;
  label: string;
  href?: string;
  icon?: React.ReactNode;
  badge?: string | number;
  disabled?: boolean;
  current?: boolean;
  children?: NavigationItem[];
  'aria-label'?: string;
  'aria-describedby'?: string;
}

export interface AccessibleAppBarProps extends AppBarProps {
  /**
   * Application name or logo
   */
  title?: string;
  
  /**
   * Navigation items
   */
  navigationItems?: NavigationItem[];
  
  /**
   * Show skip navigation links
   */
  showSkipLinks?: boolean;
  
  /**
   * Skip link targets
   */
  skipLinks?: Array<{ id: string; label: string }>;
  
  /**
   * User menu component
   */
  userMenu?: React.ReactNode;
  
  /**
   * Search component
   */
  searchComponent?: React.ReactNode;
}

/**
 * Accessible App Bar with skip navigation and proper landmarks
 */
export const AccessibleAppBar = forwardRef<HTMLDivElement, AccessibleAppBarProps>(({
  title,
  navigationItems = [],
  showSkipLinks = true,
  skipLinks = [
    { id: 'main', label: 'Skip to main content' },
    { id: 'navigation', label: 'Skip to navigation' },
    { id: 'search', label: 'Skip to search' },
  ],
  userMenu,
  searchComponent,
  children,
  ...props
}, ref) => {
  return (
    <StyledAccessibleAppBar {...props} ref={ref}>
      {/* Skip Navigation Links */}
      {showSkipLinks && (
        <div className="skip-nav-container">
          {skipLinks.map((link) => (
            <a
              key={link.id}
              href={`#${link.id}`}
              className="skip-nav-link"
              onClick={(e) => {
                e.preventDefault();
                const target = document.getElementById(link.id);
                if (target) {
                  target.setAttribute('tabindex', '-1');
                  target.focus();
                  target.scrollIntoView({ behavior: 'smooth', block: 'start' });
                  setTimeout(() => target.removeAttribute('tabindex'), 0);
                }
              }}
            >
              {link.label}
            </a>
          ))}
        </div>
      )}

      <Toolbar component="nav" role="navigation" aria-label="Main navigation">
        {/* Application Title/Logo */}
        {title && (
          <Typography
            variant="h1"
            component="div"
            sx={{ 
              flexGrow: 0,
              mr: 4,
              fontSize: '1.25rem',
              fontWeight: 'bold',
            }}
          >
            {title}
          </Typography>
        )}

        {/* Main Navigation */}
        {navigationItems.length > 0 && (
          <Box sx={{ flexGrow: 1, display: 'flex', alignItems: 'center' }}>
            <AccessibleNavigation
              items={navigationItems}
              orientation="horizontal"
              role="menubar"
            />
          </Box>
        )}

        {/* Search */}
        {searchComponent && (
          <Box sx={{ mr: 2 }} id="search">
            {searchComponent}
          </Box>
        )}

        {/* User Menu */}
        {userMenu && (
          <Box>
            {userMenu}
          </Box>
        )}

        {/* Custom Content */}
        {children}
      </Toolbar>
    </StyledAccessibleAppBar>
  );
});

AccessibleAppBar.displayName = 'AccessibleAppBar';

/**
 * Accessible Navigation List Component
 */
export interface AccessibleNavigationProps {
  /**
   * Navigation items
   */
  items: NavigationItem[];
  
  /**
   * Orientation
   */
  orientation?: 'horizontal' | 'vertical';
  
  /**
   * ARIA role
   */
  role?: AriaRole;
  
  /**
   * Navigation label
   */
  'aria-label'?: string;
  
  /**
   * Allow multiple expanded items (for vertical navigation)
   */
  allowMultipleExpanded?: boolean;
  
  /**
   * Item click handler
   */
  onItemClick?: (item: NavigationItem) => void;
}

export const AccessibleNavigation: React.FC<AccessibleNavigationProps> = ({
  items,
  orientation = 'vertical',
  role = 'navigation',
  'aria-label': ariaLabel = 'Navigation menu',
  allowMultipleExpanded = false,
  onItemClick,
}) => {
  const [expandedItems, setExpandedItems] = React.useState<Set<string>>(new Set());
  const { keyboardNavigationProps } = useKeyboardNavigation({
    orientation,
    wrap: true,
    activateOnFocus: false,
  });

  const handleItemClick = (item: NavigationItem) => {
    if (item.children) {
      setExpandedItems(prev => {
        const newExpanded = new Set(prev);
        if (newExpanded.has(item.id)) {
          newExpanded.delete(item.id);
        } else {
          if (!allowMultipleExpanded) {
            newExpanded.clear();
          }
          newExpanded.add(item.id);
        }
        return newExpanded;
      });
    }
    
    onItemClick?.(item);
  };

  const renderNavigationItem = (item: NavigationItem, level = 0) => {
    const hasChildren = item.children && item.children.length > 0;
    const isExpanded = expandedItems.has(item.id);
    const isCurrent = item.current;

    const itemContent = (
      <Box
        sx={{
          display: 'flex',
          alignItems: 'center',
          width: '100%',
          gap: 1,
          pl: level * 2,
        }}
      >
        {item.icon && (
          <ListItemIcon sx={{ minWidth: 'auto' }}>
            {item.icon}
          </ListItemIcon>
        )}
        
        <ListItemText 
          primary={item.label}
          sx={{ 
            '& .MuiListItemText-primary': {
              fontWeight: isCurrent ? 'bold' : 'normal',
            },
          }}
        />
        
        {item.badge && (
          <Badge
            badgeContent={item.badge}
            color="primary"
            sx={{ ml: 1 }}
          />
        )}
        
        {hasChildren && (
          <>
            <ScreenReaderOnly>
              {isExpanded ? 'expanded' : 'collapsed'}
            </ScreenReaderOnly>
            {isExpanded ? <ExpandLess /> : <ExpandMore />}
          </>
        )}
        
        {isCurrent && (
          <ScreenReaderOnly>
            current page
          </ScreenReaderOnly>
        )}
      </Box>
    );

    return (
      <React.Fragment key={item.id}>
        <ListItem disablePadding>
          {item.href && !hasChildren ? (
            <ListItemButton
              component={Link}
              href={item.href}
              disabled={item.disabled}
              selected={isCurrent}
              onClick={() => handleItemClick(item)}
              aria-label={item['aria-label']}
              aria-describedby={item['aria-describedby']}
              aria-current={isCurrent ? 'page' : undefined}
              sx={{
                '&:focus': {
                  outline: '2px solid',
                  outlineColor: 'primary.main',
                  outlineOffset: '2px',
                },
              }}
            >
              {itemContent}
            </ListItemButton>
          ) : (
            <ListItemButton
              disabled={item.disabled}
              onClick={() => handleItemClick(item)}
              aria-expanded={hasChildren ? isExpanded : undefined}
              aria-controls={hasChildren ? `submenu-${item.id}` : undefined}
              aria-label={item['aria-label']}
              aria-describedby={item['aria-describedby']}
              aria-current={isCurrent ? 'page' : undefined}
              sx={{
                '&:focus': {
                  outline: '2px solid',
                  outlineColor: 'primary.main',
                  outlineOffset: '2px',
                },
              }}
            >
              {itemContent}
            </ListItemButton>
          )}
        </ListItem>
        
        {/* Submenu */}
        {hasChildren && (
          <Collapse 
            in={isExpanded} 
            timeout="auto" 
            unmountOnExit
            id={`submenu-${item.id}`}
          >
            <List component="div" disablePadding role="group">
              {item.children!.map((child) => renderNavigationItem(child, level + 1))}
            </List>
          </Collapse>
        )}
      </React.Fragment>
    );
  };

  return (
    <Box
      component="nav"
      role={role}
      aria-label={ariaLabel}
      {...keyboardNavigationProps}
    >
      <List
        sx={{
          width: '100%',
          display: orientation === 'horizontal' ? 'flex' : 'block',
          flexDirection: orientation === 'horizontal' ? 'row' : 'column',
          gap: orientation === 'horizontal' ? 2 : 0,
        }}
      >
        {items.map((item) => renderNavigationItem(item))}
      </List>
    </Box>
  );
};

/**
 * Accessible Breadcrumb Component
 */
export interface AccessibleBreadcrumbsProps extends BreadcrumbsProps {
  /**
   * Breadcrumb items
   */
  items: Array<{
    label: string;
    href?: string;
    current?: boolean;
    icon?: React.ReactNode;
  }>;
  
  /**
   * Home icon for first breadcrumb
   */
  showHomeIcon?: boolean;
  
  /**
   * Current page label for screen readers
   */
  currentPageLabel?: string;
}

export const AccessibleBreadcrumbs = forwardRef<HTMLNavElement, AccessibleBreadcrumbsProps>(({
  items,
  showHomeIcon = true,
  currentPageLabel = 'current page',
  ...props
}, ref) => {
  return (
    <nav ref={ref} aria-label="Breadcrumb">
      <Breadcrumbs
        {...props}
        aria-label="Breadcrumb navigation"
        separator={<ArrowForwardIcon fontSize="small" />}
      >
        {items.map((item, index) => {
          const isLast = index === items.length - 1;
          const isCurrent = item.current || isLast;

          if (item.href && !isCurrent) {
            return (
              <Link
                key={index}
                href={item.href}
                color="inherit"
                sx={{
                  display: 'flex',
                  alignItems: 'center',
                  gap: 0.5,
                  textDecoration: 'none',
                  '&:hover': {
                    textDecoration: 'underline',
                  },
                  '&:focus': {
                    outline: '2px solid',
                    outlineColor: 'primary.main',
                    outlineOffset: '2px',
                    borderRadius: 1,
                  },
                }}
              >
                {index === 0 && showHomeIcon && <HomeIcon fontSize="small" />}
                {item.icon && index > 0 && item.icon}
                {item.label}
              </Link>
            );
          }

          return (
            <Typography
              key={index}
              color="text.primary"
              aria-current={isCurrent ? 'page' : undefined}
              sx={{
                display: 'flex',
                alignItems: 'center',
                gap: 0.5,
                fontWeight: isCurrent ? 'bold' : 'normal',
              }}
            >
              {index === 0 && showHomeIcon && <HomeIcon fontSize="small" />}
              {item.icon && index > 0 && item.icon}
              {item.label}
              {isCurrent && (
                <ScreenReaderOnly>
                  , {currentPageLabel}
                </ScreenReaderOnly>
              )}
            </Typography>
          );
        })}
      </Breadcrumbs>
    </nav>
  );
});

AccessibleBreadcrumbs.displayName = 'AccessibleBreadcrumbs';

/**
 * Accessible Dropdown Menu Component
 */
export interface AccessibleMenuProps extends Omit<MenuProps, 'children'> {
  /**
   * Menu trigger element
   */
  trigger: React.ReactElement;
  
  /**
   * Menu items
   */
  items: Array<{
    id: string;
    label: string;
    icon?: React.ReactNode;
    disabled?: boolean;
    divider?: boolean;
    onClick?: () => void;
    href?: string;
    shortcut?: string;
    description?: string;
  }>;
  
  /**
   * Menu label for screen readers
   */
  menuLabel?: string;
}

export const AccessibleMenu: React.FC<AccessibleMenuProps> = ({
  trigger,
  items,
  menuLabel = 'Menu',
  ...props
}) => {
  const [anchorEl, setAnchorEl] = React.useState<null | HTMLElement>(null);
  const { announcePolite } = useAnnouncement();
  
  const open = Boolean(anchorEl);
  const menuId = React.useId();

  const handleClick = (event: React.MouseEvent<HTMLElement>) => {
    setAnchorEl(event.currentTarget);
    announcePolite(`${menuLabel} opened`);
  };

  const handleClose = () => {
    setAnchorEl(null);
    announcePolite(`${menuLabel} closed`);
  };

  const handleItemClick = (item: typeof items[0]) => {
    if (!item.disabled) {
      item.onClick?.();
      handleClose();
    }
  };

  const enhancedTrigger = React.cloneElement(trigger, {
    'aria-controls': open ? menuId : undefined,
    'aria-haspopup': 'true',
    'aria-expanded': open,
    'aria-label': trigger.props['aria-label'] || `${menuLabel} button`,
    onClick: handleClick,
  });

  return (
    <>
      {enhancedTrigger}
      
      <Menu
        {...props}
        id={menuId}
        anchorEl={anchorEl}
        open={open}
        onClose={handleClose}
        MenuListProps={{
          'aria-labelledby': trigger.props.id,
          role: 'menu',
          ...props.MenuListProps,
        }}
        PaperProps={{
          ...props.PaperProps,
          sx: {
            '& .MuiMenuItem-root': {
              '&:focus': {
                outline: '2px solid',
                outlineColor: 'primary.main',
                outlineOffset: '-2px',
              },
            },
            ...props.PaperProps?.sx,
          },
        }}
      >
        {items.map((item) => {
          if (item.divider) {
            return <Divider key={item.id} />;
          }

          return (
            <MenuItem
              key={item.id}
              onClick={() => handleItemClick(item)}
              disabled={item.disabled}
              component={item.href ? Link : 'li'}
              href={item.href}
              role="menuitem"
              aria-describedby={item.description ? `${item.id}-desc` : undefined}
            >
              {item.icon && (
                <ListItemIcon>
                  {item.icon}
                </ListItemIcon>
              )}
              
              <ListItemText>
                {item.label}
              </ListItemText>
              
              {item.shortcut && (
                <Chip
                  label={item.shortcut}
                  size="small"
                  variant="outlined"
                  sx={{ ml: 2, fontSize: '0.75rem' }}
                />
              )}
              
              {/* Hidden description for screen readers */}
              {item.description && (
                <ScreenReaderOnly id={`${item.id}-desc`}>
                  {item.description}
                </ScreenReaderOnly>
              )}
            </MenuItem>
          );
        })}
      </Menu>
    </>
  );
};

/**
 * Accessible Tab Navigation Component
 */
export interface AccessibleTabsProps {
  /**
   * Tab items
   */
  tabs: Array<{
    id: string;
    label: string;
    content: React.ReactNode;
    disabled?: boolean;
    badge?: string | number;
    icon?: React.ReactNode;
  }>;
  
  /**
   * Active tab ID
   */
  activeTab?: string;
  
  /**
   * Tab change handler
   */
  onChange?: (tabId: string) => void;
  
  /**
   * Orientation
   */
  orientation?: 'horizontal' | 'vertical';
  
  /**
   * Tab list label
   */
  tabsLabel?: string;
}

export const AccessibleTabs: React.FC<AccessibleTabsProps> = ({
  tabs,
  activeTab,
  onChange,
  orientation = 'horizontal',
  tabsLabel = 'Tabs',
}) => {
  const [selectedTab, setSelectedTab] = React.useState(activeTab || tabs[0]?.id);
  const { keyboardNavigationProps } = useKeyboardNavigation({
    orientation,
    wrap: true,
    activateOnFocus: true,
  });

  const handleTabChange = (tabId: string) => {
    setSelectedTab(tabId);
    onChange?.(tabId);
  };

  const activeTabContent = tabs.find(tab => tab.id === selectedTab)?.content;

  return (
    <Box sx={{ display: 'flex', flexDirection: orientation === 'vertical' ? 'row' : 'column' }}>
      {/* Tab List */}
      <Box
        role="tablist"
        aria-label={tabsLabel}
        aria-orientation={orientation}
        {...keyboardNavigationProps}
        sx={{
          display: 'flex',
          flexDirection: orientation === 'vertical' ? 'column' : 'row',
          borderBottom: orientation === 'horizontal' ? 1 : 0,
          borderRight: orientation === 'vertical' ? 1 : 0,
          borderColor: 'divider',
          minWidth: orientation === 'vertical' ? 200 : 'auto',
        }}
      >
        {tabs.map((tab, index) => (
          <Box
            key={tab.id}
            component="button"
            role="tab"
            aria-selected={selectedTab === tab.id}
            aria-controls={`tabpanel-${tab.id}`}
            id={`tab-${tab.id}`}
            tabIndex={selectedTab === tab.id ? 0 : -1}
            disabled={tab.disabled}
            onClick={() => handleTabChange(tab.id)}
            sx={{
              display: 'flex',
              alignItems: 'center',
              gap: 1,
              px: 2,
              py: 1.5,
              backgroundColor: 'transparent',
              border: 'none',
              borderBottom: orientation === 'horizontal' && selectedTab === tab.id ? 2 : 0,
              borderRight: orientation === 'vertical' && selectedTab === tab.id ? 2 : 0,
              borderColor: 'primary.main',
              cursor: tab.disabled ? 'not-allowed' : 'pointer',
              opacity: tab.disabled ? 0.6 : 1,
              color: selectedTab === tab.id ? 'primary.main' : 'text.primary',
              fontWeight: selectedTab === tab.id ? 'bold' : 'normal',
              '&:hover:not(:disabled)': {
                backgroundColor: 'action.hover',
              },
              '&:focus': {
                outline: '2px solid',
                outlineColor: 'primary.main',
                outlineOffset: '2px',
                backgroundColor: 'action.focus',
              },
            }}
          >
            {tab.icon}
            {tab.label}
            {tab.badge && (
              <Badge badgeContent={tab.badge} color="primary" />
            )}
          </Box>
        ))}
      </Box>

      {/* Tab Panel */}
      <Box
        role="tabpanel"
        id={`tabpanel-${selectedTab}`}
        aria-labelledby={`tab-${selectedTab}`}
        tabIndex={0}
        sx={{
          flex: 1,
          p: 3,
          '&:focus': {
            outline: '2px solid',
            outlineColor: 'primary.main',
            outlineOffset: '2px',
          },
        }}
      >
        {activeTabContent}
      </Box>
    </Box>
  );
};

export default AccessibleNavigation;