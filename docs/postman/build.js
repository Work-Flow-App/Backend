#!/usr/bin/env node

/**
 * Postman Collection Builder
 *
 * This script merges modular Postman collection files into a single collection.
 *
 * Usage: node build.js
 * Or via npm: npm run postman:build
 */

const fs = require('fs');
const path = require('path');

// Paths
const MODULES_DIR = path.join(__dirname, 'modules');
const BASE_FILE = path.join(__dirname, 'base.json');
const OUTPUT_FILE = path.join(__dirname, '..', 'Work-Flow-App-API.postman_collection.json');

// Module order (determines folder order in Postman)
const MODULE_ORDER = [
  'auth',
  'company',
  'workers',
  'clients',
  'job-templates',
  'jobs',
  'assets',
  'asset-assignments',
  'workflows',
  'job-workflows',
  'job-workflow-step-activities'
];

/**
 * Load a JSON file
 */
function loadJson(filePath) {
  try {
    const content = fs.readFileSync(filePath, 'utf-8');
    return JSON.parse(content);
  } catch (error) {
    console.error(`❌ Error loading ${filePath}:`, error.message);
    process.exit(1);
  }
}

/**
 * Save JSON to file
 */
function saveJson(filePath, data) {
  try {
    const json = JSON.stringify(data, null, 2);
    fs.writeFileSync(filePath, json, 'utf-8');
    console.log(`✅ Saved: ${path.relative(process.cwd(), filePath)}`);
  } catch (error) {
    console.error(`❌ Error saving ${filePath}:`, error.message);
    process.exit(1);
  }
}

/**
 * Build the complete collection
 */
function buildCollection() {
  console.log('🔨 Building Postman collection...\n');

  // Load base collection
  const collection = loadJson(BASE_FILE);
  console.log(`📖 Loaded base collection: ${collection.info.name}`);

  // Initialize items array
  collection.item = [];

  // Load and merge modules in order
  let loadedCount = 0;
  let skippedCount = 0;

  for (const moduleName of MODULE_ORDER) {
    const moduleFile = path.join(MODULES_DIR, `${moduleName}.json`);

    if (!fs.existsSync(moduleFile)) {
      console.log(`⚠️  Skipped: ${moduleName}.json (file not found)`);
      skippedCount++;
      continue;
    }

    const module = loadJson(moduleFile);

    // Validate module structure
    if (!module.name || !Array.isArray(module.item)) {
      console.error(`❌ Invalid module structure in ${moduleName}.json`);
      console.error('   Expected: { "name": "...", "item": [...] }');
      process.exit(1);
    }

    collection.item.push(module);
    console.log(`✅ Loaded: ${moduleName}.json (${module.item.length} endpoints)`);
    loadedCount++;
  }

  // Check for extra modules not in MODULE_ORDER
  const allFiles = fs.readdirSync(MODULES_DIR).filter(f => f.endsWith('.json'));
  const extraModules = allFiles.filter(f => !MODULE_ORDER.includes(f.replace('.json', '')));

  if (extraModules.length > 0) {
    console.log(`\n⚠️  Warning: Found modules not in MODULE_ORDER:`);
    extraModules.forEach(m => console.log(`   - ${m}`));
    console.log('   Add them to MODULE_ORDER in build.js to include them.\n');
  }

  // Save merged collection
  saveJson(OUTPUT_FILE, collection);

  // Summary
  console.log(`\n📊 Summary:`);
  console.log(`   Modules loaded: ${loadedCount}`);
  console.log(`   Modules skipped: ${skippedCount}`);
  console.log(`   Total endpoints: ${collection.item.reduce((sum, module) => sum + module.item.length, 0)}`);
  console.log(`\n✨ Collection built successfully!`);
  console.log(`   Import to Postman: ${path.relative(process.cwd(), OUTPUT_FILE)}\n`);
}

// Run the build
try {
  buildCollection();
} catch (error) {
  console.error('\n❌ Build failed:', error.message);
  console.error(error.stack);
  process.exit(1);
}