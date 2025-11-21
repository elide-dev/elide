// Quick test of the race API endpoints

const BASE_URL = 'http://localhost:3001';

async function testRaceAPI() {
  console.log('🧪 Testing Race API endpoints...\n');

  // Test 1: Check if a race exists for a repo that doesn't exist yet
  console.log('1️⃣  Testing /api/races/check (non-existent repo)...');
  try {
    const response = await fetch(`${BASE_URL}/api/races/check?repo=https://github.com/google/gson`);
    const data = await response.json();
    console.log('   ✅ Response:', data);
    console.log('   Expected: { exists: false }\n');
  } catch (error) {
    console.error('   ❌ Error:', error.message, '\n');
  }

  // Test 2: Check health endpoint
  console.log('2️⃣  Testing /health...');
  try {
    const response = await fetch(`${BASE_URL}/health`);
    const data = await response.json();
    console.log('   ✅ Response:', data);
    console.log('   Expected: { status: "ok", timestamp: "..." }\n');
  } catch (error) {
    console.error('   ❌ Error:', error.message, '\n');
  }

  // Test 3: Try to start a race (won't actually run since Docker containers need to be ready)
  console.log('3️⃣  Testing /api/races/start (dry run - will fail without Docker images)...');
  try {
    const response = await fetch(`${BASE_URL}/api/races/start`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ repositoryUrl: 'https://github.com/google/gson' })
    });
    const data = await response.json();

    if (response.ok) {
      console.log('   ✅ Response:', data);
      console.log('   Race started successfully!\n');
    } else {
      console.log('   ⚠️  Expected failure (Docker images not ready):', data, '\n');
    }
  } catch (error) {
    console.error('   ❌ Error:', error.message, '\n');
  }

  // Test 4: Check stats endpoint
  console.log('4️⃣  Testing /api/races/stats...');
  try {
    const response = await fetch(`${BASE_URL}/api/races/stats`);
    const data = await response.json();
    console.log('   ✅ Response:', data);
    console.log('   Expected: { statistics: [] } (empty initially)\n');
  } catch (error) {
    console.error('   ❌ Error:', error.message, '\n');
  }

  console.log('✅ API endpoint tests complete!');
}

testRaceAPI().catch(console.error);
