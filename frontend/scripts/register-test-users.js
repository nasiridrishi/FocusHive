const axios = require('axios');

// Configuration
const IDENTITY_SERVICE_URL = 'http://localhost:8081';
const REGISTER_ENDPOINT = '/api/v1/auth/register';
const LOGIN_ENDPOINT = '/api/v1/auth/login';

// Test users to register
const testUsers = [
  {
    email: 'alice.focused@focushive.test',
    password: 'SecurePass123!',
    firstName: 'Alice',
    lastName: 'Focused',
    username: 'alicefocused'
  },
  {
    email: 'bob.productive@focushive.test',
    password: 'SecurePass123!',
    firstName: 'Bob',
    lastName: 'Productive',
    username: 'bobproductive'
  },
  {
    email: 'charlie.organized@focushive.test',
    password: 'SecurePass123!',
    firstName: 'Charlie',
    lastName: 'Organized',
    username: 'charlieorganized'
  },
  {
    email: 'diana.efficient@focushive.test',
    password: 'SecurePass123!',
    firstName: 'Diana',
    lastName: 'Efficient',
    username: 'dianaefficient'
  },
  {
    email: 'eve.motivated@focushive.test',
    password: 'SecurePass123!',
    firstName: 'Eve',
    lastName: 'Motivated',
    username: 'evemotivated'
  },
  {
    email: 'frank.dedicated@focushive.test',
    password: 'SecurePass123!',
    firstName: 'Frank',
    lastName: 'Dedicated',
    username: 'frankdedicated'
  }
];

// Function to register a single user
async function registerUser(user) {
  try {
    console.log(`\n👤 Registering user: ${user.firstName} ${user.lastName} (${user.email})...`);
    
    // Create registration payload matching API specification
    const payload = {
      email: user.email,
      password: user.password,
      confirmPassword: user.password,
      firstName: user.firstName,
      lastName: user.lastName,
      username: user.username
    };
    
    // Make registration request
    const response = await axios.post(
      `${IDENTITY_SERVICE_URL}${REGISTER_ENDPOINT}`,
      payload,
      {
        headers: {
          'Content-Type': 'application/json'
        },
        timeout: 10000
      }
    );
    
    console.log(`✅ Successfully registered ${user.firstName} ${user.lastName}`);
    
    // Test login to verify account works
    console.log(`🔐 Testing login for ${user.email}...`);
    const loginResponse = await axios.post(
      `${IDENTITY_SERVICE_URL}${LOGIN_ENDPOINT}`,
      {
        usernameOrEmail: user.email,
        password: user.password
      },
      {
        headers: {
          'Content-Type': 'application/json'
        },
        timeout: 10000
      }
    );
    
    console.log(`✅ Login test successful for ${user.email}`);
    
    return {
      success: true,
      user: user.email,
      message: 'Registration and login successful'
    };
    
  } catch (error) {
    if (error.response) {
      // Server responded with error status
      const status = error.response.status;
      const message = error.response.data?.message || error.response.data?.error || 'Unknown error';
      
      if (status === 409 || message.includes('already exists') || message.includes('already registered')) {
        console.log(`ℹ️  User ${user.email} already exists, skipping...`);
        return {
          success: true,
          user: user.email,
          message: 'User already exists'
        };
      } else {
        console.error(`❌ Failed to register ${user.firstName} ${user.lastName} (HTTP ${status})`);
        console.error(`Response: ${message}`);
        return {
          success: false,
          user: user.email,
          error: `HTTP ${status}: ${message}`
        };
      }
    } else if (error.request) {
      // Network error
      console.error(`❌ Network error registering ${user.firstName} ${user.lastName}`);
      console.error('Please check if the Identity Service is running');
      return {
        success: false,
        user: user.email,
        error: 'Network error'
      };
    } else {
      // Other error
      console.error(`❌ Unexpected error registering ${user.firstName} ${user.lastName}:`, error.message);
      return {
        success: false,
        user: user.email,
        error: error.message
      };
    }
  }
}

// Function to check if identity service is running
async function checkIdentityService() {
  try {
    console.log('📡 Checking Identity Service health...');
    await axios.get(`${IDENTITY_SERVICE_URL}/health`, { timeout: 5000 });
    console.log('✅ Identity Service is running\n');
    return true;
  } catch (error) {
    console.error('❌ Identity Service is not running at', IDENTITY_SERVICE_URL);
    console.error('💡 Please start the Identity Service first\n');
    return false;
  }
}

// Main function to register all test users
async function registerAllUsers() {
  console.log('🚀 Starting test user registration...\n');
  
  // Check if identity service is running
  const serviceRunning = await checkIdentityService();
  if (!serviceRunning) {
    process.exit(1);
  }
  
  const results = [];
  
  // Register users sequentially to avoid overwhelming the service
  for (const user of testUsers) {
    const result = await registerUser(user);
    results.push(result);
    
    // Small delay between registrations
    await new Promise(resolve => setTimeout(resolve, 500));
  }
  
  // Summary
  console.log('\n🎉 Test user registration process completed!');
  
  const successful = results.filter(r => r.success).length;
  const failed = results.filter(r => !r.success).length;
  
  console.log(`✅ Successfully processed: ${successful} users`);
  if (failed > 0) {
    console.log(`❌ Failed: ${failed} users`);
    console.log('\nFailed users:');
    results.filter(r => !r.success).forEach(r => {
      console.log(`  - ${r.user}: ${r.error}`);
    });
  }
  
  console.log('\n💡 You can now use these test accounts to login to FocusHive\n');
}

// Run the script if called directly
if (require.main === module) {
  registerAllUsers().catch(error => {
    console.error('💥 Script failed:', error.message);
    process.exit(1);
  });
}

module.exports = { registerAllUsers, registerUser, testUsers };