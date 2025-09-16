import {OverridableComponent} from '@mui/material/OverridableComponent';
import {SvgIconTypeMap} from '@mui/material';

export type IconComponent = OverridableComponent<SvgIconTypeMap<object, 'svg'>>;

export const iconMap = {
  // Navigation
  Home: 'Home',
  Dashboard: 'Dashboard',
  Menu: 'Menu',
  Close: 'Close',
  ArrowBack: 'ArrowBack',
  ArrowForward: 'ArrowForward',
  ExpandMore: 'ExpandMore',
  ExpandLess: 'ExpandLess',
  MoreVert: 'MoreVert',
  MoreHoriz: 'MoreHoriz',

  // Actions
  Add: 'Add',
  Edit: 'Edit',
  Delete: 'Delete',
  Save: 'Save',
  Cancel: 'Cancel',
  Check: 'Check',
  Clear: 'Clear',
  Refresh: 'Refresh',
  Search: 'Search',
  Settings: 'Settings',

  // Media
  PlayArrow: 'PlayArrow',
  Pause: 'Pause',
  Stop: 'Stop',
  SkipNext: 'SkipNext',
  SkipPrevious: 'SkipPrevious',
  VolumeUp: 'VolumeUp',
  VolumeOff: 'VolumeOff',
  MusicNote: 'MusicNote',

  // Social
  Person: 'Person',
  People: 'People',
  Group: 'Group',
  Chat: 'Chat',
  Forum: 'Forum',
  Notifications: 'Notifications',

  // Status
  Info: 'Info',
  Warning: 'Warning',
  Error: 'Error',
  CheckCircle: 'CheckCircle',
  HelpOutline: 'HelpOutline',

  // File & Content
  Article: 'Article',
  Description: 'Description',
  Folder: 'Folder',
  InsertDriveFile: 'InsertDriveFile',

  // Misc
  Timer: 'Timer',
  Schedule: 'Schedule',
  TrendingUp: 'TrendingUp',
  EmojiEvents: 'EmojiEvents',
  Psychology: 'Psychology',
  Lightbulb: 'Lightbulb',
  DarkMode: 'DarkMode',
  LightMode: 'LightMode',
  Language: 'Language',
} as const;

export type IconName = keyof typeof iconMap;