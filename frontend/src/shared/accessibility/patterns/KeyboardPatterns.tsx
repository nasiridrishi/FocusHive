/**
 * Keyboard Navigation Patterns
 * 
 * Implementation of common WCAG 2.1 AA keyboard navigation patterns
 * for complex components like data grids, tree views, and carousels.
 */

import React, { forwardRef } from 'react';
import { Box, Typography, Card, CardContent, IconButton, Button } from '@mui/material';
import { 
  KeyboardArrowLeft, 
  KeyboardArrowRight, 
  KeyboardArrowUp, 
  KeyboardArrowDown,
  ExpandMore,
  ExpandLess,
  Folder,
  FolderOpen,
  Description,
} from '@mui/icons-material';
import { styled } from '@mui/material/styles';
import { useKeyboardNavigation } from '../hooks/useKeyboardNavigation';
import { useAnnouncement } from '../hooks/useAnnouncement';
import { ScreenReaderOnly } from '../components/ScreenReaderOnly';

// Styled components
const FocusableContainer = styled(Box)(({ theme }) => ({
  '&:focus': {
    outline: `2px solid ${theme.palette.primary.main}`,
    outlineOffset: '2px',
  },
  
  '&:focus-visible': {
    outline: `2px solid ${theme.palette.primary.main}`,
    outlineOffset: '2px',
  },
  
  // High contrast mode
  '@media (prefers-contrast: high)': {
    '&:focus': {
      outline: '3px solid currentColor',
    },
  },
}));

/**
 * Accessible Data Grid Pattern
 */
export interface DataGridProps {
  /**
   * Grid data
   */
  data: Array<Record<string, any>>;
  
  /**
   * Column definitions
   */
  columns: Array<{
    key: string;
    header: string;
    sortable?: boolean;
    width?: number;
  }>;
  
  /**
   * Grid caption for screen readers
   */
  caption?: string;
  
  /**
   * Selection mode
   */
  selectionMode?: 'none' | 'single' | 'multiple';
  
  /**
   * Selected rows
   */
  selectedRows?: Set<number>;
  
  /**
   * Selection change handler
   */
  onSelectionChange?: (selectedRows: Set<number>) => void;
}

export const AccessibleDataGrid: React.FC<DataGridProps> = ({
  data,
  columns,
  caption = 'Data grid',
  selectionMode = 'none',
  selectedRows = new Set(),
  onSelectionChange,
}) => {
  const [focusedCell, setFocusedCell] = React.useState({ row: 0, col: 0 });
  const [sortColumn, setSortColumn] = React.useState<string | null>(null);
  const [sortDirection, setSortDirection] = React.useState<'asc' | 'desc'>('asc');
  
  const { announcePolite } = useAnnouncement();
  
  const gridRef = React.useRef<HTMLDivElement>(null);

  // Handle keyboard navigation
  const handleKeyDown = (event: React.KeyboardEvent) => {
    const { row, col } = focusedCell;
    let newRow = row;
    let newCol = col;

    switch (event.key) {
      case 'ArrowDown':
        newRow = Math.min(row + 1, data.length - 1);
        event.preventDefault();
        break;
      case 'ArrowUp':
        newRow = Math.max(row - 1, 0);
        event.preventDefault();
        break;
      case 'ArrowRight':
        newCol = Math.min(col + 1, columns.length - 1);
        event.preventDefault();
        break;
      case 'ArrowLeft':
        newCol = Math.max(col - 1, 0);
        event.preventDefault();
        break;
      case 'Home':
        if (event.ctrlKey) {
          newRow = 0;
          newCol = 0;
        } else {
          newCol = 0;
        }
        event.preventDefault();
        break;
      case 'End':
        if (event.ctrlKey) {
          newRow = data.length - 1;
          newCol = columns.length - 1;
        } else {
          newCol = columns.length - 1;
        }
        event.preventDefault();
        break;
      case ' ':
        if (selectionMode !== 'none') {
          handleRowSelection(row, event.shiftKey);
          event.preventDefault();
        }
        break;
      case 'Enter':
        announcePolite(`Row ${row + 1}, ${columns[col].header}: ${data[row][columns[col].key]}`);
        event.preventDefault();
        break;
    }

    if (newRow !== row || newCol !== col) {
      setFocusedCell({ row: newRow, col: newCol });
      const cellValue = data[newRow][columns[newCol].key];
      announcePolite(`${columns[newCol].header}: ${cellValue}`);
    }
  };

  const handleRowSelection = (rowIndex: number, extend: boolean = false) => {
    if (selectionMode === 'none') return;

    const newSelection = new Set(selectedRows);
    
    if (selectionMode === 'single') {
      newSelection.clear();
      newSelection.add(rowIndex);
    } else if (selectionMode === 'multiple') {
      if (newSelection.has(rowIndex)) {
        newSelection.delete(rowIndex);
      } else {
        newSelection.add(rowIndex);
      }
    }
    
    onSelectionChange?.(newSelection);
    announcePolite(`Row ${rowIndex + 1} ${newSelection.has(rowIndex) ? 'selected' : 'deselected'}`);
  };

  const handleSort = (columnKey: string) => {
    const newDirection = sortColumn === columnKey && sortDirection === 'asc' ? 'desc' : 'asc';
    setSortColumn(columnKey);
    setSortDirection(newDirection);
    announcePolite(`Table sorted by ${columnKey} ${newDirection}ending`);
  };

  return (
    <FocusableContainer
      ref={gridRef}
      role="grid"
      aria-label={caption}
      aria-rowcount={data.length + 1}
      aria-colcount={columns.length}
      tabIndex={0}
      onKeyDown={handleKeyDown}
      sx={{ 
        border: 1, 
        borderColor: 'divider',
        '&:focus': {
          outline: '2px solid',
          outlineColor: 'primary.main',
          outlineOffset: '2px',
        },
      }}
    >
      {/* Screen reader instructions */}
      <ScreenReaderOnly>
        Use arrow keys to navigate cells. Press Space to select rows. Press Enter for cell details.
      </ScreenReaderOnly>

      {/* Header row */}
      <Box role="row" aria-rowindex={1} sx={{ display: 'flex', bgcolor: 'grey.100' }}>
        {columns.map((column, colIndex) => (
          <Box
            key={column.key}
            role="columnheader"
            aria-colindex={colIndex + 1}
            aria-sort={
              sortColumn === column.key 
                ? (sortDirection === 'asc' ? 'ascending' : 'descending')
                : 'none'
            }
            sx={{
              p: 1,
              flex: column.width ? `0 0 ${column.width}px` : 1,
              borderRight: 1,
              borderColor: 'divider',
              fontWeight: 'bold',
              cursor: column.sortable ? 'pointer' : 'default',
            }}
            onClick={column.sortable ? () => handleSort(column.key) : undefined}
          >
            {column.header}
            {column.sortable && sortColumn === column.key && (
              sortDirection === 'asc' ? <KeyboardArrowUp /> : <KeyboardArrowDown />
            )}
          </Box>
        ))}
      </Box>

      {/* Data rows */}
      {data.map((row, rowIndex) => (
        <Box
          key={rowIndex}
          role="row"
          aria-rowindex={rowIndex + 2}
          aria-selected={selectedRows.has(rowIndex)}
          sx={{
            display: 'flex',
            bgcolor: selectedRows.has(rowIndex) ? 'action.selected' : 'transparent',
            '&:hover': { bgcolor: 'action.hover' },
          }}
        >
          {columns.map((column, colIndex) => (
            <Box
              key={`${rowIndex}-${colIndex}`}
              role="gridcell"
              aria-colindex={colIndex + 1}
              tabIndex={
                focusedCell.row === rowIndex && focusedCell.col === colIndex ? 0 : -1
              }
              sx={{
                p: 1,
                flex: column.width ? `0 0 ${column.width}px` : 1,
                borderRight: 1,
                borderBottom: 1,
                borderColor: 'divider',
                bgcolor: 
                  focusedCell.row === rowIndex && focusedCell.col === colIndex
                    ? 'action.focus'
                    : 'transparent',
                '&:focus': {
                  outline: '2px solid',
                  outlineColor: 'primary.main',
                  outlineOffset: '-2px',
                },
              }}
              onFocus={() => setFocusedCell({ row: rowIndex, col: colIndex })}
            >
              {row[column.key]}
            </Box>
          ))}
        </Box>
      ))}
    </FocusableContainer>
  );
};

/**
 * Accessible Tree View Pattern
 */
interface TreeNode {
  id: string;
  label: string;
  icon?: 'folder' | 'file';
  children?: TreeNode[];
  expanded?: boolean;
  selected?: boolean;
}

export interface AccessibleTreeViewProps {
  /**
   * Tree data
   */
  data: TreeNode[];
  
  /**
   * Tree label
   */
  label?: string;
  
  /**
   * Selection mode
   */
  selectionMode?: 'none' | 'single' | 'multiple';
  
  /**
   * Node selection handler
   */
  onNodeSelect?: (nodeId: string) => void;
  
  /**
   * Node expansion handler
   */
  onNodeToggle?: (nodeId: string) => void;
}

export const AccessibleTreeView: React.FC<AccessibleTreeViewProps> = ({
  data,
  label = 'Tree view',
  selectionMode = 'single',
  onNodeSelect,
  onNodeToggle,
}) => {
  const [focusedNodeId, setFocusedNodeId] = React.useState<string | null>(null);
  const [expandedNodes, setExpandedNodes] = React.useState<Set<string>>(new Set());
  const { announcePolite } = useAnnouncement();

  const flattenTree = (nodes: TreeNode[], level = 0): Array<TreeNode & { level: number }> => {
    const result: Array<TreeNode & { level: number }> = [];
    
    for (const node of nodes) {
      result.push({ ...node, level });
      
      if (node.children && expandedNodes.has(node.id)) {
        result.push(...flattenTree(node.children, level + 1));
      }
    }
    
    return result;
  };

  const flatNodes = flattenTree(data);
  const focusedIndex = flatNodes.findIndex(node => node.id === focusedNodeId);

  const handleKeyDown = (event: React.KeyboardEvent) => {
    if (focusedIndex === -1) return;

    const currentNode = flatNodes[focusedIndex];
    let newIndex = focusedIndex;

    switch (event.key) {
      case 'ArrowDown':
        newIndex = Math.min(focusedIndex + 1, flatNodes.length - 1);
        event.preventDefault();
        break;
      case 'ArrowUp':
        newIndex = Math.max(focusedIndex - 1, 0);
        event.preventDefault();
        break;
      case 'ArrowRight':
        if (currentNode.children) {
          if (expandedNodes.has(currentNode.id)) {
            // Move to first child
            newIndex = focusedIndex + 1;
          } else {
            // Expand node
            toggleNode(currentNode.id);
          }
        }
        event.preventDefault();
        break;
      case 'ArrowLeft':
        if (currentNode.children && expandedNodes.has(currentNode.id)) {
          // Collapse node
          toggleNode(currentNode.id);
        } else if (currentNode.level > 0) {
          // Move to parent
          for (let i = focusedIndex - 1; i >= 0; i--) {
            if (flatNodes[i].level < currentNode.level) {
              newIndex = i;
              break;
            }
          }
        }
        event.preventDefault();
        break;
      case 'Home':
        newIndex = 0;
        event.preventDefault();
        break;
      case 'End':
        newIndex = flatNodes.length - 1;
        event.preventDefault();
        break;
      case 'Enter':
      case ' ':
        if (currentNode.children) {
          toggleNode(currentNode.id);
        } else {
          selectNode(currentNode.id);
        }
        event.preventDefault();
        break;
    }

    if (newIndex !== focusedIndex && flatNodes[newIndex]) {
      setFocusedNodeId(flatNodes[newIndex].id);
      announcePolite(`${flatNodes[newIndex].label}, level ${flatNodes[newIndex].level + 1}`);
    }
  };

  const toggleNode = (nodeId: string) => {
    const newExpanded = new Set(expandedNodes);
    if (newExpanded.has(nodeId)) {
      newExpanded.delete(nodeId);
      announcePolite('Collapsed');
    } else {
      newExpanded.add(nodeId);
      announcePolite('Expanded');
    }
    setExpandedNodes(newExpanded);
    onNodeToggle?.(nodeId);
  };

  const selectNode = (nodeId: string) => {
    announcePolite('Selected');
    onNodeSelect?.(nodeId);
  };

  const renderTreeNode = (node: TreeNode & { level: number }) => {
    const hasChildren = node.children && node.children.length > 0;
    const isExpanded = expandedNodes.has(node.id);
    const isFocused = focusedNodeId === node.id;

    return (
      <Box
        key={node.id}
        role="treeitem"
        aria-level={node.level + 1}
        aria-expanded={hasChildren ? isExpanded : undefined}
        aria-selected={node.selected}
        tabIndex={isFocused ? 0 : -1}
        onFocus={() => setFocusedNodeId(node.id)}
        sx={{
          display: 'flex',
          alignItems: 'center',
          py: 0.5,
          px: 1,
          pl: 1 + node.level * 2,
          cursor: 'pointer',
          bgcolor: isFocused ? 'action.focus' : 'transparent',
          '&:hover': { bgcolor: 'action.hover' },
          '&:focus': {
            outline: '2px solid',
            outlineColor: 'primary.main',
            outlineOffset: '-2px',
          },
        }}
        onClick={() => {
          setFocusedNodeId(node.id);
          if (hasChildren) {
            toggleNode(node.id);
          } else {
            selectNode(node.id);
          }
        }}
      >
        {hasChildren ? (
          isExpanded ? <ExpandLess /> : <ExpandMore />
        ) : (
          <Box sx={{ width: 24 }} />
        )}
        
        {node.icon === 'folder' ? (
          isExpanded ? <FolderOpen sx={{ mr: 1 }} /> : <Folder sx={{ mr: 1 }} />
        ) : (
          <Description sx={{ mr: 1 }} />
        )}
        
        <Typography variant="body2">
          {node.label}
        </Typography>
        
        {hasChildren && (
          <ScreenReaderOnly>
            {isExpanded ? 'expanded' : 'collapsed'}, {node.children?.length} items
          </ScreenReaderOnly>
        )}
      </Box>
    );
  };

  React.useEffect(() => {
    if (!focusedNodeId && flatNodes.length > 0) {
      setFocusedNodeId(flatNodes[0].id);
    }
  }, [flatNodes, focusedNodeId]);

  return (
    <FocusableContainer
      role="tree"
      aria-label={label}
      tabIndex={0}
      onKeyDown={handleKeyDown}
      sx={{
        border: 1,
        borderColor: 'divider',
        maxHeight: 400,
        overflow: 'auto',
      }}
    >
      <ScreenReaderOnly>
        Use arrow keys to navigate. Press Enter or Space to select or expand items.
      </ScreenReaderOnly>
      
      {flatNodes.map(renderTreeNode)}
    </FocusableContainer>
  );
};

/**
 * Accessible Carousel Pattern
 */
export interface AccessibleCarouselProps {
  /**
   * Carousel items
   */
  items: Array<{
    id: string;
    content: React.ReactNode;
    alt?: string;
    caption?: string;
  }>;
  
  /**
   * Carousel label
   */
  label?: string;
  
  /**
   * Auto-play interval (0 to disable)
   */
  autoPlayInterval?: number;
  
  /**
   * Show navigation controls
   */
  showControls?: boolean;
  
  /**
   * Show pagination dots
   */
  showPagination?: boolean;
}

export const AccessibleCarousel: React.FC<AccessibleCarouselProps> = ({
  items,
  label = 'Carousel',
  autoPlayInterval = 0,
  showControls = true,
  showPagination = true,
}) => {
  const [currentIndex, setCurrentIndex] = React.useState(0);
  const [isPlaying, setIsPlaying] = React.useState(autoPlayInterval > 0);
  const { announcePolite } = useAnnouncement();
  
  const intervalRef = React.useRef<NodeJS.Timeout>();

  // Auto-play functionality
  React.useEffect(() => {
    if (isPlaying && autoPlayInterval > 0) {
      intervalRef.current = setInterval(() => {
        setCurrentIndex((prev) => (prev + 1) % items.length);
      }, autoPlayInterval);
    } else {
      clearInterval(intervalRef.current);
    }

    return () => clearInterval(intervalRef.current);
  }, [isPlaying, autoPlayInterval, items.length]);

  const goToSlide = (index: number) => {
    setCurrentIndex(index);
    announcePolite(`Slide ${index + 1} of ${items.length}: ${items[index].caption || items[index].alt || ''}`);
  };

  const goToPrevious = () => {
    const newIndex = currentIndex === 0 ? items.length - 1 : currentIndex - 1;
    goToSlide(newIndex);
  };

  const goToNext = () => {
    const newIndex = (currentIndex + 1) % items.length;
    goToSlide(newIndex);
  };

  const togglePlayPause = () => {
    setIsPlaying(!isPlaying);
    announcePolite(isPlaying ? 'Carousel paused' : 'Carousel playing');
  };

  const handleKeyDown = (event: React.KeyboardEvent) => {
    switch (event.key) {
      case 'ArrowLeft':
        goToPrevious();
        event.preventDefault();
        break;
      case 'ArrowRight':
        goToNext();
        event.preventDefault();
        break;
      case 'Home':
        goToSlide(0);
        event.preventDefault();
        break;
      case 'End':
        goToSlide(items.length - 1);
        event.preventDefault();
        break;
      case ' ':
        if (autoPlayInterval > 0) {
          togglePlayPause();
          event.preventDefault();
        }
        break;
    }
  };

  return (
    <Box
      role="region"
      aria-label={label}
      aria-live={isPlaying ? 'polite' : 'off'}
      onKeyDown={handleKeyDown}
      sx={{ position: 'relative' }}
    >
      <ScreenReaderOnly>
        Carousel with {items.length} slides. Use left and right arrow keys to navigate. 
        {autoPlayInterval > 0 && 'Press Space to pause or resume auto-play.'}
      </ScreenReaderOnly>

      {/* Main carousel container */}
      <Box
        sx={{
          position: 'relative',
          overflow: 'hidden',
          '&:focus': {
            outline: '2px solid',
            outlineColor: 'primary.main',
            outlineOffset: '2px',
          },
        }}
        tabIndex={0}
      >
        <Box
          sx={{
            display: 'flex',
            transform: `translateX(-${currentIndex * 100}%)`,
            transition: 'transform 0.3s ease-in-out',
          }}
        >
          {items.map((item, index) => (
            <Box
              key={item.id}
              role="tabpanel"
              aria-label={`Slide ${index + 1} of ${items.length}`}
              aria-hidden={index !== currentIndex}
              sx={{
                flex: '0 0 100%',
                minHeight: 200,
                display: 'flex',
                alignItems: 'center',
                justifyContent: 'center',
              }}
            >
              {item.content}
            </Box>
          ))}
        </Box>
      </Box>

      {/* Navigation controls */}
      {showControls && (
        <>
          <IconButton
            onClick={goToPrevious}
            aria-label="Previous slide"
            sx={{
              position: 'absolute',
              left: 8,
              top: '50%',
              transform: 'translateY(-50%)',
              bgcolor: 'rgba(0, 0, 0, 0.5)',
              color: 'white',
              '&:hover': { bgcolor: 'rgba(0, 0, 0, 0.7)' },
            }}
          >
            <KeyboardArrowLeft />
          </IconButton>

          <IconButton
            onClick={goToNext}
            aria-label="Next slide"
            sx={{
              position: 'absolute',
              right: 8,
              top: '50%',
              transform: 'translateY(-50%)',
              bgcolor: 'rgba(0, 0, 0, 0.5)',
              color: 'white',
              '&:hover': { bgcolor: 'rgba(0, 0, 0, 0.7)' },
            }}
          >
            <KeyboardArrowRight />
          </IconButton>

          {autoPlayInterval > 0 && (
            <Button
              onClick={togglePlayPause}
              aria-label={isPlaying ? 'Pause carousel' : 'Play carousel'}
              sx={{
                position: 'absolute',
                bottom: 8,
                left: 8,
                bgcolor: 'rgba(0, 0, 0, 0.5)',
                color: 'white',
                '&:hover': { bgcolor: 'rgba(0, 0, 0, 0.7)' },
              }}
            >
              {isPlaying ? 'Pause' : 'Play'}
            </Button>
          )}
        </>
      )}

      {/* Pagination dots */}
      {showPagination && (
        <Box
          role="tablist"
          aria-label="Carousel slides"
          sx={{
            display: 'flex',
            justifyContent: 'center',
            gap: 1,
            mt: 2,
          }}
        >
          {items.map((_, index) => (
            <Button
              key={index}
              role="tab"
              aria-selected={index === currentIndex}
              aria-controls={`slide-${index}`}
              onClick={() => goToSlide(index)}
              sx={{
                minWidth: 12,
                width: 12,
                height: 12,
                borderRadius: '50%',
                p: 0,
                bgcolor: index === currentIndex ? 'primary.main' : 'grey.300',
                '&:focus': {
                  outline: '2px solid',
                  outlineColor: 'primary.main',
                  outlineOffset: '2px',
                },
              }}
              aria-label={`Go to slide ${index + 1}`}
            />
          ))}
        </Box>
      )}
    </Box>
  );
};

export default {
  AccessibleDataGrid,
  AccessibleTreeView,
  AccessibleCarousel,
};