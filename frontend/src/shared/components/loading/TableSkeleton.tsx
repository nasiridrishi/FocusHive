import React from 'react'
import {
  Table,
  TableBody,
  TableCell,
  TableContainer,
  TableHead,
  TableRow,
  Skeleton,
  Checkbox,
  IconButton
} from '@mui/material'
import type { TableSkeletonProps } from './types'

/**
 * Table-specific skeleton loading component
 * 
 * Features:
 * - Configurable rows and columns
 * - Header skeleton support
 * - Actions column skeleton
 * - Responsive design
 * - Material UI table structure
 */
const TableSkeleton: React.FC<TableSkeletonProps> = ({
  rows = 5,
  columns = 4,
  showHeader = true,
  showActions = true,
  animation = 'wave'
}) => {
  const totalColumns = columns + (showActions ? 1 : 0)

  const renderHeaderCell = (index: number) => {
    if (index === 0 && showActions) {
      return (
        <TableCell padding="checkbox">
          <Checkbox disabled />
        </TableCell>
      )
    }
    
    if (index === totalColumns - 1 && showActions) {
      return (
        <TableCell align="right">
          <Skeleton variant="text" width="60%" height={24} animation={animation} />
        </TableCell>
      )
    }
    
    return (
      <TableCell>
        <Skeleton variant="text" width="80%" height={24} animation={animation} />
      </TableCell>
    )
  }

  const renderBodyCell = (_rowIndex: number, colIndex: number) => {
    if (colIndex === 0 && showActions) {
      return (
        <TableCell padding="checkbox">
          <Checkbox disabled />
        </TableCell>
      )
    }
    
    if (colIndex === totalColumns - 1 && showActions) {
      return (
        <TableCell align="right">
          <IconButton disabled size="small">
            <Skeleton variant="circular" width={20} height={20} animation={animation} />
          </IconButton>
          <IconButton disabled size="small">
            <Skeleton variant="circular" width={20} height={20} animation={animation} />
          </IconButton>
        </TableCell>
      )
    }
    
    // Vary the width for more realistic appearance
    const widths = ['100%', '80%', '60%', '90%', '70%']
    const width = widths[colIndex % widths.length]
    
    return (
      <TableCell>
        <Skeleton variant="text" width={width} height={20} animation={animation} />
      </TableCell>
    )
  }

  return (
    <TableContainer>
      <Table>
        {showHeader && (
          <TableHead>
            <TableRow>
              {Array.from({ length: totalColumns }).map((_, index) => (
                <React.Fragment key={`header-${index}`}>
                  {renderHeaderCell(index)}
                </React.Fragment>
              ))}
            </TableRow>
          </TableHead>
        )}
        
        <TableBody>
          {Array.from({ length: rows }).map((_, rowIndex) => (
            <TableRow key={`row-${rowIndex}`}>
              {Array.from({ length: totalColumns }).map((_, colIndex) => (
                <React.Fragment key={`cell-${rowIndex}-${colIndex}`}>
                  {renderBodyCell(rowIndex, colIndex)}
                </React.Fragment>
              ))}
            </TableRow>
          ))}
        </TableBody>
      </Table>
    </TableContainer>
  )
}

export default TableSkeleton