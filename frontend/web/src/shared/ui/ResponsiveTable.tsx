/**
 * Responsive Table Component
 * 
 * Intelligent table that transforms from traditional table layout on desktop
 * to card-based layout on mobile, with smart column management and sorting
 */

import React, { useState, useMemo } from 'react'
import {
  Table,
  TableBody,
  TableCell,
  TableContainer,
  TableHead,
  TableRow,
  TableSortLabel,
  Paper,
  Box,
  Typography,
  Chip,
  Avatar,
  Card,
  CardContent,
  Skeleton,
  useTheme,
  alpha,
  Collapse,
  Button,
} from '@mui/material'
import {
  ExpandMore as ExpandMoreIcon,
  ExpandLess as ExpandLessIcon,
} from '@mui/icons-material'
import { styled } from '@mui/material/styles'
import { useResponsive } from '../hooks'

// Types
interface TableColumn<T = Record<string, unknown>> {
  id: string
  label: string
  accessor: keyof T | ((row: T) => unknown)
  width?: number | string
  minWidth?: number
  align?: 'left' | 'center' | 'right'
  sortable?: boolean
  filterable?: boolean
  hiddenOnMobile?: boolean
  renderCell?: (value: unknown, row: T, column: TableColumn<T>) => React.ReactNode
  renderMobileCard?: (row: T) => React.ReactNode
}

interface ResponsiveTableProps<T = Record<string, unknown>> {
  data: T[]
  columns: TableColumn<T>[]
  loading?: boolean
  sortable?: boolean
  filterable?: boolean
  searchable?: boolean
  pagination?: boolean
  selectedRows?: Set<string>
  onRowSelect?: (rowId: string, selected: boolean) => void
  onRowClick?: (row: T, index: number) => void
  onSort?: (column: string, direction: 'asc' | 'desc') => void
  getRowId?: (row: T) => string
  emptyMessage?: string
  stickyHeader?: boolean
  maxHeight?: number | string
  density?: 'compact' | 'normal' | 'spacious'
}

// Styled components
const StyledTableContainer = styled(TableContainer)(({ theme }) => ({
  '& .MuiTable-root': {
    minWidth: 650,
  },
  
  // Mobile responsiveness
  [theme.breakpoints.down('tablet')]: {
    '& .MuiTable-root, & .MuiTableHead-root, & .MuiTableBody-root, & .MuiTableRow-root, & .MuiTableCell-root': {
      display: 'block',
    },
    
    '& .MuiTableHead-root': {
      display: 'none', // Hide table header on mobile
    },
    
    '& .MuiTableRow-root': {
      marginBottom: theme.spacing(2),
      backgroundColor: theme.palette.background.paper,
      borderRadius: theme.shape.borderRadius,
      boxShadow: theme.shadows[1],
      border: `1px solid ${theme.palette.divider}`,
      padding: theme.spacing(1),
    },
    
    '& .MuiTableCell-root': {
      border: 'none',
      paddingLeft: theme.spacing(2),
      paddingRight: theme.spacing(2),
      paddingTop: theme.spacing(1),
      paddingBottom: theme.spacing(1),
      position: 'relative',
      
      '&:before': {
        content: 'attr(data-label)',
        position: 'absolute',
        left: theme.spacing(2),
        top: theme.spacing(1),
        fontWeight: 600,
        fontSize: '0.875rem',
        color: theme.palette.text.secondary,
        minWidth: '100px',
      },
      
      '& .cell-content': {
        marginLeft: '120px',
        display: 'block',
      },
    },
  },
}))

const MobileCard = styled(Card)(({ theme }) => ({
  marginBottom: theme.spacing(2),
  transition: 'all 0.2s ease-in-out',
  '&:hover': {
    boxShadow: theme.shadows[4],
    transform: 'translateY(-1px)',
  },
}))

const LoadingSkeleton: React.FC<{ columns: number }> = ({ columns }) => (
  <>
    {Array.from({ length: 5 }).map((_, rowIndex) => (
      <TableRow key={rowIndex}>
        {Array.from({ length: columns }).map((_, colIndex) => (
          <TableCell key={colIndex}>
            <Skeleton variant="text" width="80%" />
          </TableCell>
        ))}
      </TableRow>
    ))}
  </>
)

// Mobile card renderer
const MobileTableCard = <T extends Record<string, unknown>>({ row, columns, onRowClick }: {
  row: T
  columns: TableColumn<T>[]
  onRowClick?: (row: T) => void
  getRowId?: (row: T) => string
}) => {
  const [expanded, setExpanded] = useState(false)
  
  // Primary columns (always shown)
  const primaryColumns = columns.filter(col => !col.hiddenOnMobile).slice(0, 3)
  
  // Secondary columns (shown when expanded)
  const secondaryColumns = columns.filter(col => !col.hiddenOnMobile).slice(3)
  
  const renderCellValue = (column: TableColumn<T>, value: unknown) => {
    if (column.renderCell) {
      return column.renderCell(value, row, column)
    }
    
    // Default rendering based on value type
    if (typeof value === 'boolean') {
      return <Chip label={value ? 'Yes' : 'No'} size="small" color={value ? 'success' : 'default'} />
    }
    
    if (typeof value === 'number' && column.id.toLowerCase().includes('price')) {
      return `$${value.toFixed(2)}`
    }
    
    if (value instanceof Date) {
      return value.toLocaleDateString()
    }
    
    return String(value)
  }
  
  const getValue = (column: TableColumn) => {
    return typeof column.accessor === 'function' 
      ? column.accessor(row)
      : row[column.accessor]
  }
  
  return (
    <MobileCard onClick={() => onRowClick?.(row)}>
      <CardContent sx={{ pb: 1 }}>
        {/* Primary information */}
        {primaryColumns.map((column, index) => {
          const value = getValue(column)
          return (
            <Box key={column.id} sx={{ mb: index < primaryColumns.length - 1 ? 1 : 0 }}>
              <Typography variant="caption" color="text.secondary" display="block">
                {column.label}
              </Typography>
              <Typography variant="body2" fontWeight={index === 0 ? 600 : 400}>
                {renderCellValue(column, value)}
              </Typography>
            </Box>
          )
        })}
        
        {/* Expand/collapse for additional info */}
        {secondaryColumns.length > 0 && (
          <>
            <Collapse in={expanded} timeout="auto" unmountOnExit>
              <Box sx={{ mt: 2, pt: 2, borderTop: 1, borderColor: 'divider' }}>
                {secondaryColumns.map((column, index) => {
                  const value = getValue(column)
                  return (
                    <Box key={column.id} sx={{ mb: index < secondaryColumns.length - 1 ? 1 : 0 }}>
                      <Typography variant="caption" color="text.secondary" display="block">
                        {column.label}
                      </Typography>
                      <Typography variant="body2">
                        {renderCellValue(column, value)}
                      </Typography>
                    </Box>
                  )
                })}
              </Box>
            </Collapse>
            
            <Box sx={{ display: 'flex', justifyContent: 'center', mt: 1 }}>
              <Button
                size="small"
                onClick={(e) => {
                  e.stopPropagation()
                  setExpanded(!expanded)
                }}
                endIcon={expanded ? <ExpandLessIcon /> : <ExpandMoreIcon />}
              >
                {expanded ? 'Less' : 'More'}
              </Button>
            </Box>
          </>
        )}
      </CardContent>
    </MobileCard>
  )
}

// Main ResponsiveTable component
export const ResponsiveTable = <T extends Record<string, unknown>>({
  data,
  columns,
  loading = false,
  sortable = true,
  onRowClick,
  onSort,
  getRowId = (row) => (row as Record<string, unknown> & { id?: string }).id || JSON.stringify(row),
  emptyMessage = 'No data available',
  stickyHeader = false,
  maxHeight,
  density = 'normal',
}: ResponsiveTableProps<T>) => {
  const theme = useTheme()
  const { isMobile, isTablet } = useResponsive()
  const [sortColumn, setSortColumn] = useState<string>('')
  const [sortDirection, setSortDirection] = useState<'asc' | 'desc'>('asc')
  
  // Use mobile layout for small screens
  const useMobileLayout = isMobile || isTablet
  
  // Density configuration
  const densityConfig = {
    compact: { padding: '6px 8px', fontSize: '0.875rem' },
    normal: { padding: '12px 16px', fontSize: '1rem' },
    spacious: { padding: '16px 24px', fontSize: '1rem' },
  }
  
  // Handle sorting
  const handleSort = (columnId: string) => {
    if (!sortable) return
    
    const newDirection = sortColumn === columnId && sortDirection === 'asc' ? 'desc' : 'asc'
    setSortColumn(columnId)
    setSortDirection(newDirection)
    onSort?.(columnId, newDirection)
  }
  
  // Filter visible columns for mobile
  const visibleColumns = useMemo(() => {
    if (useMobileLayout) {
      return columns.filter(col => !col.hiddenOnMobile)
    }
    return columns
  }, [columns, useMobileLayout])
  
  // Render loading state
  if (loading) {
    if (useMobileLayout) {
      return (
        <Box>
          {Array.from({ length: 5 }).map((_, index) => (
            <Card key={index} sx={{ mb: 2 }}>
              <CardContent>
                <Skeleton variant="text" width="60%" height={20} />
                <Skeleton variant="text" width="80%" height={16} sx={{ mt: 1 }} />
                <Skeleton variant="text" width="40%" height={16} sx={{ mt: 1 }} />
              </CardContent>
            </Card>
          ))}
        </Box>
      )
    }
    
    return (
      <Paper>
        <StyledTableContainer>
          <Table stickyHeader={stickyHeader}>
            <TableHead>
              <TableRow>
                {columns.map((column) => (
                  <TableCell key={column.id}>
                    <Skeleton variant="text" width="80%" />
                  </TableCell>
                ))}
              </TableRow>
            </TableHead>
            <TableBody>
              <LoadingSkeleton columns={columns.length} />
            </TableBody>
          </Table>
        </StyledTableContainer>
      </Paper>
    )
  }
  
  // Render empty state
  if (data.length === 0) {
    return (
      <Box
        sx={{
          display: 'flex',
          flexDirection: 'column',
          alignItems: 'center',
          justifyContent: 'center',
          py: 8,
          textAlign: 'center',
        }}
      >
        <Typography variant="h6" color="text.secondary" gutterBottom>
          {emptyMessage}
        </Typography>
      </Box>
    )
  }
  
  // Mobile card layout
  if (useMobileLayout) {
    return (
      <Box>
        {data.map((row) => (
          <MobileTableCard
            key={getRowId(row)}
            row={row}
            columns={visibleColumns}
            onRowClick={onRowClick ? (clickedRow) => onRowClick(clickedRow, 0) : undefined}
            getRowId={getRowId}
          />
        ))}
      </Box>
    )
  }
  
  // Desktop table layout
  return (
    <Paper>
      <StyledTableContainer 
        sx={{ 
          maxHeight,
          '& .MuiTableCell-root': {
            ...densityConfig[density],
          },
        }}
      >
      <Table stickyHeader={stickyHeader}>
        <TableHead>
          <TableRow>
            {visibleColumns.map((column) => (
              <TableCell
                key={column.id}
                align={column.align}
                style={{ 
                  minWidth: column.minWidth,
                  width: column.width,
                }}
                sortDirection={sortColumn === column.id ? sortDirection : false}
              >
                {sortable && column.sortable !== false ? (
                  <TableSortLabel
                    active={sortColumn === column.id}
                    direction={sortColumn === column.id ? sortDirection : 'asc'}
                    onClick={() => handleSort(column.id)}
                  >
                    {column.label}
                  </TableSortLabel>
                ) : (
                  column.label
                )}
              </TableCell>
            ))}
          </TableRow>
        </TableHead>
        <TableBody>
          {data.map((row, index) => (
            <TableRow
              key={getRowId(row)}
              hover={!!onRowClick}
              onClick={() => onRowClick?.(row, index)}
              sx={{
                cursor: onRowClick ? 'pointer' : 'default',
                '&:hover': onRowClick ? {
                  backgroundColor: alpha(theme.palette.primary.main, 0.04),
                } : {},
              }}
            >
              {visibleColumns.map((column) => {
                const value = typeof column.accessor === 'function' 
                  ? column.accessor(row)
                  : row[column.accessor]
                
                return (
                  <TableCell
                    key={column.id}
                    align={column.align}
                    data-label={column.label}
                  >
                    <div className="cell-content">
                      {column.renderCell ? column.renderCell(value, row, column) : (value as React.ReactNode)}
                    </div>
                  </TableCell>
                )
              })}
            </TableRow>
          ))}
        </TableBody>
      </Table>
      </StyledTableContainer>
    </Paper>
  )
}

// Pre-configured table components
export const UserTable: React.FC<{
  users: Array<{
    id: string
    name: string
    email: string
    avatar?: string
    role: string
    status: 'active' | 'inactive'
    lastSeen: Date
  }>
  onUserClick?: (user: unknown) => void
  loading?: boolean
}> = ({ users, onUserClick, loading }) => {
  const columns: TableColumn<{
    id: string;
    name: string;
    email: string;
    avatar?: string;
    role: string;
    status: 'active' | 'inactive';
    lastSeen: Date;
  }>[] = [
    {
      id: 'user',
      label: 'User',
      accessor: 'name',
      renderCell: (value, row) => (
        <Box sx={{ display: 'flex', alignItems: 'center', gap: 2 }}>
          <Avatar src={row.avatar as string} alt={row.name as string} sx={{ width: 32, height: 32 }}>
            {(row.name as string)?.[0] || ''}
          </Avatar>
          <Box>
            <Typography variant="body2" fontWeight={600}>
              {value as React.ReactNode}
            </Typography>
            <Typography variant="caption" color="text.secondary">
              {row.email as string}
            </Typography>
          </Box>
        </Box>
      ),
    },
    {
      id: 'role',
      label: 'Role',
      accessor: 'role',
      renderCell: (value) => (
        <Chip label={value as React.ReactNode} size="small" variant="outlined" />
      ),
    },
    {
      id: 'status',
      label: 'Status',
      accessor: 'status',
      renderCell: (value) => (
        <Chip
          label={value as React.ReactNode}
          size="small"
          color={(value as string) === 'active' ? 'success' : 'default'}
        />
      ),
    },
    {
      id: 'lastSeen',
      label: 'Last Seen',
      accessor: 'lastSeen',
      hiddenOnMobile: true,
      renderCell: (value) => (value as Date).toLocaleDateString(),
    },
  ]
  
  return (
    <ResponsiveTable
      data={users}
      columns={columns}
      onRowClick={onUserClick}
      loading={loading}
    />
  )
}

export default ResponsiveTable