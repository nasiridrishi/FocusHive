// Components
export { PersonaSwitcher, PersonaManagement } from './components';

// Pages
export { PersonaSettingsPage, PersonaManagementPage } from './pages';

// Types
export type {
  PersonaType,
  PersonaSettings,
  Persona,
  PersonaCreateRequest,
  PersonaUpdateRequest
} from './types';

// Services
export {
  getPersonas,
  getCurrentPersona,
  switchPersona,
  createPersona,
  updatePersona,
  deletePersona
} from './services/identityService';