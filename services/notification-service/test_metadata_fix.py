#!/usr/bin/env python3

import requests
import json
import time

def test_metadata_fix():
    """
    Test the metadata fix by sending a notification request 
    that mimics what the identity service would send
    """
    
    # Notification service endpoint
    url = "https://notification.focushive.app/api/v1/notifications"
    
    # Headers with API key (simulating identity service)
    headers = {
        "Content-Type": "application/json",
        "X-API-Key": "identity-service-secret-key-placeholder"  # This would be the real API key
    }
    
    # Payload that mimics identity service notification with metadata
    payload = {
        "userId": "test-user-12345",
        "type": "PASSWORD_RESET", 
        "title": "🎯 METADATA ROUTING TEST",
        "content": "Testing if metadata with userEmail now routes to email queue!",
        "priority": "HIGH",
        "forceDelivery": True,
        "language": "en",
        "metadata": {
            "source": "identity-service",
            "userEmail": "test@focushive.app", 
            "notificationType": "password_reset",
            "timestamp": "2025-09-21T21:32:00Z",
            "trackingId": "test-123"
        }
    }
    
    print("🧪 Testing Metadata Fix for Email Routing")
    print("=" * 50)
    print(f"URL: {url}")
    print(f"Payload: {json.dumps(payload, indent=2)}")
    print()
    
    try:
        response = requests.post(url, headers=headers, json=payload, timeout=10)
        
        print(f"Response Status: {response.status_code}")
        print(f"Response Headers: {dict(response.headers)}")
        print(f"Response Body: {response.text}")
        
        if response.status_code == 201:
            print("✅ SUCCESS: Notification created successfully!")
            print("📧 Check logs to see if metadata was processed and routed to email queue")
        elif response.status_code == 401:
            print("🔒 Expected: Authentication required (API key not valid)")
        elif response.status_code == 502:
            print("⚠️  Service temporarily unavailable")
        else:
            print(f"❌ Unexpected response: {response.status_code}")
            
    except requests.exceptions.RequestException as e:
        print(f"❌ Request failed: {e}")
    
    print("\n" + "=" * 50)
    print("Test completed. Check notification service logs for:")
    print("1. 'METADATA DEBUG: Processing metadata as Map' - indicates metadata was processed")
    print("2. 'Routing notification ... to EMAIL queue' - indicates email routing worked")
    print("3. 'notification.email.send' routing key - confirms email queue usage")

if __name__ == "__main__":
    test_metadata_fix()