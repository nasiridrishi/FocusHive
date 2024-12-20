#!/usr/bin/env node

const fs = require('fs');
const path = require('path');
const { exec } = require('child_process');
const { promisify } = require('util');

const execAsync = promisify(exec);

const colors = {
  red: '\x1b[31m',
  green: '\x1b[32m',
  yellow: '\x1b[33m',
  reset: '\x1b[0m'
};

async function createFavicon() {
  const ICONS_DIR = path.join(__dirname, '..', 'public', 'icons');
  
  // Check if png2ico is installed globally
  try {
    await execAsync('which png2ico');
  } catch (e) {
    console.log(`${colors.yellow}png2ico is not installed. Installing globally...${colors.reset}`);
    try {
      await execAsync('npm install -g png2ico');
      console.log(`${colors.green}✓ png2ico installed${colors.reset}`);
    } catch (installError) {
      console.log(`${colors.yellow}Could not install png2ico globally. Trying locally...${colors.reset}`);
      try {
        await execAsync('npm install png2ico');
        console.log(`${colors.green}✓ png2ico installed locally${colors.reset}`);
      } catch (localError) {
        console.error(`${colors.red}Failed to install png2ico${colors.reset}`);
        console.log('Please install manually: npm install -g png2ico');
        console.log('Or use an online favicon generator.');
        return;
      }
    }
  }

  // Create favicon.ico
  const icon16 = path.join(ICONS_DIR, 'icon-16x16.png');
  const icon32 = path.join(ICONS_DIR, 'icon-32x32.png');
  const faviconPath = path.join(ICONS_DIR, 'favicon.ico');

  try {
    // Try global png2ico first
    await execAsync(`png2ico ${faviconPath} ${icon16} ${icon32}`);
    console.log(`${colors.green}✓ favicon.ico created successfully${colors.reset}`);
  } catch (e) {
    // Try local png2ico
    try {
      await execAsync(`npx png2ico ${faviconPath} ${icon16} ${icon32}`);
      console.log(`${colors.green}✓ favicon.ico created successfully${colors.reset}`);
    } catch (e2) {
      // Fallback: just copy the 32x32 as favicon.ico (not ideal but works)
      console.log(`${colors.yellow}png2ico failed. Creating simple favicon...${colors.reset}`);
      fs.copyFileSync(icon32, faviconPath);
      console.log(`${colors.green}✓ Simple favicon.ico created (copy of 32x32 PNG)${colors.reset}`);
    }
  }

  console.log(`\n${colors.green}Favicon creation complete!${colors.reset}`);
}

createFavicon().catch(error => {
  console.error(`${colors.red}Script failed:${colors.reset}`, error);
});