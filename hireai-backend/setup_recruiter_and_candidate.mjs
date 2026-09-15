import fs from 'fs';

const BASE_URL = 'http://localhost:8080';

async function request(method, endpoint, body = null, token = null) {
  const url = `${BASE_URL}${endpoint.startsWith('/') ? endpoint : '/' + endpoint}`;
  const headers = {
    'Accept': 'application/json',
  };
  if (body !== null) {
    headers['Content-Type'] = 'application/json';
  }
  if (token) {
    headers['Authorization'] = token.startsWith('Bearer ') ? token : `Bearer ${token}`;
  }

  const options = {
    method: method.toUpperCase(),
    headers,
  };
  if (body !== null) {
    options.body = typeof body === 'string' ? body : JSON.stringify(body);
  }

  try {
    const res = await fetch(url, options);
    let data = null;
    const text = await res.text();
    try {
      data = JSON.parse(text);
    } catch {
      data = text;
    }
    return {
      status: res.status,
      ok: res.ok,
      data
    };
  } catch (err) {
    return {
      status: 0,
      ok: false,
      error: err.message,
      data: null
    };
  }
}

async function main() {
  console.log('=== SETTING UP RECRUITER AND CANDIDATE ===\n');

  // 1. Setup Recruiter Neha Sharma (or Neha Mishra)
  const recruiterEmails = ['neha.sharma@vionsys.com', 'neha.mishra@vionsys.com'];
  let recruiterToken = null;
  let recruiterUserId = null;
  let activeRecruiterEmail = '';

  for (const email of recruiterEmails) {
    console.log(`Checking/Registering Recruiter: ${email}...`);
    let regRes = await request('POST', '/auth/register/recruiter', {
      firstName: 'Neha',
      lastName: email.includes('sharma') ? 'Sharma' : 'Mishra',
      email: email,
      password: 'Password@123',
      companyName: 'Vionsys Technologies'
    });

    if (regRes.status === 201) {
      console.log(`-> Recruiter registered successfully: ${email}`);
      recruiterToken = regRes.data.accessToken;
      recruiterUserId = regRes.data.userId;
      activeRecruiterEmail = email;
      break;
    } else if (regRes.status === 409 || (regRes.data && regRes.data.message && regRes.data.message.includes('already exists'))) {
      console.log(`-> Recruiter already registered. Logging in as ${email}...`);
      let loginRes = await request('POST', '/auth/login', {
        email: email,
        password: 'Password@123'
      });
      if (loginRes.status === 200) {
        console.log(`-> Logged in successfully: ${email}`);
        recruiterToken = loginRes.data.accessToken;
        recruiterUserId = loginRes.data.userId;
        activeRecruiterEmail = email;
        break;
      }
    }
  }

  if (!recruiterToken) {
    console.error('Failed to authenticate recruiter.');
    return;
  }

  // 2. Create/Update Recruiter Profile
  console.log('\nSetting up Recruiter Company Profile...');
  const profRes = await request('POST', '/recruiter/profile', {
    companyName: 'Vionsys Technologies',
    designation: 'Lead Technical Recruiter',
    companyWebsite: 'https://vionsys.com',
    companyEmail: activeRecruiterEmail,
    companyPhone: '9876543210',
    companyDescription: 'Innovative IT Solutions & Next-Gen AI Engineering',
    industry: 'Information Technology',
    companySize: 150,
    country: 'India',
    state: 'Maharashtra',
    city: 'Pune',
    address: 'Vionsys Towers, Baner, Pune'
  }, recruiterToken);

  if (profRes.status === 200) {
    console.log('-> Recruiter Profile saved successfully!');
  } else {
    console.log(`-> Recruiter Profile response (${profRes.status}):`, profRes.data);
  }

  // 3. Post a Job by Recruiter
  console.log('\nPosting Job by Recruiter...');
  const jobRes = await request('POST', '/jobs', {
    title: 'Senior Java Backend Engineer',
    description: 'Looking for an experienced Java / Spring Boot developer to build scalable cloud microservices, PostgreSQL architectures, and REST APIs.',
    employmentType: 'FULL_TIME',
    experienceLevel: 'SENIOR',
    location: 'Pune, Maharashtra',
    remote: true,
    salaryMin: 1400000,
    salaryMax: 2400000,
    currency: 'INR',
    skills: ['Java', 'Spring Boot', 'PostgreSQL', 'Microservices', 'Docker'],
    education: 'B.Tech / MCA / BE in Computer Science',
    openings: 3,
    applicationDeadline: '2026-12-31'
  }, recruiterToken);

  let createdJob = null;
  if (jobRes.status === 201) {
    createdJob = jobRes.data;
    console.log(`-> Job Created! ID: ${createdJob.id}, Title: "${createdJob.title}"`);
  } else {
    console.log(`-> Job creation returned ${jobRes.status}:`, jobRes.data);
    // Try to get my jobs
    const myJobsRes = await request('GET', '/jobs', null, recruiterToken);
    if (myJobsRes.status === 200 && myJobsRes.data.length > 0) {
      createdJob = myJobsRes.data[0];
      console.log(`-> Using existing job ID: ${createdJob.id}, Title: "${createdJob.title}"`);
    }
  }

  // 4. Setup Candidate Raj Patidar
  const candidateEmail = 'raj.patidar@vionsys.com';
  let candidateToken = null;
  let candidateUserId = null;

  console.log(`\nChecking/Registering Candidate: ${candidateEmail}...`);
  let candReg = await request('POST', '/auth/register/candidate', {
    firstName: 'Raj',
    lastName: 'Patidar',
    email: candidateEmail,
    password: 'Password@123',
    phoneNumber: '9876543299'
  });

  if (candReg.status === 201) {
    console.log('-> Candidate registered successfully!');
    candidateToken = candReg.data.accessToken;
    candidateUserId = candReg.data.userId;
  } else {
    console.log('-> Candidate already exists. Logging in...');
    let candLogin = await request('POST', '/auth/login', {
      email: candidateEmail,
      password: 'Password@123'
    });
    if (candLogin.status === 200) {
      console.log('-> Candidate logged in successfully!');
      candidateToken = candLogin.data.accessToken;
      candidateUserId = candLogin.data.userId;
    }
  }

  // 5. Create Candidate Profile
  console.log('\nCreating Candidate Profile for Raj Patidar...');
  let candProfRes = await request('POST', '/candidate/profile', {
    linkedinUrl: 'https://linkedin.com/in/rajpatidar',
    githubUrl: 'https://github.com/rajpatidar',
    portfolioUrl: 'https://rajpatidar.dev',
    currentCompany: 'Tech Innovators Pvt Ltd',
    currentDesignation: 'Java Backend Developer',
    experience: 5.0,
    currentCtc: 1200000,
    expectedCtc: 1800000,
    noticePeriod: 30,
    location: 'Pune, Maharashtra',
    skillIds: []
  }, candidateToken);

  if (candProfRes.status === 201) {
    console.log('-> Candidate Profile created successfully!');
  } else if (candProfRes.status === 409) {
    console.log('-> Candidate Profile already exists.');
  } else {
    console.log(`-> Candidate profile response (${candProfRes.status}):`, candProfRes.data);
  }

  console.log('\n================ SUMMARY ================');
  console.log(`Recruiter Email: ${activeRecruiterEmail}`);
  console.log(`Recruiter Token: ${recruiterToken}`);
  console.log(`Recruiter Job ID: ${createdJob ? createdJob.id : 'N/A'}`);
  console.log(`Candidate Email: ${candidateEmail}`);
  console.log(`Candidate Token: ${candidateToken}`);
  console.log('=========================================\n');
}

main().catch(console.error);
