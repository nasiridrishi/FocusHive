const axios = require('axios');

async function testIdentityService() {
  try {
    // Test health endpoint
    console.log('Testing health endpoint...');
    const healthResponse = await axios.get('http://localhost:8081/actuator/health');
    console.log('Health check passed:', healthResponse.data.status);

    // Test registration endpoint
    console.log('\nTesting registration endpoint...');
    const testUser = {
      email: `test-${Date.now()}@focushive.test`,
      password: 'TestPassword123!',
      confirmPassword: 'TestPassword123!',
      firstName: 'Test',
      lastName: 'User',
      acceptTerms: true
    };

    console.log('Sending registration request with:', JSON.stringify(testUser, null, 2));

    const registerResponse = await axios.post(
      'http://localhost:8081/api/v1/auth/register',
      testUser,
      {
        headers: {
          'Content-Type': 'application/json',
          'Accept': 'application/json'
        }
      }
    );
    console.log('Registration response:', {
      status: registerResponse.status,
      hasUser: !!registerResponse.data.user,
      hasTokens: !!registerResponse.data.tokens
    });

  } catch (error) {
    console.error('Error details:', {
      message: error.message,
      status: error.response?.status,
      data: error.response?.data,
      url: error.config?.url
    });
  }
}

testIdentityService();