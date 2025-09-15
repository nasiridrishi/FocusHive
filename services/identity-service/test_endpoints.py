#!/usr/bin/env python3

import requests
import json
import time
from datetime import datetime

# Configuration
BASE_URL = "https://identity.focushive.app"
TIMESTAMP = int(time.time())

# Test accounts
ADMIN_USERNAME = "focushive-admin"
ADMIN_PASSWORD = "FocusHiveAdmin2024!"
TEST_EMAIL = f"test{TIMESTAMP}@example.com"
TEST_USERNAME = f"testuser{TIMESTAMP}"
TEST_PASSWORD = "TestPassword123@"

# Colors for output
GREEN = '\033[0;32m'
RED = '\033[0;31m'
YELLOW = '\033[1;33m'
BLUE = '\033[0;34m'
NC = '\033[0m'  # No Color

def print_header(text):
    print(f"\n{YELLOW}{text}{NC}")

def print_test(text):
    print(f"{BLUE}{text}{NC}")

def print_success(text):
    print(f"{GREEN}✅ {text}{NC}")

def print_error(text):
    print(f"{RED}❌ {text}{NC}")

def api_call(method, endpoint, data=None, token=None):
    """Make API call with proper error handling"""
    url = f"{BASE_URL}{endpoint}"
    headers = {"Content-Type": "application/json"}

    if token:
        headers["Authorization"] = f"Bearer {token}"

    try:
        if method == "GET":
            response = requests.get(url, headers=headers)
        elif method == "POST":
            response = requests.post(url, headers=headers, json=data)
        elif method == "PUT":
            response = requests.put(url, headers=headers, json=data)
        elif method == "DELETE":
            response = requests.delete(url, headers=headers)
        else:
            raise ValueError(f"Unknown method: {method}")

        return response
    except Exception as e:
        print(f"Error: {e}")
        return None

def test_authentication():
    """Test authentication endpoints"""
    print_header("1. AUTHENTICATION ENDPOINTS")

    # Register new user
    print_test("1.1 Register New User")
    register_data = {
        "username": TEST_USERNAME,
        "email": TEST_EMAIL,
        "password": TEST_PASSWORD,
        "confirmPassword": TEST_PASSWORD,
        "firstName": "Test",
        "lastName": "User"
    }

    response = api_call("POST", "/api/v1/auth/register", register_data)
    if response and response.status_code == 201:
        user_data = response.json()
        user_token = user_data.get("accessToken")
        user_refresh = user_data.get("refreshToken")
        print_success(f"Registration successful - Token: {user_token[:20]}...")
        return user_token, user_refresh
    else:
        print_error(f"Registration failed: {response.text if response else 'No response'}")
        return None, None

def test_admin_login():
    """Test admin login"""
    print_test("1.2 Admin Login")
    login_data = {
        "usernameOrEmail": ADMIN_USERNAME,
        "password": ADMIN_PASSWORD
    }

    response = api_call("POST", "/api/v1/auth/login", login_data)
    if response and response.status_code == 200:
        admin_data = response.json()
        admin_token = admin_data.get("accessToken")
        print_success(f"Admin login successful - Token: {admin_token[:20]}...")
        return admin_token
    else:
        print_error(f"Admin login failed: {response.text if response else 'No response'}")
        return None

def test_user_endpoints(token):
    """Test user management endpoints"""
    print_header("2. USER MANAGEMENT ENDPOINTS")

    # Get profile
    print_test("2.1 Get User Profile")
    response = api_call("GET", "/api/users/profile", token=token)
    if response and response.status_code == 200:
        print_success("Get profile successful")
        print(json.dumps(response.json(), indent=2)[:200])
    else:
        print_error(f"Get profile failed: {response.text if response else 'No response'}")

    # Update profile
    print_test("2.2 Update User Profile")
    update_data = {
        "firstName": "Updated",
        "lastName": "Name",
        "bio": "Test bio"
    }
    response = api_call("PUT", "/api/users/profile", update_data, token)
    if response and response.status_code == 200:
        print_success("Update profile successful")
    else:
        print_error(f"Update profile failed: {response.text if response else 'No response'}")

    # Get personas
    print_test("2.3 Get User Personas")
    response = api_call("GET", "/api/users/personas", token=token)
    if response and response.status_code == 200:
        print_success("Get personas successful")
    else:
        print_error(f"Get personas failed: {response.text if response else 'No response'}")

def test_admin_endpoints(token):
    """Test admin endpoints"""
    print_header("3. ADMIN ENDPOINTS")

    # List users
    print_test("3.1 List All Users")
    response = api_call("GET", "/api/admin/users", token=token)
    if response and response.status_code == 200:
        print_success("List users successful")
        users = response.json()
        print(f"Found {len(users) if isinstance(users, list) else 'unknown number of'} users")
    else:
        print_error(f"List users failed: {response.text if response else 'No response'}")

    # List OAuth2 clients
    print_test("3.2 List OAuth2 Clients")
    response = api_call("GET", "/api/admin/clients", token=token)
    if response and response.status_code == 200:
        print_success("List OAuth2 clients successful")
    else:
        print_error(f"List OAuth2 clients failed: {response.text if response else 'No response'}")

def test_oauth2_endpoints():
    """Test OAuth2/OIDC endpoints"""
    print_header("4. OAUTH2/OIDC ENDPOINTS")

    # JWKS
    print_test("4.1 JWKS Endpoint")
    response = requests.get(f"{BASE_URL}/.well-known/jwks.json")
    if response.status_code == 200:
        print_success("JWKS endpoint accessible")
    else:
        print_error(f"JWKS endpoint failed: {response.status_code}")

    # OpenID Configuration
    print_test("4.2 OpenID Configuration")
    response = requests.get(f"{BASE_URL}/.well-known/openid-configuration")
    if response.status_code == 200:
        print_success("OpenID configuration accessible")
    else:
        print_error(f"OpenID configuration failed: {response.status_code}")

    # OAuth2 Authorization
    print_test("4.3 OAuth2 Authorization")
    params = {
        "client_id": "test-client",
        "response_type": "code",
        "redirect_uri": "http://localhost:3000/callback",
        "scope": "openid profile email",
        "state": "test123"
    }
    response = requests.get(f"{BASE_URL}/oauth2/authorize", params=params, allow_redirects=False)
    if response.status_code == 302:
        print_success(f"OAuth2 authorization redirects correctly to: {response.headers.get('Location')}")
    else:
        print_error(f"OAuth2 authorization failed: {response.status_code}")

def test_actuator_endpoints(admin_token):
    """Test actuator endpoints"""
    print_header("5. ACTUATOR ENDPOINTS")

    # Public endpoints
    public_endpoints = [
        ("/actuator/health", "Health Check"),
        ("/actuator/info", "Info Endpoint"),
        ("/actuator/prometheus", "Prometheus Metrics")
    ]

    for endpoint, name in public_endpoints:
        print_test(f"5.{public_endpoints.index((endpoint, name)) + 1} {name}")
        response = requests.get(f"{BASE_URL}{endpoint}")
        if response.status_code == 200:
            print_success(f"{name} accessible (public)")
        else:
            print_error(f"{name} failed: {response.status_code}")

    # Admin-only endpoint
    print_test("5.4 Environment Endpoint (Admin)")
    response = api_call("GET", "/actuator/env", token=admin_token)
    if response and response.status_code == 200:
        print_success("Environment endpoint accessible with admin token")
    else:
        print_error(f"Environment endpoint failed: {response.status_code if response else 'No response'}")

def main():
    print("=" * 50)
    print(f"{BLUE}Identity Service Comprehensive Testing{NC}")
    print(f"Base URL: {BASE_URL}")
    print(f"Timestamp: {datetime.now()}")
    print("=" * 50)

    # Test authentication and get tokens
    user_token, user_refresh = test_authentication()
    admin_token = test_admin_login()

    # Test other endpoints if we have tokens
    if user_token:
        test_user_endpoints(user_token)

    if admin_token:
        test_admin_endpoints(admin_token)

    # Test OAuth2 endpoints (no auth needed)
    test_oauth2_endpoints()

    # Test actuator endpoints
    test_actuator_endpoints(admin_token)

    # Summary
    print("\n" + "=" * 50)
    print(f"{BLUE}TEST SUMMARY{NC}")
    print("=" * 50)
    print("\nAll endpoint categories have been tested.")
    print("Check the output above for specific results.")
    print(f"\nTest completed at: {datetime.now()}")

if __name__ == "__main__":
    main()