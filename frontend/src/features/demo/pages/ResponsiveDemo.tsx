/**
 * Responsive Design Demo Page
 * 
 * Showcases all the advanced responsive components and features
 * Demonstrates the comprehensive responsive design system in action
 */

import React, { useState } from 'react'
import {
  Typography,
  Button,
  Switch,
  FormControlLabel,
  Box,
  Chip,
  Avatar,
} from '@mui/material'
// Icons will be added as needed

// Import our responsive components
import {
  ResponsiveGrid,
  Section,
  PageLayout,
  FlexContainer,
} from '@shared/layout'
import {
  AdaptiveCard,
  ProductCard,
  ArticleCard,
  ProfileCard,
  ResponsiveTable,
  SmartModal,
  ScrollToTopFAB,
  ContextualFAB,
} from '@shared/ui'
import { useResponsive } from '@shared/hooks'

// Mock data
const mockUsers = [
  {
    id: '1',
    name: 'Alice Johnson',
    email: 'alice@example.com',
    avatar: '',
    role: 'Team Lead',
    status: 'active' as const,
    lastSeen: new Date('2024-01-20'),
  },
  {
    id: '2',
    name: 'Bob Smith',
    email: 'bob@example.com',
    avatar: '',
    role: 'Developer',
    status: 'active' as const,
    lastSeen: new Date('2024-01-19'),
  },
  {
    id: '3',
    name: 'Carol Williams',
    email: 'carol@example.com',
    avatar: '',
    role: 'Designer',
    status: 'inactive' as const,
    lastSeen: new Date('2024-01-18'),
  },
]

const mockProducts = [
  {
    id: 1,
    title: 'Focus Timer Pro',
    description: 'Advanced Pomodoro timer with analytics and team collaboration features.',
    image: 'https://via.placeholder.com/300x200',
    price: '$29.99',
    originalPrice: '$39.99',
    discount: '25% OFF',
  },
  {
    id: 2,
    title: 'Study Planner',
    description: 'Comprehensive study planning tool with calendar integration and progress tracking.',
    image: 'https://via.placeholder.com/300x200',
    price: '$19.99',
  },
  {
    id: 3,
    title: 'Productivity Bundle',
    description: 'Complete productivity suite with focus tools, note-taking, and project management.',
    image: 'https://via.placeholder.com/300x200',
    price: '$49.99',
    originalPrice: '$79.99',
    discount: '37% OFF',
  },
]

const mockArticles = [
  {
    id: 1,
    title: 'The Science of Focus: How Deep Work Transforms Productivity',
    description: 'Explore the neurological foundations of focused attention and learn evidence-based strategies for maintaining deep concentration in our distracted world.',
    image: 'https://via.placeholder.com/400x250',
    author: 'Dr. Sarah Chen',
    publishDate: 'Jan 15, 2024',
    readTime: '8 min',
    category: 'Science',
  },
  {
    id: 2,
    title: 'Building Effective Study Groups: A Guide to Collaborative Learning',
    description: 'Discover how to create and maintain productive study groups that enhance learning outcomes and build lasting academic relationships.',
    image: 'https://via.placeholder.com/400x250',
    author: 'Michael Rodriguez',
    publishDate: 'Jan 12, 2024',
    readTime: '6 min',
    category: 'Education',
  },
]

export const ResponsiveDemo: React.FC = () => {
  const { currentBreakpoint, isMobile, isTablet, isDesktop } = useResponsive()
  const [modalOpen, setModalOpen] = useState(false)
  const [tableLoading, setTableLoading] = useState(false)
  const [darkMode, setDarkMode] = useState(false)
  
  const handleRefreshTable = () => {
    setTableLoading(true)
    setTimeout(() => setTableLoading(false), 2000)
  }
  
  const handleFABAction = (action: string) => {
    // In a real app, you'd handle the specific action
  }
  
  return (
    <PageLayout
      title="Responsive Design System Demo"
      subtitle="Showcase of advanced responsive components and features"
      maxWidth="desktopLg"
      spacing="normal"
      actions={
        <>
          <FormControlLabel
            control={
              <Switch
                checked={darkMode}
                onChange={(e) => setDarkMode(e.target.checked)}
              />
            }
            label="Dark Mode"
          />
          <Button
            variant="outlined"
            onClick={() => setModalOpen(true)}
          >
            Open Modal
          </Button>
        </>
      }
    >
      {/* Responsive Info Section */}
      <Section background="paper" spacing="medium">
        <Typography variant="h5" gutterBottom fontWeight={600}>
          Current Responsive State
        </Typography>
        <FlexContainer direction="row" gap={2} wrap>
          <Chip
            label={`Breakpoint: ${currentBreakpoint}`}
            color="primary"
            variant="outlined"
          />
          <Chip
            label={`Mobile: ${isMobile ? 'Yes' : 'No'}`}
            color={isMobile ? 'success' : 'default'}
          />
          <Chip
            label={`Tablet: ${isTablet ? 'Yes' : 'No'}`}
            color={isTablet ? 'success' : 'default'}
          />
          <Chip
            label={`Desktop: ${isDesktop ? 'Yes' : 'No'}`}
            color={isDesktop ? 'success' : 'default'}
          />
        </FlexContainer>
      </Section>
      
      {/* Adaptive Cards Section */}
      <Section spacing="medium">
        <Typography variant="h5" gutterBottom fontWeight={600}>
          Adaptive Cards
        </Typography>
        <Typography variant="body1" color="text.secondary" paragraph>
          Cards that adapt their layout and content density based on container size.
        </Typography>
        
        <ResponsiveGrid
          columns={{ mobile: 1, tablet: 2, desktop: 3 }}
          gap={{ mobile: 2, tablet: 3, desktop: 4 }}
          useContainerQueries={true}
        >
          {mockProducts.map((product) => (
            <ProductCard
              key={product.id}
              title={product.title}
              description={product.description}
              image={product.image}
              price={product.price}
              originalPrice={product.originalPrice}
              discount={product.discount}
              interactive
              onCardClick={() => {}}
            />
          ))}
        </ResponsiveGrid>
      </Section>
      
      {/* Article Cards */}
      <Section spacing="medium">
        <Typography variant="h5" gutterBottom fontWeight={600}>
          Article Cards
        </Typography>
        
        <ResponsiveGrid columns={{ mobile: 1, tablet: 1, desktop: 2 }} gap={3}>
          {mockArticles.map((article) => (
            <ArticleCard
              key={article.id}
              title={article.title}
              description={article.description}
              image={article.image}
              author={article.author}
              publishDate={article.publishDate}
              readTime={article.readTime}
              category={article.category}
              interactive
              onCardClick={() => {}}
            />
          ))}
        </ResponsiveGrid>
      </Section>
      
      {/* Profile Cards */}
      <Section spacing="medium">
        <Typography variant="h5" gutterBottom fontWeight={600}>
          Profile Cards
        </Typography>
        
        <ResponsiveGrid columns={{ mobile: 1, tablet: 2, desktop: 3 }} gap={3}>
          {mockUsers.map((user) => (
            <ProfileCard
              key={user.id}
              title={user.name}
              subtitle={user.email}
              avatar={`https://api.dicebear.com/7.x/avataaars/svg?seed=${user.name}`}
              status={user.status === 'active' ? 'online' : 'offline'}
              role={user.role}
              stats={[
                { label: 'Focus Hours', value: '124' },
                { label: 'Sessions', value: '45' },
                { label: 'Streak', value: '12' },
              ]}
              interactive
              onCardClick={() => {}}
            />
          ))}
        </ResponsiveGrid>
      </Section>
      
      {/* Responsive Table Section */}
      <Section spacing="medium">
        <Typography variant="h5" gutterBottom fontWeight={600}>
          Responsive Table
        </Typography>
        <Typography variant="body1" color="text.secondary" paragraph>
          Table that transforms to cards on mobile while maintaining full functionality.
        </Typography>
        
        <Box sx={{ mb: 2 }}>
          <Button
            variant="outlined"
            onClick={handleRefreshTable}
            disabled={tableLoading}
          >
            {tableLoading ? 'Loading...' : 'Refresh Data'}
          </Button>
        </Box>
        
        <ResponsiveTable
          data={mockUsers}
          columns={[
            {
              id: 'user',
              label: 'User',
              accessor: 'name',
              renderCell: (value, row) => (
                <Box sx={{ display: 'flex', alignItems: 'center', gap: 2 }}>
                  <Avatar 
                    src={`https://api.dicebear.com/7.x/avataaars/svg?seed=${row.name}`}
                    sx={{ width: 32, height: 32 }}
                  >
                    {String(value)[0]}
                  </Avatar>
                  <Box>
                    <Typography variant="body2" fontWeight={600}>
                      {String(value)}
                    </Typography>
                    <Typography variant="caption" color="text.secondary">
                      {row.email}
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
                <Chip label={String(value)} size="small" variant="outlined" />
              ),
            },
            {
              id: 'status',
              label: 'Status',
              accessor: 'status',
              renderCell: (value) => (
                <Chip
                  label={String(value)}
                  size="small"
                  color={value === 'active' ? 'success' : 'default'}
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
          ]}
          onRowClick={(user) => {}}
          loading={tableLoading}
          density="normal"
        />
      </Section>
      
      {/* Grid System Demo */}
      <Section spacing="medium">
        <Typography variant="h5" gutterBottom fontWeight={600}>
          Responsive Grid System
        </Typography>
        <Typography variant="body1" color="text.secondary" paragraph>
          CSS Grid with container queries and adaptive column sizing.
        </Typography>
        
        <ResponsiveGrid
          autoFit
          minItemWidth={200}
          maxItemWidth={300}
          gap={{ mobile: 2, desktop: 3 }}
        >
          {Array.from({ length: 8 }).map((_, index) => (
            <AdaptiveCard
              key={index}
              title={`Grid Item ${index + 1}`}
              description="This item automatically sizes based on container width using CSS Grid auto-fit."
              variant="minimal"
              density="compact"
            />
          ))}
        </ResponsiveGrid>
      </Section>
      
      {/* Smart Modal Demo */}
      <SmartModal
        open={modalOpen}
        title="Smart Modal Demo"
        subtitle="This modal adapts to your device"
        onClose={() => setModalOpen(false)}
        variant="adaptive"
        size="medium"
        actions={
          <>
            <Button onClick={() => setModalOpen(false)}>
              Cancel
            </Button>
            <Button variant="contained" onClick={() => setModalOpen(false)}>
              Save
            </Button>
          </>
        }
      >
        <Typography paragraph>
          This modal automatically adapts its behavior based on your device:
        </Typography>
        <Box component="ul" sx={{ pl: 3 }}>
          <Typography component="li">
            <strong>Mobile:</strong> Displays as a bottom drawer with swipe-to-dismiss
          </Typography>
          <Typography component="li">
            <strong>Tablet:</strong> Shows as a centered dialog
          </Typography>
          <Typography component="li">
            <strong>Desktop:</strong> Full dialog with fullscreen toggle option
          </Typography>
        </Box>
        <Typography paragraph>
          The modal also respects user preferences for reduced motion and provides
          appropriate touch targets for different input methods.
        </Typography>
      </SmartModal>
      
      {/* Floating Action Buttons */}
      <ScrollToTopFAB threshold={200} />
      
      <ContextualFAB
        context="dashboard"
        onAction={handleFABAction}
      />
    </PageLayout>
  )
}

export default ResponsiveDemo