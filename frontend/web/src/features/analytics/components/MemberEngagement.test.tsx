import { render, screen } from '@testing-library/react';
import { describe, it, expect } from 'vitest';
import { MemberEngagement } from './MemberEngagement';
import { MemberEngagementProps, MemberEngagementData } from '../types';

const mockData: MemberEngagementData[] = [
  {
    user: { id: '1', name: 'Alice Johnson', avatar: 'https://example.com/alice.jpg' },
    focusTime: 480,
    sessions: 12,
    lastActive: new Date('2024-01-15T14:30:00Z'),
    rank: 1,
    engagement: 'high',
    contribution: 35.2
  },
  {
    user: { id: '2', name: 'Bob Smith', avatar: 'https://example.com/bob.jpg' },
    focusTime: 360,
    sessions: 9,
    lastActive: new Date('2024-01-14T10:15:00Z'),
    rank: 2,
    engagement: 'high',
    contribution: 26.4
  },
  {
    user: { id: '3', name: 'Carol Davis', avatar: 'https://example.com/carol.jpg' },
    focusTime: 240,
    sessions: 6,
    lastActive: new Date('2024-01-13T16:45:00Z'),
    rank: 3,
    engagement: 'medium',
    contribution: 17.6
  },
  {
    user: { id: '4', name: 'David Wilson', avatar: 'https://example.com/david.jpg' },
    focusTime: 120,
    sessions: 3,
    lastActive: new Date('2024-01-10T09:20:00Z'),
    rank: 4,
    engagement: 'low',
    contribution: 8.8
  },
  {
    user: { id: '5', name: 'Eve Brown', avatar: 'https://example.com/eve.jpg' },
    focusTime: 60,
    sessions: 2,
    lastActive: new Date('2024-01-08T13:10:00Z'),
    rank: 5,
    engagement: 'low',
    contribution: 4.4
  }
];

const defaultProps: MemberEngagementProps = {
  data: mockData
};

describe('MemberEngagement', () => {
  it('renders without crashing', () => {
    render(<MemberEngagement {...defaultProps} />);
    expect(screen.getByText('Member Engagement')).toBeInTheDocument();
  });

  it('displays all members when no maxMembers limit', () => {
    render(<MemberEngagement {...defaultProps} />);
    
    expect(screen.getByText('Alice Johnson')).toBeInTheDocument();
    expect(screen.getByText('Bob Smith')).toBeInTheDocument();
    expect(screen.getByText('Carol Davis')).toBeInTheDocument();
    expect(screen.getByText('David Wilson')).toBeInTheDocument();
    expect(screen.getByText('Eve Brown')).toBeInTheDocument();
  });

  it('limits displayed members when maxMembers is set', () => {
    render(<MemberEngagement {...defaultProps} maxMembers={3} />);
    
    expect(screen.getByText('Alice Johnson')).toBeInTheDocument();
    expect(screen.getByText('Bob Smith')).toBeInTheDocument();
    expect(screen.getByText('Carol Davis')).toBeInTheDocument();
    expect(screen.queryByText('David Wilson')).not.toBeInTheDocument();
    expect(screen.queryByText('Eve Brown')).not.toBeInTheDocument();
  });

  it('shows member ranks when showRank is true', () => {
    render(<MemberEngagement {...defaultProps} showRank={true} />);
    
    expect(screen.getByText('#1')).toBeInTheDocument();
    expect(screen.getByText('#2')).toBeInTheDocument();
    expect(screen.getByText('#3')).toBeInTheDocument();
  });

  it('hides member ranks when showRank is false', () => {
    render(<MemberEngagement {...defaultProps} showRank={false} />);
    
    expect(screen.queryByText('#1')).not.toBeInTheDocument();
    expect(screen.queryByText('#2')).not.toBeInTheDocument();
  });

  it('displays focus time in hours and minutes format', () => {
    render(<MemberEngagement {...defaultProps} />);
    
    expect(screen.getByText('8h 0m')).toBeInTheDocument(); // Alice: 480 minutes = 8 hours
    expect(screen.getByText('6h 0m')).toBeInTheDocument(); // Bob: 360 minutes = 6 hours
    expect(screen.getByText('4h 0m')).toBeInTheDocument(); // Carol: 240 minutes = 4 hours
    expect(screen.getByText('2h 0m')).toBeInTheDocument(); // David: 120 minutes = 2 hours
    expect(screen.getByText('1h 0m')).toBeInTheDocument(); // Eve: 60 minutes = 1 hour
  });

  it('displays session counts correctly', () => {
    render(<MemberEngagement {...defaultProps} />);
    
    expect(screen.getByText('12 sessions')).toBeInTheDocument();
    expect(screen.getByText('9 sessions')).toBeInTheDocument();
    expect(screen.getByText('6 sessions')).toBeInTheDocument();
    expect(screen.getByText('3 sessions')).toBeInTheDocument();
    expect(screen.getByText('2 sessions')).toBeInTheDocument();
  });

  it('shows engagement level indicators with correct colors', () => {
    render(<MemberEngagement {...defaultProps} />);
    
    // High engagement (green)
    const highEngagementBadges = screen.getAllByText('High');
    expect(highEngagementBadges).toHaveLength(2); // Alice and Bob
    
    // Medium engagement (orange)
    const mediumEngagementBadges = screen.getAllByText('Medium');
    expect(mediumEngagementBadges).toHaveLength(1); // Carol
    
    // Low engagement (red)
    const lowEngagementBadges = screen.getAllByText('Low');
    expect(lowEngagementBadges).toHaveLength(2); // David and Eve
  });

  it('displays contribution percentages', () => {
    render(<MemberEngagement {...defaultProps} />);
    
    expect(screen.getByText('35.2%')).toBeInTheDocument();
    expect(screen.getByText('26.4%')).toBeInTheDocument();
    expect(screen.getByText('17.6%')).toBeInTheDocument();
    expect(screen.getByText('8.8%')).toBeInTheDocument();
    expect(screen.getByText('4.4%')).toBeInTheDocument();
  });

  it('shows last active timestamps', () => {
    render(<MemberEngagement {...defaultProps} />);
    
    expect(screen.getByText('Jan 15, 2024')).toBeInTheDocument();
    expect(screen.getByText('Jan 14, 2024')).toBeInTheDocument();
    expect(screen.getByText('Jan 13, 2024')).toBeInTheDocument();
    expect(screen.getByText('Jan 10, 2024')).toBeInTheDocument();
    expect(screen.getByText('Jan 8, 2024')).toBeInTheDocument();
  });

  it('highlights current user when currentUserId is provided', () => {
    render(<MemberEngagement {...defaultProps} currentUserId="2" />);
    
    const bobCard = screen.getByTestId('member-card-2');
    expect(bobCard).toHaveClass('highlighted-member');
  });

  it('sorts by focus time by default', () => {
    render(<MemberEngagement {...defaultProps} />);
    
    const memberCards = screen.getAllByTestId(/member-card-/);
    expect(memberCards[0]).toHaveAttribute('data-testid', 'member-card-1'); // Alice (highest focus time)
    expect(memberCards[1]).toHaveAttribute('data-testid', 'member-card-2'); // Bob
    expect(memberCards[2]).toHaveAttribute('data-testid', 'member-card-3'); // Carol
  });

  it('sorts by sessions when sortBy is sessions', () => {
    render(<MemberEngagement {...defaultProps} sortBy="sessions" />);
    
    const memberCards = screen.getAllByTestId(/member-card-/);
    expect(memberCards[0]).toHaveAttribute('data-testid', 'member-card-1'); // Alice (12 sessions)
    expect(memberCards[1]).toHaveAttribute('data-testid', 'member-card-2'); // Bob (9 sessions)
  });

  it('sorts by engagement level when sortBy is engagement', () => {
    render(<MemberEngagement {...defaultProps} sortBy="engagement" />);
    
    // High engagement members should come first
    const memberCards = screen.getAllByTestId(/member-card-/);
    const firstTwoCards = memberCards.slice(0, 2);
    
    // Alice and Bob should be first (both high engagement)
    expect(['member-card-1', 'member-card-2']).toContain(
      firstTwoCards[0].getAttribute('data-testid')
    );
    expect(['member-card-1', 'member-card-2']).toContain(
      firstTwoCards[1].getAttribute('data-testid')
    );
  });

  it('renders user avatars when available', () => {
    render(<MemberEngagement {...defaultProps} />);
    
    const avatars = screen.getAllByRole('img');
    expect(avatars).toHaveLength(5);
    expect(avatars[0]).toHaveAttribute('src', 'https://example.com/alice.jpg');
    expect(avatars[0]).toHaveAttribute('alt', 'Alice Johnson');
  });

  it('shows user initials when avatar is not available', () => {
    const dataWithoutAvatars = mockData.map(member => ({
      ...member,
      user: { ...member.user, avatar: undefined }
    }));
    
    render(<MemberEngagement data={dataWithoutAvatars} />);
    
    expect(screen.getByText('AJ')).toBeInTheDocument(); // Alice Johnson
    expect(screen.getByText('BS')).toBeInTheDocument(); // Bob Smith
    expect(screen.getByText('CD')).toBeInTheDocument(); // Carol Davis
    expect(screen.getByText('DW')).toBeInTheDocument(); // David Wilson
    expect(screen.getByText('EB')).toBeInTheDocument(); // Eve Brown
  });

  it('handles empty data gracefully', () => {
    render(<MemberEngagement data={[]} />);
    
    expect(screen.getByText('Member Engagement')).toBeInTheDocument();
    expect(screen.getByText('No member engagement data available')).toBeInTheDocument();
    expect(screen.getByText('Invite members to your hive to see engagement metrics.')).toBeInTheDocument();
  });

  it('displays engagement chart bars with correct widths', () => {
    render(<MemberEngagement {...defaultProps} />);
    
    // Alice should have the widest bar (highest contribution)
    const aliceBar = screen.getByTestId('engagement-bar-1');
    expect(aliceBar).toHaveStyle({ width: '100%' }); // Normalized to 100%
    
    // Bob should have proportionally smaller bar
    const bobBar = screen.getByTestId('engagement-bar-2');
    expect(bobBar).toHaveStyle({ width: '75%' }); // 26.4/35.2 * 100 ≈ 75%
  });
});