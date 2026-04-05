#!/usr/bin/env node
/**
 * nexus CLI launcher
 * Finds the Java runtime and executes the bundled nexus-autopilot jar.
 */
const { execFileSync, spawnSync } = require('child_process');
const path = require('path');
const fs   = require('fs');
const os   = require('os');

const JAR = path.join(__dirname, '..', 'target', 'nexus-autopilot-1.0-SNAPSHOT-jar-with-dependencies.jar');

// Verify Java is available
try {
  execFileSync('java', ['-version'], { stdio: 'ignore' });
} catch {
  console.error('\n  ✖ Java 17+ is required but was not found on your PATH.');
  console.error('  ↳ Install Java: https://adoptium.net/\n');
  process.exit(1);
}

// Build if jar doesn't exist
if (!fs.existsSync(JAR)) {
  console.log('\n  ℹ nexus-autopilot: building from source (first run)...');
  const build = spawnSync('mvn', ['package', '-q', '-f', path.join(__dirname, '..', 'pom.xml')], {
    stdio: 'inherit',
    shell: os.platform() === 'win32',
  });
  if (build.status !== 0) {
    console.error('\n  ✖ Build failed. Ensure Maven is installed: https://maven.apache.org\n');
    process.exit(1);
  }
}

// Forward all args to the jar
const args = process.argv.slice(2);
const result = spawnSync('java', ['-jar', JAR, ...args], {
  stdio: 'inherit',
  shell: false,
});
process.exit(result.status || 0);
