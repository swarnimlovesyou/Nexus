/**
 * postinstall.js
 * Runs automatically after npm install -g nexus-autopilot.
 * Triggers a Maven build if the jar is not already present.
 */
const { spawnSync } = require('child_process');
const path = require('fs');
const fs   = require('fs');
const os   = require('os');

const jarPath = require('path').join(__dirname, '..', 'target', 'nexus-autopilot-1.0-SNAPSHOT-jar-with-dependencies.jar');

if (fs.existsSync(jarPath)) {
  console.log('  ✔ nexus-autopilot: jar already built, skipping build step.');
  process.exit(0);
}

console.log('\n  ℹ nexus-autopilot: building Java source (this runs once)...');
const result = spawnSync('mvn', ['package', '-q', '-f', require('path').join(__dirname, '..', 'pom.xml')], {
  stdio: 'inherit',
  shell: os.platform() === 'win32',
});

if (result.status !== 0) {
  console.warn('\n  ⚠ Build step failed. Run `mvn package -q` manually before using nexus.');
} else {
  console.log('  ✔ nexus-autopilot: build complete. Run: nexus start\n');
}
