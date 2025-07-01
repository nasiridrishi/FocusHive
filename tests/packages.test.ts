// Test to verify package structures are set up correctly
import * as fs from 'fs';
import * as path from 'path';

describe('Package Structure', () => {
  const rootDir = path.join(__dirname, '..');
  
  describe('Backend Package', () => {
    const backendPath = path.join(rootDir, 'packages/backend');
    
    test('should have package.json', () => {
      const packageJsonPath = path.join(backendPath, 'package.json');
      expect(fs.existsSync(packageJsonPath)).toBe(true);
      
      const packageJson = JSON.parse(fs.readFileSync(packageJsonPath, 'utf8'));
      expect(packageJson.name).toBe('@focushive/backend');
      expect(packageJson.version).toBe('0.0.1');
    });
    
    test('should have src directory', () => {
      const srcPath = path.join(backendPath, 'src');
      expect(fs.existsSync(srcPath)).toBe(true);
    });
    
    test('should have tsconfig.json', () => {
      const tsconfigPath = path.join(backendPath, 'tsconfig.json');
      expect(fs.existsSync(tsconfigPath)).toBe(true);
    });
  });

  describe('Frontend Package', () => {
    const frontendPath = path.join(rootDir, 'packages/frontend');
    
    test('should have package.json', () => {
      const packageJsonPath = path.join(frontendPath, 'package.json');
      expect(fs.existsSync(packageJsonPath)).toBe(true);
      
      const packageJson = JSON.parse(fs.readFileSync(packageJsonPath, 'utf8'));
      expect(packageJson.name).toBe('@focushive/frontend');
      expect(packageJson.version).toBe('0.0.1');
    });
    
    test('should have src directory', () => {
      const srcPath = path.join(frontendPath, 'src');
      expect(fs.existsSync(srcPath)).toBe(true);
    });
    
    test('should have tsconfig.json', () => {
      const tsconfigPath = path.join(frontendPath, 'tsconfig.json');
      expect(fs.existsSync(tsconfigPath)).toBe(true);
    });
  });

  describe('Shared Package', () => {
    const sharedPath = path.join(rootDir, 'packages/shared');
    
    test('should have package.json', () => {
      const packageJsonPath = path.join(sharedPath, 'package.json');
      expect(fs.existsSync(packageJsonPath)).toBe(true);
      
      const packageJson = JSON.parse(fs.readFileSync(packageJsonPath, 'utf8'));
      expect(packageJson.name).toBe('@focushive/shared');
      expect(packageJson.version).toBe('0.0.1');
    });
    
    test('should have src directory with index.ts', () => {
      const srcPath = path.join(sharedPath, 'src');
      const indexPath = path.join(srcPath, 'index.ts');
      expect(fs.existsSync(srcPath)).toBe(true);
      expect(fs.existsSync(indexPath)).toBe(true);
    });
    
    test('should have tsconfig.json', () => {
      const tsconfigPath = path.join(sharedPath, 'tsconfig.json');
      expect(fs.existsSync(tsconfigPath)).toBe(true);
    });
  });

});