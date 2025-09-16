#!/usr/bin/env python3
"""
Test script to send notifications through RabbitMQ to the notification service
"""
import json
import pika
import sys
from datetime import datetime

def send_test_notification(event_type, message_data):
    """Send a test notification to RabbitMQ"""
    
    # Connect to RabbitMQ
    connection = pika.BlockingConnection(
        pika.ConnectionParameters('localhost', 5672)
    )
    channel = connection.channel()
    
    # Declare the exchange (should match the service configuration)
    channel.exchange_declare(
        exchange='notification.exchange',
        exchange_type='topic',
        durable=True
    )
    
    # Prepare the message
    message = {
        "eventType": event_type,
        "timestamp": datetime.now().isoformat(),
        **message_data
    }
    
    # Send the message
    routing_key = f"notification.{event_type.lower()}"
    channel.basic_publish(
        exchange='notification.exchange',
        routing_key=routing_key,
        body=json.dumps(message),
        properties=pika.BasicProperties(
            content_type='application/json',
            delivery_mode=2  # persistent
        )
    )
    
    print(f"✅ Sent {event_type} notification:")
    print(json.dumps(message, indent=2))
    
    connection.close()

def main():
    # Test 1: User Registration (from Identity Service)
    print("\n📧 Test 1: User Registration Notification")
    send_test_notification("USER_REGISTERED", {
        "userId": "test-user-001",
        "username": "johndoe",
        "email": "john.doe@example.com",
        "firstName": "John",
        "lastName": "Doe",
        "source": "identity-service"
    })
    
    # Test 2: Hive Invitation (from Backend Service)
    print("\n📧 Test 2: Hive Invitation Notification")
    send_test_notification("HIVE_INVITATION_SENT", {
        "inviteeId": "test-user-002",
        "inviterId": "test-user-001",
        "inviterName": "John Doe",
        "inviteeName": "Jane Smith",
        "hiveId": "hive-123",
        "hiveName": "Study Group Alpha",
        "invitationUrl": "https://focushive.com/hive/join/abc123",
        "message": "Join our study group!",
        "source": "focushive-backend"
    })
    
    # Test 3: Buddy Match (from Buddy Service)
    print("\n📧 Test 3: Buddy Match Notification")
    send_test_notification("BUDDY_MATCHED", {
        "user1Id": "test-user-001",
        "user2Id": "test-user-002",
        "user1Name": "John Doe",
        "user2Name": "Jane Smith",
        "matchId": "match-456",
        "compatibilityScore": 92,
        "commonInterests": ["programming", "AI", "coffee"],
        "chatUrl": "https://focushive.com/chat/buddy-match-456",
        "source": "buddy-service"
    })
    
    # Test 4: Achievement Unlocked (from Analytics Service)
    print("\n📧 Test 4: Achievement Unlocked Notification")
    send_test_notification("ACHIEVEMENT_UNLOCKED", {
        "userId": "test-user-001",
        "userName": "John Doe",
        "achievementId": "achievement-789",
        "achievementName": "Focus Master",
        "achievementDescription": "Completed 100 focus sessions",
        "pointsEarned": 1000,
        "totalPoints": 5500,
        "rarity": "LEGENDARY",
        "badgeUrl": "https://focushive.com/badges/focus-master.png",
        "shareUrl": "https://focushive.com/achievements/share/achievement-789",
        "source": "analytics-service"
    })
    
    # Test 5: Hive Activity (from Backend Service)
    print("\n📧 Test 5: Hive Activity Notification")
    send_test_notification("HIVE_ACTIVITY", {
        "userId": "test-user-001",
        "hiveId": "hive-123",
        "hiveName": "Study Group Alpha",
        "activityType": "SESSION_STARTED",
        "source": "focushive-backend"
    })
    
    print("\n✨ All test notifications sent successfully!")
    print("Check the notification service logs to verify processing.")

if __name__ == "__main__":
    try:
        main()
    except Exception as e:
        print(f"❌ Error: {e}")
        sys.exit(1)