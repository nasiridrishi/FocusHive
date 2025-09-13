/**
 * Test Fixtures for Identity Service E2E Tests
 * 
 * Provides test data, mock personas, and OAuth2 clients for comprehensive testing
 * Supports data generation, cleanup, and state management across test runs
 * 
 * @fileoverview Identity service test fixtures and data factories
 * @version 1.0.0
 */

import { faker } from '@faker-js/faker';
import type { Page, APIRequestContext } from '@playwright/test';
import { IDENTITY_API, TEST_USERS, PERSONA_TEMPLATES, OAUTH2_TEST_CLIENTS } from '../tests/identity/identity.config';

/**
 * User account data structure
 */
export interface TestUser {
  id?: string;
  email: string;
  password: string;
  displayName: string;
  firstName?: string;
  lastName?: string;
  avatarUrl?: string;
  createdAt?: string;
  personas?: TestPersona[];
  accessToken?: string;
  refreshToken?: string;
}

/**
 * Persona data structure
 */
export interface TestPersona {
  id?: string;
  userId?: string;
  name: string;
  type: PersonaType;
  displayName: string;
  bio?: string;
  avatarUrl?: string;
  isDefault: boolean;
  isActive: boolean;
  privacySettings: PrivacySettings;
  customAttributes: Record<string, string>;
  notificationPreferences: Record<string, boolean>;
  themePreference: string;
  language: string;
  timezone: string;
}

/**
 * Persona types matching backend enum
 */
export type PersonaType = 'PROFESSIONAL' | 'PERSONAL' | 'ACADEMIC' | 'SOCIAL' | 'GAMING' | 'CREATIVE';

/**
 * Privacy settings structure
 */
export interface PrivacySettings {
  profileVisibility: 'PUBLIC' | 'FRIENDS' | 'PRIVATE';
  showOnlineStatus: boolean;
  allowMessagesFrom: 'EVERYONE' | 'FRIENDS' | 'NOBODY';
  shareActivityData: boolean;
  allowDataExport: boolean;
  twoFactorEnabled: boolean;
  sessionTimeout: number;
}

/**
 * OAuth2 client data structure
 */
export interface TestOAuth2Client {
  clientId: string;
  clientSecret?: string;
  name: string;
  description?: string;
  redirectUris: string[];
  scopes: string[];
  grantTypes: string[];
  pkce?: boolean;
  confidential?: boolean;
  autoApprove?: boolean;
  accessTokenValidity?: number;
  refreshTokenValidity?: number;
}

/**
 * Data export request structure
 */
export interface DataExportRequest {
  id?: string;
  userId: string;
  requestType: 'PROFILE' | 'PERSONAS' | 'ACTIVITY' | 'FULL';
  format: 'JSON' | 'CSV' | 'XML';
  status: 'PENDING' | 'PROCESSING' | 'COMPLETED' | 'FAILED';
  downloadUrl?: string;
  expiresAt?: string;
  requestedAt: string;
}

/**
 * User data structure returned from login API
 */
export interface LoginUserResponse {
  id: string;
  email: string;
  displayName: string;
  firstName?: string;
  lastName?: string;
  avatarUrl?: string;
  createdAt: string;
  lastLoginAt?: string;
  roles: string[];
}

/**
 * Login response structure
 */
export interface LoginResponse {
  accessToken: string;
  refreshToken: string;
  user: LoginUserResponse;
  expiresIn: number;
  tokenType: string;
}

/**
 * Factory class for generating test data
 */
export class IdentityTestDataFactory {
  
  /**
   * Generate a test user with random data
   */
  static createUser(overrides: Partial<TestUser> = {}): TestUser {
    const firstName = faker.person.firstName();
    const lastName = faker.person.lastName();
    
    return {
      email: overrides.email || faker.internet.email({ firstName, lastName }).toLowerCase(),
      password: overrides.password || 'TestPassword123!',
      displayName: overrides.displayName || `${firstName} ${lastName}`,
      firstName,
      lastName,
      avatarUrl: faker.image.avatar(),
      personas: [],
      ...overrides
    };
  }

  /**
   * Generate a test persona with random data
   */
  static createPersona(type: PersonaType = 'PERSONAL', overrides: Partial<TestPersona> = {}): TestPersona {
    const name = overrides.name || faker.word.adjective();
    
    return {
      name,
      type,
      displayName: overrides.displayName || `${name} Persona`,
      bio: faker.lorem.sentence(),
      avatarUrl: faker.image.avatar(),
      isDefault: overrides.isDefault ?? false,
      isActive: overrides.isActive ?? false,
      privacySettings: this.createPrivacySettings(),
      customAttributes: {
        occupation: faker.person.jobTitle(),
        location: faker.location.city(),
        interests: faker.word.words(3)
      },
      notificationPreferences: {
        email: true,
        push: type === 'PROFESSIONAL',
        desktop: type !== 'GAMING',
        marketing: type === 'SOCIAL'
      },
      themePreference: faker.helpers.arrayElement(['light', 'dark', 'auto']),
      language: 'en',
      timezone: faker.date.timeZone(),
      ...overrides
    };
  }

  /**
   * Generate privacy settings
   */
  static createPrivacySettings(overrides: Partial<PrivacySettings> = {}): PrivacySettings {
    return {
      profileVisibility: 'FRIENDS',
      showOnlineStatus: true,
      allowMessagesFrom: 'FRIENDS',
      shareActivityData: false,
      allowDataExport: true,
      twoFactorEnabled: false,
      sessionTimeout: 3600,
      ...overrides
    };
  }

  /**
   * Generate OAuth2 client configuration
   */
  static createOAuth2Client(overrides: Partial<TestOAuth2Client> = {}): TestOAuth2Client {
    const name = overrides.name || faker.company.name();
    
    return {
      clientId: overrides.clientId || `test-${faker.string.alphanumeric(8)}`,
      clientSecret: overrides.clientSecret || faker.string.alphanumeric(32),
      name,
      description: faker.company.catchPhrase(),
      redirectUris: ['http://localhost:3000/callback'],
      scopes: ['profile', 'email'],
      grantTypes: ['authorization_code', 'refresh_token'],
      pkce: false,
      confidential: true,
      autoApprove: false,
      accessTokenValidity: 3600,
      refreshTokenValidity: 86400,
      ...overrides
    };
  }

  /**
   * Create a complete user with multiple personas
   */
  static createMultiPersonaUser(): TestUser & { personas: TestPersona[] } {
    const user = this.createUser(TEST_USERS.MULTI_PERSONA_USER);
    
    const personas = [
      this.createPersona('PROFESSIONAL', { 
        name: 'Work', 
        isDefault: true, 
        isActive: true,
        privacySettings: this.createPrivacySettings({ profileVisibility: 'PUBLIC' })
      }),
      this.createPersona('PERSONAL', { 
        name: 'Personal',
        privacySettings: this.createPrivacySettings({ profileVisibility: 'FRIENDS' })
      }),
      this.createPersona('ACADEMIC', { 
        name: 'Study',
        privacySettings: this.createPrivacySettings({ shareActivityData: true })
      })
    ];

    return { ...user, personas };
  }

  /**
   * Create enterprise user with complex persona setup
   */
  static createEnterpriseUser(): TestUser & { personas: TestPersona[] } {
    const user = this.createUser(TEST_USERS.ENTERPRISE_USER);
    
    const personas = [
      this.createPersona('PROFESSIONAL', { 
        name: 'Work',
        isDefault: true,
        privacySettings: this.createPrivacySettings({ 
          profileVisibility: 'PUBLIC',
          twoFactorEnabled: true,
          sessionTimeout: 1800
        })
      }),
      this.createPersona('PERSONAL', { 
        name: 'Personal',
        privacySettings: this.createPrivacySettings({ profileVisibility: 'PRIVATE' })
      }),
      this.createPersona('PROFESSIONAL', { 
        name: 'Admin',
        privacySettings: this.createPrivacySettings({ 
          profileVisibility: 'FRIENDS',
          twoFactorEnabled: true
        })
      }),
      this.createPersona('SOCIAL', { 
        name: 'Guest',
        privacySettings: this.createPrivacySettings({ 
          profileVisibility: 'PUBLIC',
          shareActivityData: false
        })
      })
    ];

    return { ...user, personas };
  }
}

/**
 * Test data manager for setup and cleanup
 */
export class IdentityTestDataManager {
  private page: Page;
  private apiContext: APIRequestContext;
  private createdUsers: TestUser[] = [];
  private createdClients: TestOAuth2Client[] = [];

  constructor(page: Page, apiContext: APIRequestContext) {
    this.page = page;
    this.apiContext = apiContext;
  }

  /**
   * Create and register a test user
   */
  async createUser(userData?: Partial<TestUser>): Promise<TestUser> {
    const user = IdentityTestDataFactory.createUser(userData);
    
    // Register user via API
    const response = await this.apiContext.post(`${IDENTITY_API.BASE_URL}${IDENTITY_API.ENDPOINTS.REGISTER}`, {
      data: {
        email: user.email,
        password: user.password,
        displayName: user.displayName,
        firstName: user.firstName,
        lastName: user.lastName
      }
    });

    if (!response.ok()) {
      throw new Error(`Failed to create user: ${response.status()} ${await response.text()}`);
    }

    const result = await response.json();
    user.id = result.user?.id;
    user.accessToken = result.accessToken;
    user.refreshToken = result.refreshToken;

    this.createdUsers.push(user);
    return user;
  }

  /**
   * Create personas for a user
   */
  async createPersonas(userId: string, personas: TestPersona[], accessToken: string): Promise<TestPersona[]> {
    const createdPersonas: TestPersona[] = [];

    for (const personaData of personas) {
      const response = await this.apiContext.post(`${IDENTITY_API.BASE_URL}${IDENTITY_API.ENDPOINTS.PERSONAS}`, {
        headers: {
          'Authorization': `Bearer ${accessToken}`,
          'Content-Type': 'application/json'
        },
        data: {
          ...personaData,
          userId
        }
      });

      if (!response.ok()) {
        throw new Error(`Failed to create persona: ${response.status()} ${await response.text()}`);
      }

      const result = await response.json();
      createdPersonas.push({ ...personaData, id: result.id, userId });
    }

    return createdPersonas;
  }

  /**
   * Create OAuth2 client
   */
  async createOAuth2Client(clientData: TestOAuth2Client, accessToken: string): Promise<TestOAuth2Client> {
    const response = await this.apiContext.post(`${IDENTITY_API.BASE_URL}${IDENTITY_API.ENDPOINTS.OAUTH2_CLIENTS}`, {
      headers: {
        'Authorization': `Bearer ${accessToken}`,
        'Content-Type': 'application/json'
      },
      data: clientData
    });

    if (!response.ok()) {
      throw new Error(`Failed to create OAuth2 client: ${response.status()} ${await response.text()}`);
    }

    this.createdClients.push(clientData);
    return clientData;
  }

  /**
   * Login as a user and return tokens
   */
  async loginUser(email: string, password: string): Promise<LoginResponse> {
    const response = await this.apiContext.post(`${IDENTITY_API.BASE_URL}${IDENTITY_API.ENDPOINTS.LOGIN}`, {
      data: { email, password }
    });

    if (!response.ok()) {
      throw new Error(`Failed to login: ${response.status()} ${await response.text()}`);
    }

    return await response.json();
  }

  /**
   * Setup complete test environment with users and personas
   */
  async setupTestEnvironment(): Promise<{
    multiPersonaUser: TestUser & { personas: TestPersona[] };
    enterpriseUser: TestUser & { personas: TestPersona[] };
    privacyUser: TestUser;
    oauthDevUser: TestUser;
  }> {
    // Create multi-persona user
    const multiPersonaUserData = IdentityTestDataFactory.createMultiPersonaUser();
    const multiPersonaUser = await this.createUser(multiPersonaUserData);
    multiPersonaUser.personas = await this.createPersonas(
      multiPersonaUser.id!,
      multiPersonaUserData.personas,
      multiPersonaUser.accessToken!
    );

    // Create enterprise user  
    const enterpriseUserData = IdentityTestDataFactory.createEnterpriseUser();
    const enterpriseUser = await this.createUser(enterpriseUserData);
    enterpriseUser.personas = await this.createPersonas(
      enterpriseUser.id!,
      enterpriseUserData.personas,
      enterpriseUser.accessToken!
    );

    // Create privacy-focused user
    const privacyUser = await this.createUser(TEST_USERS.PRIVACY_USER);

    // Create OAuth developer user
    const oauthDevUser = await this.createUser(TEST_USERS.OAUTH_DEV_USER);
    
    // Create test OAuth2 clients for the developer
    await this.createOAuth2Client(
      IdentityTestDataFactory.createOAuth2Client(OAUTH2_TEST_CLIENTS.WEB_APP),
      oauthDevUser.accessToken!
    );

    return {
      multiPersonaUser: multiPersonaUser as TestUser & { personas: TestPersona[] },
      enterpriseUser: enterpriseUser as TestUser & { personas: TestPersona[] },
      privacyUser,
      oauthDevUser
    };
  }

  /**
   * Cleanup all created test data
   */
  async cleanup(): Promise<void> {
    // Cleanup OAuth2 clients
    for (const client of this.createdClients) {
      try {
        await this.apiContext.delete(`${IDENTITY_API.BASE_URL}${IDENTITY_API.ENDPOINTS.OAUTH2_CLIENTS}/${client.clientId}`);
      } catch (error) {
        console.warn(`Failed to cleanup OAuth2 client ${client.clientId}:`, error);
      }
    }

    // Cleanup users (this should cascade to personas)
    for (const user of this.createdUsers) {
      try {
        if (user.id) {
          await this.apiContext.delete(`${IDENTITY_API.BASE_URL}/api/v1/users/${user.id}`, {
            headers: {
              'Authorization': `Bearer ${user.accessToken}`
            }
          });
        }
      } catch (error) {
        console.warn(`Failed to cleanup user ${user.email}:`, error);
      }
    }

    // Clear tracking arrays
    this.createdUsers = [];
    this.createdClients = [];
  }
}

/**
 * Pre-defined test scenarios data
 */
export const TEST_SCENARIOS = {
  PERSONA_SWITCHING: {
    description: 'User switches between work and personal personas',
    personas: [
      IdentityTestDataFactory.createPersona('PROFESSIONAL', { name: 'Work', isActive: true }),
      IdentityTestDataFactory.createPersona('PERSONAL', { name: 'Personal' })
    ]
  },

  PRIVACY_LEVELS: {
    description: 'Different privacy configurations across personas',
    personas: [
      IdentityTestDataFactory.createPersona('PROFESSIONAL', {
        name: 'Public Work',
        privacySettings: IdentityTestDataFactory.createPrivacySettings({ profileVisibility: 'PUBLIC' })
      }),
      IdentityTestDataFactory.createPersona('PERSONAL', {
        name: 'Private Personal',
        privacySettings: IdentityTestDataFactory.createPrivacySettings({ profileVisibility: 'PRIVATE' })
      })
    ]
  },

  OAUTH_AUTHORIZATION: {
    description: 'OAuth2 authorization flow with different scopes',
    clients: [
      IdentityTestDataFactory.createOAuth2Client({
        name: 'Full Access App',
        scopes: ['profile', 'email', 'personas.read', 'personas.write']
      }),
      IdentityTestDataFactory.createOAuth2Client({
        name: 'Limited Access App',
        scopes: ['profile']
      })
    ]
  },

  CONCURRENT_SESSIONS: {
    description: 'Multiple persona sessions active simultaneously',
    sessionCount: 3,
    personas: [
      IdentityTestDataFactory.createPersona('PROFESSIONAL', { name: 'Work' }),
      IdentityTestDataFactory.createPersona('PERSONAL', { name: 'Personal' }),
      IdentityTestDataFactory.createPersona('ACADEMIC', { name: 'Study' })
    ]
  }
} as const;