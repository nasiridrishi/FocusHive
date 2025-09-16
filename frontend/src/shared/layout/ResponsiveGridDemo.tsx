/**
 * Demo component to verify ResponsiveGrid fix works in practice
 */

import React from 'react';
import {GridItem, ResponsiveGrid} from './ResponsiveGrid';
import {Box, Paper, Typography} from '@mui/material';

export const ResponsiveGridDemo: React.FC = () => {
  return (
      <Box sx={{p: 3}}>
        <Typography variant="h4" gutterBottom>
          ResponsiveGrid Fix Demo
        </Typography>

        <Typography variant="body1" sx={{mb: 3}}>
          This demo verifies that the infinite recursion bug in GridItem has been fixed.
        </Typography>

        {/* Basic ResponsiveGrid */}
        <Typography variant="h6" gutterBottom>
          Basic Grid Layout
        </Typography>
        <ResponsiveGrid
            columns={{mobile: 1, tablet: 2, desktop: 3}}
            gap={{mobile: 1, tablet: 2, desktop: 3}}
            sx={{mb: 4}}
        >
          <GridItem>
            <Paper sx={{
              p: 2,
              height: 100,
              display: 'flex',
              alignItems: 'center',
              justifyContent: 'center'
            }}>
              <Typography>Item 1</Typography>
            </Paper>
          </GridItem>
          <GridItem>
            <Paper sx={{
              p: 2,
              height: 100,
              display: 'flex',
              alignItems: 'center',
              justifyContent: 'center'
            }}>
              <Typography>Item 2</Typography>
            </Paper>
          </GridItem>
          <GridItem>
            <Paper sx={{
              p: 2,
              height: 100,
              display: 'flex',
              alignItems: 'center',
              justifyContent: 'center'
            }}>
              <Typography>Item 3</Typography>
            </Paper>
          </GridItem>
        </ResponsiveGrid>

        {/* GridItem with span properties */}
        <Typography variant="h6" gutterBottom>
          Grid with Span Properties
        </Typography>
        <ResponsiveGrid columns={4} gap={2} sx={{mb: 4}}>
          <GridItem span={2}>
            <Paper sx={{
              p: 2,
              height: 80,
              display: 'flex',
              alignItems: 'center',
              justifyContent: 'center'
            }}>
              <Typography>Span 2</Typography>
            </Paper>
          </GridItem>
          <GridItem span={1}>
            <Paper sx={{
              p: 2,
              height: 80,
              display: 'flex',
              alignItems: 'center',
              justifyContent: 'center'
            }}>
              <Typography>Span 1</Typography>
            </Paper>
          </GridItem>
          <GridItem span={1}>
            <Paper sx={{
              p: 2,
              height: 80,
              display: 'flex',
              alignItems: 'center',
              justifyContent: 'center'
            }}>
              <Typography>Span 1</Typography>
            </Paper>
          </GridItem>
        </ResponsiveGrid>

        {/* Responsive GridItem */}
        <Typography variant="h6" gutterBottom>
          Responsive GridItem Spans
        </Typography>
        <ResponsiveGrid columns={{mobile: 2, tablet: 4, desktop: 6}} gap={2}>
          <GridItem span={{mobile: 2, tablet: 2, desktop: 3}}>
            <Paper sx={{
              p: 2,
              height: 80,
              display: 'flex',
              alignItems: 'center',
              justifyContent: 'center'
            }}>
              <Typography>Responsive Item</Typography>
            </Paper>
          </GridItem>
          <GridItem span={{mobile: 1, tablet: 2, desktop: 2}}>
            <Paper sx={{
              p: 2,
              height: 80,
              display: 'flex',
              alignItems: 'center',
              justifyContent: 'center'
            }}>
              <Typography>Item 2</Typography>
            </Paper>
          </GridItem>
          <GridItem span={{mobile: 1, tablet: 2, desktop: 1}}>
            <Paper sx={{
              p: 2,
              height: 80,
              display: 'flex',
              alignItems: 'center',
              justifyContent: 'center'
            }}>
              <Typography>Item 3</Typography>
            </Paper>
          </GridItem>
        </ResponsiveGrid>

        <Typography variant="body2" sx={{mt: 3, color: 'success.main'}}>
          ✅ If you can see this demo without browser crashes, the infinite recursion bug has been
          successfully fixed!
        </Typography>
      </Box>
  );
};