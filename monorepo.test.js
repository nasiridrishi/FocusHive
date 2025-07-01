// Test to verify monorepo structure is set up correctly
const fs = require('fs');
const path = require('path');

describe('Monorepo Setup', () => {
  test('nx.json should exist', () => {
    const nxConfigPath = path.join(__dirname, 'nx.json');
    expect(fs.existsSync(nxConfigPath)).toBe(true);
  });

  test('workspace.json should exist', () => {
    const workspacePath = path.join(__dirname, 'workspace.json');
    expect(fs.existsSync(workspacePath)).toBe(true);
  });

  test('package.json should have Nx workspace configuration', () => {
    const packageJsonPath = path.join(__dirname, 'package.json');
    expect(fs.existsSync(packageJsonPath)).toBe(true);
    
    const packageJson = JSON.parse(fs.readFileSync(packageJsonPath, 'utf8'));
    expect(packageJson.name).toBe('@focushive/root');
    expect(packageJson.private).toBe(true);
    expect(packageJson.workspaces).toBeDefined();
  });

  test('Required packages directories should exist', () => {
    const packagesDir = path.join(__dirname, 'packages');
    expect(fs.existsSync(packagesDir)).toBe(true);
    
    // Check for expected package directories
    const expectedPackages = ['backend', 'frontend', 'shared'];
    expectedPackages.forEach(pkg => {
      const pkgPath = path.join(packagesDir, pkg);
      expect(fs.existsSync(pkgPath)).toBe(true);
    });
  });

  test('Apps and libs directories should exist', () => {
    const appsDir = path.join(__dirname, 'apps');
    const libsDir = path.join(__dirname, 'libs');
    
    expect(fs.existsSync(appsDir)).toBe(true);
    expect(fs.existsSync(libsDir)).toBe(true);
  });

  test('TypeScript configuration should be set up', () => {
    const tsconfigPath = path.join(__dirname, 'tsconfig.base.json');
    expect(fs.existsSync(tsconfigPath)).toBe(true);
    
    const tsconfig = JSON.parse(fs.readFileSync(tsconfigPath, 'utf8'));
    expect(tsconfig.compilerOptions).toBeDefined();
    expect(tsconfig.compilerOptions.paths).toBeDefined();
  });
});