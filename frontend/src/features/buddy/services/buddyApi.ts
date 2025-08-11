import axios, { AxiosInstance } from 'axios'
import {
  BuddyRelationship,
  BuddyRequest,
  BuddyMatch,
  BuddyPreferences,
  BuddyGoal,
  BuddyCheckin,
  BuddySession,
  BuddyStats,
  MatchScore,
  CheckinStats
} from '../types'

class BuddyApiService {
  private api: AxiosInstance

  constructor() {
    this.api = axios.create({
      baseURL: '/api/buddy',
      timeout: 10000,
      headers: {
        'Content-Type': 'application/json',
      },
    })

    // Request interceptor to add auth token
    this.api.interceptors.request.use(
      (config) => {
        const token = localStorage.getItem('authToken')
        if (token) {
          config.headers.Authorization = `Bearer ${token}`
        }
        return config
      },
      (error) => Promise.reject(error)
    )

    // Response interceptor for error handling
    this.api.interceptors.response.use(
      (response) => response,
      (error) => {
        console.error('Buddy API Error:', error.response?.data || error.message)
        throw error
      }
    )
  }

  // Buddy Relationship Management
  async sendBuddyRequest(request: BuddyRequest): Promise<BuddyRelationship> {
    const response = await this.api.post('/request', request)
    return response.data
  }

  async acceptBuddyRequest(relationshipId: number): Promise<BuddyRelationship> {
    const response = await this.api.put(`/request/${relationshipId}/accept`)
    return response.data
  }

  async rejectBuddyRequest(relationshipId: number): Promise<BuddyRelationship> {
    const response = await this.api.put(`/request/${relationshipId}/reject`)
    return response.data
  }

  async terminateRelationship(relationshipId: number, reason?: string): Promise<BuddyRelationship> {
    const response = await this.api.delete(`/relationship/${relationshipId}`, {
      params: { reason }
    })
    return response.data
  }

  async getActiveBuddies(): Promise<BuddyRelationship[]> {
    const response = await this.api.get('/relationships/active')
    return response.data
  }

  async getPendingRequests(): Promise<BuddyRelationship[]> {
    const response = await this.api.get('/requests/pending')
    return response.data
  }

  async getSentRequests(): Promise<BuddyRelationship[]> {
    const response = await this.api.get('/requests/sent')
    return response.data
  }

  async getRelationship(relationshipId: number): Promise<BuddyRelationship> {
    const response = await this.api.get(`/relationship/${relationshipId}`)
    return response.data
  }

  // Buddy Matching
  async findPotentialMatches(): Promise<BuddyMatch[]> {
    const response = await this.api.get('/matches')
    return response.data
  }

  async calculateMatchScore(userId: number): Promise<MatchScore> {
    const response = await this.api.get(`/match-score/${userId}`)
    return response.data
  }

  // Buddy Preferences
  async getUserPreferences(): Promise<BuddyPreferences> {
    const response = await this.api.get('/preferences')
    return response.data
  }

  async updateUserPreferences(preferences: BuddyPreferences): Promise<BuddyPreferences> {
    const response = await this.api.put('/preferences', preferences)
    return response.data
  }

  // Buddy Goals
  async createGoal(relationshipId: number, goal: BuddyGoal): Promise<BuddyGoal> {
    const response = await this.api.post(`/relationship/${relationshipId}/goals`, goal)
    return response.data
  }

  async updateGoal(goalId: number, goal: BuddyGoal): Promise<BuddyGoal> {
    const response = await this.api.put(`/goals/${goalId}`, goal)
    return response.data
  }

  async completeGoal(goalId: number): Promise<BuddyGoal> {
    const response = await this.api.put(`/goals/${goalId}/complete`)
    return response.data
  }

  async getRelationshipGoals(relationshipId: number): Promise<BuddyGoal[]> {
    const response = await this.api.get(`/relationship/${relationshipId}/goals`)
    return response.data
  }

  async getActiveGoals(relationshipId: number): Promise<BuddyGoal[]> {
    const response = await this.api.get(`/relationship/${relationshipId}/goals/active`)
    return response.data
  }

  // Buddy Check-ins
  async createCheckin(relationshipId: number, checkin: BuddyCheckin): Promise<BuddyCheckin> {
    const response = await this.api.post(`/relationship/${relationshipId}/checkin`, checkin)
    return response.data
  }

  async getRelationshipCheckins(relationshipId: number): Promise<BuddyCheckin[]> {
    const response = await this.api.get(`/relationship/${relationshipId}/checkins`)
    return response.data
  }

  async getCheckinStats(relationshipId: number): Promise<CheckinStats> {
    const response = await this.api.get(`/relationship/${relationshipId}/checkins/stats`)
    return response.data
  }

  // Buddy Sessions
  async scheduleSession(relationshipId: number, session: BuddySession): Promise<BuddySession> {
    const response = await this.api.post(`/relationship/${relationshipId}/sessions`, session)
    return response.data
  }

  async updateSession(sessionId: number, session: BuddySession): Promise<BuddySession> {
    const response = await this.api.put(`/sessions/${sessionId}`, session)
    return response.data
  }

  async startSession(sessionId: number): Promise<BuddySession> {
    const response = await this.api.put(`/sessions/${sessionId}/start`)
    return response.data
  }

  async endSession(sessionId: number): Promise<BuddySession> {
    const response = await this.api.put(`/sessions/${sessionId}/end`)
    return response.data
  }

  async cancelSession(sessionId: number, reason?: string): Promise<BuddySession> {
    const response = await this.api.put(`/sessions/${sessionId}/cancel`, null, {
      params: { reason }
    })
    return response.data
  }

  async rateSession(sessionId: number, rating: number, feedback?: string): Promise<BuddySession> {
    const response = await this.api.post(`/sessions/${sessionId}/rate`, null, {
      params: { rating, feedback }
    })
    return response.data
  }

  async getUpcomingSessions(): Promise<BuddySession[]> {
    const response = await this.api.get('/sessions/upcoming')
    return response.data
  }

  async getRelationshipSessions(relationshipId: number): Promise<BuddySession[]> {
    const response = await this.api.get(`/relationship/${relationshipId}/sessions`)
    return response.data
  }

  // Statistics
  async getRelationshipStats(relationshipId: number): Promise<BuddyStats> {
    const response = await this.api.get(`/relationship/${relationshipId}/stats`)
    return response.data
  }

  async getUserStats(): Promise<BuddyStats> {
    const response = await this.api.get('/stats')
    return response.data
  }
}

export const buddyApi = new BuddyApiService()