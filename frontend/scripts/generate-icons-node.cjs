#!/usr/bin/env node

const fs = require('fs').promises;
const path = require('path');
const { exec } = require('child_process');
const { promisify } = require('util');

const execAsync = promisify(exec);

// Icon sizes to generate
const ICON_SIZES = [16, 32, 72, 96, 128, 144, 152, 192, 384, 512];

// Paths
const PROJECT_ROOT = path.join(__dirname, '..');
const LOGO_DIR = path.join(PROJECT_ROOT, 'logo');
const PUBLIC_LOGO_DIR = path.join(PROJECT_ROOT, 'public', 'logo');
const ICONS_DIR = path.join(PROJECT_ROOT, 'public', 'icons');

// Colors for output
const colors = {
  red: '\x1b[31m',
  green: '\x1b[32m',
  yellow: '\x1b[33m',
  reset: '\x1b[0m'
};

async function ensureDirectories() {
  await fs.mkdir(ICONS_DIR, { recursive: true });
  await fs.mkdir(PUBLIC_LOGO_DIR, { recursive: true });
}

async function checkDependencies() {
  try {
    // Check if sharp is installed
    require.resolve('sharp');
    return true;
  } catch (e) {
    console.log(`${colors.yellow}Sharp is not installed. Installing...${colors.reset}`);
    try {
      await execAsync('npm install sharp --save-dev');
      console.log(`${colors.green}✓ Sharp installed successfully${colors.reset}`);
      return true;
    } catch (installError) {
      console.error(`${colors.red}Failed to install sharp:${colors.reset}`, installError.message);
      console.log('Please run: npm install sharp --save-dev');
      return false;
    }
  }
}

async function generateIcons() {
  // Check and install dependencies
  if (!await checkDependencies()) {
    process.exit(1);
  }

  // Load sharp after ensuring it's installed
  const sharp = require('sharp');

  await ensureDirectories();

  // Source SVG file
  const sourceSvg = path.join(PUBLIC_LOGO_DIR, 'icon.svg');
  
  // Check if source SVG exists
  try {
    await fs.access(sourceSvg);
  } catch (e) {
    console.log(`${colors.yellow}Warning: icon.svg not found at ${sourceSvg}${colors.reset}`);
    console.log('Using logo.svg as fallback...');
    
    const logoSvg = path.join(PUBLIC_LOGO_DIR, 'logo.svg');
    try {
      await fs.access(logoSvg);
      await fs.copyFile(logoSvg, sourceSvg);
    } catch (e2) {
      console.error(`${colors.red}Error: No SVG files found in ${PUBLIC_LOGO_DIR}${colors.reset}`);
      process.exit(1);
    }
  }

  console.log(`${colors.green}Starting icon generation from SVG...${colors.reset}`);

  // Read SVG file
  const svgBuffer = await fs.readFile(sourceSvg);

  // Generate PNG icons in different sizes
  for (const size of ICON_SIZES) {
    const outputPath = path.join(ICONS_DIR, `icon-${size}x${size}.png`);
    
    try {
      await sharp(svgBuffer)
        .resize(size, size, {
          fit: 'contain',
          background: { r: 255, g: 255, b: 255, alpha: 0 } // transparent background
        })
        .png()
        .toFile(outputPath);
      
      console.log(`${colors.green}✓${colors.reset} Generated ${size}x${size} icon`);
    } catch (error) {
      console.error(`${colors.red}✗${colors.reset} Failed to generate ${size}x${size} icon:`, error.message);
    }
  }

  // Generate Apple Touch Icon (180x180)
  try {
    await sharp(svgBuffer)
      .resize(180, 180, {
        fit: 'contain',
        background: { r: 255, g: 255, b: 255, alpha: 0 }
      })
      .png()
      .toFile(path.join(ICONS_DIR, 'apple-touch-icon.png'));
    
    console.log(`${colors.green}✓${colors.reset} Generated Apple Touch Icon (180x180)`);
  } catch (error) {
    console.error(`${colors.red}✗${colors.reset} Failed to generate Apple Touch Icon:`, error.message);
  }

  // Generate main icon.png (512x512)
  try {
    await sharp(svgBuffer)
      .resize(512, 512, {
        fit: 'contain',
        background: { r: 255, g: 255, b: 255, alpha: 0 }
      })
      .png()
      .toFile(path.join(ICONS_DIR, 'icon.png'));
    
    console.log(`${colors.green}✓${colors.reset} Generated main icon.png (512x512)`);
  } catch (error) {
    console.error(`${colors.red}✗${colors.reset} Failed to generate main icon.png:`, error.message);
  }

  // Generate PNG versions for public/logo
  try {
    // Full logo PNG
    await sharp(svgBuffer)
      .resize(512, null, { // Width 512, maintain aspect ratio
        fit: 'inside',
        background: { r: 255, g: 255, b: 255, alpha: 0 }
      })
      .png()
      .toFile(path.join(PUBLIC_LOGO_DIR, 'logo.png'));
    
    console.log(`${colors.green}✓${colors.reset} Generated logo.png`);

    // Icon PNG (square)
    await sharp(svgBuffer)
      .resize(512, 512, {
        fit: 'contain',
        background: { r: 255, g: 255, b: 255, alpha: 0 }
      })
      .png()
      .toFile(path.join(PUBLIC_LOGO_DIR, 'icon.png'));
    
    console.log(`${colors.green}✓${colors.reset} Generated icon.png`);
  } catch (error) {
    console.error(`${colors.red}Error generating PNG versions:${colors.reset}`, error.message);
  }

  // Note about favicon.ico
  console.log(`\n${colors.yellow}Note: favicon.ico generation requires additional tools.${colors.reset}`);
  console.log('You can use online converters or install ImageMagick for favicon.ico generation.');

  console.log(`\n${colors.green}Icon generation complete!${colors.reset}`);
  console.log(`Generated icons in: ${ICONS_DIR}`);
  console.log(`Generated logos in: ${PUBLIC_LOGO_DIR}`);
}

// Run the script
generateIcons().catch(error => {
  console.error(`${colors.red}Script failed:${colors.reset}`, error);
  process.exit(1);
});