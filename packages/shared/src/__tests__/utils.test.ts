import { formatDate, isValidEmail } from '../utils';

describe('Utils', () => {
  describe('formatDate', () => {
    it('should format date correctly', () => {
      const date = new Date('2025-01-01T12:00:00Z');
      expect(formatDate(date)).toBe('2025-01-01');
    });

    it('should handle invalid dates', () => {
      expect(formatDate(new Date('invalid'))).toBe('Invalid Date');
    });
  });

  describe('isValidEmail', () => {
    it('should validate correct email addresses', () => {
      expect(isValidEmail('test@example.com')).toBe(true);
      expect(isValidEmail('user.name@domain.co.uk')).toBe(true);
    });

    it('should reject invalid email addresses', () => {
      expect(isValidEmail('invalid')).toBe(false);
      expect(isValidEmail('@example.com')).toBe(false);
      expect(isValidEmail('test@')).toBe(false);
    });
  });
});