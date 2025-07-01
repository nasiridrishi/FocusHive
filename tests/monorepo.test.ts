// Test to verify monorepo structure is set up correctly
import * as fs from 'fs';
import * as path from 'path';

describe('Monorepo Setup', () => {
  const rootDir = path.join(__dirname, '..');
  
  test('nx.json should exist', () => {
    const nxConfigPath = path.join(rootDir, 'nx.json');
    expect(fs.existsSync(nxConfigPath)).toBe(true);
  });

  test('workspace.json should exist', () => {
    const workspacePath = path.join(rootDir, 'workspace.json');
    expect(fs.existsSync(workspacePath)).toBe(true);
  });

  test('package.json should have Nx workspace configuration', () => {
    const packageJsonPath = path.join(rootDir, 'package.json');
    expect(fs.existsSync(packageJsonPath)).toBe(true);
    
    const packageJson = JSON.parse(fs.readFileSync(packageJsonPath, 'utf8'));
    expect(packageJson.name).toBe('@focushive/root');
    expect(packageJson.private).toBe(true);
    expect(packageJson.workspaces).toBeDefined();
  });

  test('Required packages directories should exist', () => {
    const packagesDir = path.join(rootDir, 'packages');
    expect(fs.existsSync(packagesDir)).toBe(true);
    
    // Check for expected package directories
    const expectedPackages = ['backend', 'frontend', 'shared'];
    expectedPackages.forEach(pkg => {
      const pkgPath = path.join(packagesDir, pkg);
      expect(fs.existsSync(pkgPath)).toBe(true);
    });
  });

  test('Apps and libs directories should exist', () => {
    const appsDir = path.join(rootDir, 'apps');
    const libsDir = path.join(rootDir, 'libs');
    
    expect(fs.existsSync(appsDir)).toBe(true);
    expect(fs.existsSync(libsDir)).toBe(true);
  });

  test('TypeScript configuration should be set up', () => {
    const tsconfigPath = path.join(rootDir, 'tsconfig.base.json');
    expect(fs.existsSync(tsconfigPath)).toBe(true);
    
    const tsconfig = JSON.parse(fs.readFileSync(tsconfigPath, 'utf8'));
    expect(tsconfig.compilerOptions).toBeDefined();
    expect(tsconfig.compilerOptions.paths).toBeDefined();
  });
});