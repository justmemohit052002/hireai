// Node.js verification script for HireAI Admin API
import http from 'http';

async function request(url, options = {}) {
  return new Promise((resolve, reject) => {
    const parsed = new URL(url);
    const req = http.request(
      {
        hostname: parsed.hostname,
        port: parsed.port,
        path: parsed.pathname + parsed.search,
        method: options.method || 'GET',
        headers: options.headers || {},
      },
      (res) => {
        let data = '';
        res.on('data', (chunk) => (data += chunk));
        res.on('end', () => {
          try {
            const parsedData = JSON.parse(data);
            resolve({ status: res.statusCode, data: parsedData });
          } catch {
            resolve({ status: res.statusCode, raw: data });
          }
        });
      }
    );
    req.on('error', reject);
    if (options.body) {
      req.write(typeof options.body === 'string' ? options.body : JSON.stringify(options.body));
    }
    req.end();
  });
}

async function run() {
  console.log('--- Step 1: Login as Admin ---');
  let loginRes = await request('http://localhost:8080/auth/login', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: {
      email: 'admin@vionsys.com',
      password: 'Password@123',
      role: 'ADMIN',
    },
  });

  if (loginRes.status !== 200) {
    console.log('Admin user not found yet or login failed. Attempting recruiter login or check status:', loginRes);
    // Let's test with Jordan Kim
    loginRes = await request('http://localhost:8080/auth/login', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: {
        email: 'jordan.kim@vionsys.com',
        password: 'Password@123',
        role: 'RECRUITER',
      },
    });
    console.log('Jordan Kim Recruiter Login Status:', loginRes.status, 'User Role:', loginRes.data?.role);
  } else {
    console.log('Admin Login SUCCESS! User:', loginRes.data.email, 'Role:', loginRes.data.role);
    const token = loginRes.data.accessToken;

    console.log('\n--- Step 2: Testing /admin/health ---');
    const healthRes = await request('http://localhost:8080/admin/health', {
      headers: { Authorization: `Bearer ${token}` },
    });
    console.log('Health Status Code:', healthRes.status);
    console.log('Health Data:', JSON.stringify(healthRes.data?.data || healthRes.data, null, 2));

    console.log('\n--- Step 3: Testing /admin/stats ---');
    const statsRes = await request('http://localhost:8080/admin/stats', {
      headers: { Authorization: `Bearer ${token}` },
    });
    console.log('Stats Status Code:', statsRes.status);
    console.log('Stats Data:', JSON.stringify(statsRes.data?.data || statsRes.data, null, 2));

    console.log('\n--- Step 4: Testing /admin/logs ---');
    const logsRes = await request('http://localhost:8080/admin/logs?page=1&size=3', {
      headers: { Authorization: `Bearer ${token}` },
    });
    console.log('Logs Status Code:', logsRes.status);
    console.log('Logs Count:', (logsRes.data?.data?.items || []).length);

    console.log('\n--- Step 5: Testing /admin/users ---');
    const usersRes = await request('http://localhost:8080/admin/users?page=0&size=5', {
      headers: { Authorization: `Bearer ${token}` },
    });
    console.log('Users Status Code:', usersRes.status);
    console.log('Users List:', JSON.stringify(usersRes.data?.data?.content || [], null, 2));

    const candidateUser = (usersRes.data?.data?.content || []).find((u) => u.role === 'ROLE_CANDIDATE');
    if (candidateUser) {
      console.log('\n--- Step 6: Testing Update User Role for', candidateUser.email, '---');
      const roleRes = await request(`http://localhost:8080/admin/users/${candidateUser.id}/role`, {
        method: 'PUT',
        headers: {
          'Content-Type': 'application/json',
          Authorization: `Bearer ${token}`,
        },
        body: { role: 'ROLE_RECRUITER' },
      });
      console.log('Role Update Status:', roleRes.status, 'New Role:', roleRes.data?.data?.role);

      console.log('\n--- Step 7: Testing Soft-Deactivate User for', candidateUser.email, '---');
      const statusRes = await request(`http://localhost:8080/admin/users/${candidateUser.id}/status`, {
        method: 'PUT',
        headers: {
          'Content-Type': 'application/json',
          Authorization: `Bearer ${token}`,
        },
        body: { enabled: false },
      });
      console.log('Status Update Status:', statusRes.status, 'Enabled:', statusRes.data?.data?.enabled);

      console.log('\n--- Step 8: Restoring User Role and Active Status ---');
      await request(`http://localhost:8080/admin/users/${candidateUser.id}/role`, {
        method: 'PUT',
        headers: {
          'Content-Type': 'application/json',
          Authorization: `Bearer ${token}`,
        },
        body: { role: 'ROLE_CANDIDATE' },
      });
      await request(`http://localhost:8080/admin/users/${candidateUser.id}/status`, {
        method: 'PUT',
        headers: {
          'Content-Type': 'application/json',
          Authorization: `Bearer ${token}`,
        },
        body: { enabled: true },
      });
      console.log('Restored candidate role and enabled status cleanly!');
    }
  }
}

run().catch(console.error);
