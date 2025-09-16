import axios from 'axios';
import { SERVICE_URLS, API_ENDPOINTS } from '../../../config/apiConfig';
import type {
  Persona,
  PersonaCreateRequest,
  PersonaUpdateRequest,
  PersonaSwitchResponse
} from '../types';

const API_BASE_URL = SERVICE_URLS.IDENTITY;

export const identityService = {
  // Get all personas for the current user
  async getPersonas(): Promise<Persona[]> {
    const response = await axios.get<Persona[]>(
      `${API_BASE_URL}${API_ENDPOINTS.PERSONAS.BASE}`
    );
    return response.data;
  },

  // Get a specific persona by ID
  async getPersonaById(id: string): Promise<Persona> {
    const url = API_ENDPOINTS.PERSONAS.BY_ID.replace(':id', id);
    const response = await axios.get<Persona>(`${API_BASE_URL}${url}`);
    return response.data;
  },

  // Switch to a different persona
  async switchPersona(id: string): Promise<Persona> {
    const url = API_ENDPOINTS.PERSONAS.SWITCH.replace(':id', id);
    const response = await axios.post<PersonaSwitchResponse>(`${API_BASE_URL}${url}`);
    return response.data.persona;
  },

  // Create a new persona
  async createPersona(data: PersonaCreateRequest): Promise<Persona> {
    const response = await axios.post<Persona>(
      `${API_BASE_URL}${API_ENDPOINTS.PERSONAS.CREATE}`,
      data
    );
    return response.data;
  },

  // Update an existing persona
  async updatePersona(id: string, data: PersonaUpdateRequest): Promise<Persona> {
    const url = API_ENDPOINTS.PERSONAS.UPDATE.replace(':id', id);
    const response = await axios.put<Persona>(`${API_BASE_URL}${url}`, data);
    return response.data;
  },

  // Delete a persona
  async deletePersona(id: string): Promise<void> {
    const url = API_ENDPOINTS.PERSONAS.DELETE.replace(':id', id);
    await axios.delete(`${API_BASE_URL}${url}`);
  },

  // Get current active persona
  async getCurrentPersona(): Promise<Persona | null> {
    try {
      const personas = await this.getPersonas();
      return personas.find(p => p.isActive) || null;
    } catch (error) {
      console.error('Failed to get current persona:', error);
      return null;
    }
  }
};

// Export all functions for mocking in tests
export const {
  getPersonas,
  getPersonaById,
  switchPersona,
  createPersona,
  updatePersona,
  deletePersona,
  getCurrentPersona
} = identityService;