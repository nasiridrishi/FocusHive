#!/usr/bin/env python3
"""
Generate JWT tokens for testing Buddy Service endpoints.
Requires: pip install pyjwt
"""

import jwt
import sys
from datetime import datetime, timedelta

def generate_token(user_id="test-user-1", role="USER", hours=24):
    # Read secret from .env file
    import os
    from pathlib import Path

    env_file = Path('.env')
    if env_file.exists():
        with open(env_file) as f:
            for line in f:
                if line.startswith('JWT_SECRET='):
                    secret = line.split('=', 1)[1].strip()
                    break
    else:
        secret = "production-secret-key-change-in-real-deployment-ultra-secure-256-bit"

    payload = {
        "sub": user_id,
        "userId": user_id,
        "roles": [role],
        "iat": datetime.utcnow(),
        "exp": datetime.utcnow() + timedelta(hours=hours)
    }

    token = jwt.encode(payload, secret, algorithm="HS256")
    return token

if __name__ == "__main__":
    user_id = sys.argv[1] if len(sys.argv) > 1 else "test-user-1"
    role = sys.argv[2] if len(sys.argv) > 2 else "USER"
    hours = int(sys.argv[3]) if len(sys.argv) > 3 else 24

    token = generate_token(user_id, role, hours)

    print(f"JWT Token for {user_id} with role {role}:")
    print(token)
    print()
    print("Usage:")
    print(f'export TOKEN="{token}"')
    print('curl -H "Authorization: Bearer $TOKEN" http://localhost:8087/api/v1/buddy/...')