export type PersonaType = 'work' | 'study' | 'personal' | 'gaming' | 'creative';

export interface PersonaSettings {
  theme?: string;
  notifications?: boolean;
  focusMode?: boolean;
  musicPreferences?: {
    genre?: string;
    volume?: number;
    autoPlay?: boolean;
  };
  privacyLevel?: 'public' | 'friends' | 'private';
  workingHours?: {
    start?: string;
    end?: string;
    timezone?: string;
  };
}

export interface Persona {
  id: string;
  name: string;
  type: PersonaType;
  isActive: boolean;
  isDefault: boolean;
  settings: PersonaSettings;
  createdAt?: string;
  updatedAt?: string;
}

export interface PersonaCreateRequest {
  name: string;
  type: PersonaType;
  isDefault?: boolean;
  settings?: PersonaSettings;
}

export interface PersonaUpdateRequest {
  name?: string;
  type?: PersonaType;
  isDefault?: boolean;
  settings?: PersonaSettings;
}

export interface PersonaSwitchResponse {
  persona: Persona;
  token?: string;
}

export interface PersonaContextType {
  currentPersona: Persona | null;
  personas: Persona[];
  isLoading: boolean;
  error: string | null;
  switchPersona: (personaId: string) => Promise<void>;
  createPersona: (data: PersonaCreateRequest) => Promise<Persona>;
  updatePersona: (id: string, data: PersonaUpdateRequest) => Promise<Persona>;
  deletePersona: (id: string) => Promise<void>;
  fetchPersonas: () => Promise<void>;
}