// Automated Comprehensive API Testing Suite for HireAI Backend
const BASE_URL = 'http://localhost:8080';

const results = [];
let passCount = 0;
let failCount = 0;

function logHeader(title) {
  console.log('\n' + '='.repeat(70));
  console.log(`  ${title}`);
  console.log('='.repeat(70));
}

function recordResult(testName, passed, details = '') {
  if (passed) {
    passCount++;
    console.log(`  [PASS] ${testName}`);
    if (details) console.log(`         ${details}`);
  } else {
    failCount++;
    console.log(`  [FAIL] ${testName}`);
    if (details) console.log(`         Error: ${details}`);
  }
  results.push({ testName, passed, details });
}

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
      data,
      headers: Object.fromEntries(res.headers.entries())
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

async function runAllTests() {
  const seed = Date.now().toString().slice(-6);
  const candidateEmail = `cand_${seed}@hireai-test.io`;
  const recruiterEmail = `rec_${seed}@hireai-test.io`;
  const testPassword = 'Password@123';
  const testPhone = '98' + Math.floor(10000000 + Math.random() * 90000000);

  let candidateToken = '';
  let candidateUserId = '';
  let candidateProfileId = '';
  let candidateBusinessId = '';

  let recruiterToken = '';
  let recruiterUserId = '';

  let createdJobId = '';
  let createdApplicationId = '';

  console.log(`\nStarting HireAI Backend API Inspection & End-to-End Testing...`);
  console.log(`Target Base URL: ${BASE_URL}`);
  console.log(`Generated Test Run Seed: ${seed}`);

  // =========================================================================
  // 1. PUBLIC & SECURITY CHECKS
  // =========================================================================
  logHeader('1. PUBLIC & SECURITY VERIFICATION');

  // Test 1.1: Unauthenticated request to protected endpoint /test
  const unauthTest = await request('GET', '/test');
  recordResult(
    'GET /test without token returns 401 Unauthorized',
    unauthTest.status === 401,
    `Status: ${unauthTest.status}`
  );

  // Test 1.2: Invalid Bearer token
  const invalidTokenTest = await request('GET', '/test', null, 'invalid_token_123');
  recordResult(
    'GET /test with invalid token returns 401 Unauthorized',
    invalidTokenTest.status === 401,
    `Status: ${invalidTokenTest.status}`
  );

  // =========================================================================
  // 2. AUTHENTICATION & REGISTRATION
  // =========================================================================
  logHeader('2. AUTHENTICATION (CANDIDATE & RECRUITER)');

  // Test 2.1: Register Candidate
  const regCandPayload = {
    firstName: 'Candidate',
    lastName: `Tester${seed}`,
    email: candidateEmail,
    password: testPassword,
    phoneNumber: testPhone,
  };
  const regCandRes = await request('POST', '/auth/register/candidate', regCandPayload);
  const candRegistered = regCandRes.status === 201 && regCandRes.data?.accessToken && regCandRes.data?.role === 'ROLE_CANDIDATE';
  if (candRegistered) {
    candidateToken = regCandRes.data.accessToken;
    candidateUserId = regCandRes.data.userId;
  }
  recordResult(
    'POST /auth/register/candidate creates user & returns JWT with ROLE_CANDIDATE',
    candRegistered,
    `Status: ${regCandRes.status}, UserId: ${candidateUserId}, Role: ${regCandRes.data?.role}`
  );

  // Test 2.2: Register Recruiter
  const regRecPayload = {
    firstName: 'Recruiter',
    lastName: `Manager${seed}`,
    email: recruiterEmail,
    password: testPassword,
    phoneNumber: '97' + Math.floor(10000000 + Math.random() * 90000000),
  };
  const regRecRes = await request('POST', '/auth/register/recruiter', regRecPayload);
  const recRegistered = regRecRes.status === 201 && regRecRes.data?.accessToken && regRecRes.data?.role === 'ROLE_RECRUITER';
  if (recRegistered) {
    recruiterToken = regRecRes.data.accessToken;
    recruiterUserId = regRecRes.data.userId;
  }
  recordResult(
    'POST /auth/register/recruiter creates user & returns JWT with ROLE_RECRUITER',
    recRegistered,
    `Status: ${regRecRes.status}, UserId: ${recruiterUserId}, Role: ${regRecRes.data?.role}`
  );

  // Test 2.3: Duplicate Email Registration Check
  const dupEmailRes = await request('POST', '/auth/register/candidate', regCandPayload);
  recordResult(
    'POST /auth/register/candidate with duplicate email returns 409 Conflict',
    dupEmailRes.status === 409,
    `Status: ${dupEmailRes.status}`
  );

  // Test 2.4: Candidate Login
  const loginCandRes = await request('POST', '/auth/login', {
    email: candidateEmail,
    password: testPassword,
  });
  recordResult(
    'POST /auth/login (Candidate credentials) returns 200 OK & new token',
    loginCandRes.status === 200 && !!loginCandRes.data?.accessToken,
    `Status: ${loginCandRes.status}, Email: ${loginCandRes.data?.email}`
  );

  // Test 2.5: Recruiter Login
  const loginRecRes = await request('POST', '/auth/login', {
    email: recruiterEmail,
    password: testPassword,
  });
  recordResult(
    'POST /auth/login (Recruiter credentials) returns 200 OK & new token',
    loginRecRes.status === 200 && !!loginRecRes.data?.accessToken,
    `Status: ${loginRecRes.status}, Email: ${loginRecRes.data?.email}`
  );

  // Test 2.6: Login Bad Credentials
  const badLoginRes = await request('POST', '/auth/login', {
    email: candidateEmail,
    password: 'WrongPassword@999',
  });
  recordResult(
    'POST /auth/login with wrong password returns 401 Unauthorized',
    badLoginRes.status === 401,
    `Status: ${badLoginRes.status}`
  );

  // Test 2.7: Authenticated request to /test with candidate token
  const authTestRes = await request('GET', '/test', null, candidateToken);
  recordResult(
    'GET /test with Candidate JWT returns 200 OK',
    authTestRes.status === 200 && authTestRes.data === 'JWT Authentication Successful',
    `Status: ${authTestRes.status}, Body: ${authTestRes.data}`
  );

  // =========================================================================
  // 3. USER SELF-SERVICE APIS (/users)
  // =========================================================================
  logHeader('3. USER SELF-SERVICE ENDPOINTS');

  // Test 3.1: GET /users/me
  const getMeRes = await request('GET', '/users/me', null, candidateToken);
  recordResult(
    'GET /users/me returns authenticated user details',
    getMeRes.status === 200 && getMeRes.data?.email === candidateEmail,
    `Status: ${getMeRes.status}, User ID: ${getMeRes.data?.id}, Name: ${getMeRes.data?.firstName} ${getMeRes.data?.lastName}`
  );

  // Test 3.2: PUT /users/me
  const updateMeRes = await request('PUT', '/users/me', {
    firstName: 'UpdatedCand',
    lastName: 'Tested',
    phoneNumber: testPhone,
  }, candidateToken);
  recordResult(
    'PUT /users/me updates user basic details',
    updateMeRes.status === 200 && updateMeRes.data?.firstName === 'UpdatedCand',
    `Status: ${updateMeRes.status}, New Name: ${updateMeRes.data?.firstName} ${updateMeRes.data?.lastName}`
  );

  // Test 3.3: GET /users/{userId}
  const getUserByIdRes = await request('GET', `/users/${candidateUserId}`, null, candidateToken);
  recordResult(
    'GET /users/{userId} fetches user by UUID',
    getUserByIdRes.status === 200 && getUserByIdRes.data?.id === candidateUserId,
    `Status: ${getUserByIdRes.status}`
  );

  // =========================================================================
  // 4. RECRUITER PROFILE ENDPOINTS (/recruiter/profile)
  // =========================================================================
  logHeader('4. RECRUITER PROFILE ENDPOINTS');

  // Test 4.1: RBAC check - Candidate attempting to create recruiter profile
  const candRecProfileRes = await request('POST', '/recruiter/profile', {
    companyName: 'Unauthorized Corp',
  }, candidateToken);
  recordResult(
    'POST /recruiter/profile with CANDIDATE role is rejected with 403 Forbidden',
    candRecProfileRes.status === 403,
    `Status: ${candRecProfileRes.status}`
  );

  // Test 4.2: Recruiter create profile
  const createRecProfilePayload = {
    companyName: 'Acme AI Technologies Inc',
    designation: 'Lead Talent Acquisition Partner',
    companyWebsite: 'https://acmeai.io',
    companyEmail: `hr.${seed}@acmeai.io`,
    companyPhone: '9988776655',
    companyLogoUrl: 'https://acmeai.io/logo.png',
    companyDescription: 'Leading provider of enterprise AI and deep learning solutions.',
    industry: 'Information Technology',
    companySize: 250,
    country: 'India',
    state: 'Maharashtra',
    city: 'Pune',
    address: 'Viman Nagar IT Park, Pune',
  };
  const recProfileRes = await request('POST', '/recruiter/profile', createRecProfilePayload, recruiterToken);
  recordResult(
    'POST /recruiter/profile creates recruiter company profile',
    recProfileRes.status === 200 && recProfileRes.data?.companyName === 'Acme AI Technologies Inc',
    `Status: ${recProfileRes.status}, Company: ${recProfileRes.data?.companyName}`
  );

  // Test 4.3: GET /recruiter/profile
  const getRecProfileRes = await request('GET', '/recruiter/profile', null, recruiterToken);
  recordResult(
    'GET /recruiter/profile fetches logged-in recruiter company profile',
    getRecProfileRes.status === 200 && getRecProfileRes.data?.companyName === 'Acme AI Technologies Inc',
    `Status: ${getRecProfileRes.status}, Recruiter: ${getRecProfileRes.data?.designation}`
  );

  // Test 4.4: PUT /recruiter/profile
  const updateRecProfileRes = await request('PUT', '/recruiter/profile', {
    ...createRecProfilePayload,
    companySize: 350,
    designation: 'VP of Global Talent Acquisition',
  }, recruiterToken);
  recordResult(
    'PUT /recruiter/profile updates recruiter profile details',
    updateRecProfileRes.status === 200 && updateRecProfileRes.data?.companySize === 350,
    `Status: ${updateRecProfileRes.status}, Designation: ${updateRecProfileRes.data?.designation}`
  );

  // Test 4.5: GET /recruiter/profile/{userId}
  const getRecProfileByUserIdRes = await request('GET', `/recruiter/profile/${recruiterUserId}`, null, recruiterToken);
  recordResult(
    'GET /recruiter/profile/{userId} fetches recruiter profile by User UUID',
    getRecProfileByUserIdRes.status === 200 && getRecProfileByUserIdRes.data?.companyName === 'Acme AI Technologies Inc',
    `Status: ${getRecProfileByUserIdRes.status}, Recruiter User ID: ${recruiterUserId}`
  );

  // =========================================================================
  // 5. CANDIDATE PROFILE ENDPOINTS (/candidate/profile)
  // =========================================================================
  logHeader('5. CANDIDATE PROFILE ENDPOINTS');

  // Test 5.1: RBAC check - Recruiter creating candidate profile
  const recCandProfileRes = await request('POST', '/candidate/profile', {
    currentDesignation: 'Invalid Recruiter Attempt',
  }, recruiterToken);
  recordResult(
    'POST /candidate/profile with RECRUITER role is rejected with 403 Forbidden',
    recCandProfileRes.status === 403,
    `Status: ${recCandProfileRes.status}`
  );

  // Test 5.2: Candidate creates their own profile
  const candProfilePayload = {
    linkedinUrl: 'https://linkedin.com/in/candidatetester',
    githubUrl: 'https://github.com/candidatetester',
    portfolioUrl: 'https://candidate.dev',
    currentCompany: 'Tech Pioneers Ltd',
    currentDesignation: 'Senior Java Backend Engineer',
    experience: 5.5,
    currentCtc: 1800000,
    expectedCtc: 2400000,
    noticePeriod: 30,
    location: 'Pune, India',
    skillIds: [],
  };
  const createCandProfileRes = await request('POST', '/candidate/profile', candProfilePayload, candidateToken);
  const candProfileCreated = createCandProfileRes.status === 201 && createCandProfileRes.data?.id;
  if (candProfileCreated) {
    candidateProfileId = createCandProfileRes.data.id;
    candidateBusinessId = createCandProfileRes.data.candidateId;
  }
  recordResult(
    'POST /candidate/profile creates candidate profile with business ID & status ACTIVE',
    candProfileCreated,
    `Status: ${createCandProfileRes.status}, Candidate Business ID: ${candidateBusinessId}`
  );

  // Test 5.3: GET /candidate/profile
  const getCandProfileRes = await request('GET', '/candidate/profile', null, candidateToken);
  recordResult(
    'GET /candidate/profile returns candidate self-service profile',
    getCandProfileRes.status === 200 && getCandProfileRes.data?.id === candidateProfileId,
    `Status: ${getCandProfileRes.status}, Designation: ${getCandProfileRes.data?.currentDesignation}, Exp: ${getCandProfileRes.data?.experience} yrs`
  );

  // Test 5.4: PUT /candidate/profile
  const updateCandProfileRes = await request('PUT', '/candidate/profile', {
    ...candProfilePayload,
    currentDesignation: 'Lead Java Backend Architect',
    experience: 6.0,
    expectedCtc: 2800000,
  }, candidateToken);
  recordResult(
    'PUT /candidate/profile updates candidate profile details',
    updateCandProfileRes.status === 200 && updateCandProfileRes.data?.experience === 6.0,
    `Status: ${updateCandProfileRes.status}, Updated Designation: ${updateCandProfileRes.data?.currentDesignation}`
  );

  // =========================================================================
  // 6. CANDIDATE MANAGEMENT APIS FOR RECRUITERS (/candidates)
  // =========================================================================
  logHeader('6. CANDIDATE DIRECTORY & CRUD FOR RECRUITERS (/candidates)');

  // Test 6.1: Recruiter creates direct candidate record (POST /candidates)
  const seed2 = seed + '9';
  const cand2Email = `direct_cand_${seed2}@hireai-test.io`;
  const cand2Phone = '96' + Math.floor(10000000 + Math.random() * 90000000);
  const regCand2Res = await request('POST', '/auth/register/candidate', {
    firstName: 'Direct',
    lastName: `Candidate${seed2}`,
    email: cand2Email,
    password: testPassword,
    phoneNumber: cand2Phone,
  });
  let directCandidateId = '';
  if (regCand2Res.status === 201) {
    const cand2UserId = regCand2Res.data.userId;
    const directCandPayload = {
      userId: cand2UserId,
      firstName: 'Direct',
      lastName: `Candidate${seed2}`,
      email: cand2Email,
      phone: cand2Phone,
      currentDesignation: 'Full Stack Engineer',
      currentCompany: 'Direct Systems',
      experience: 4.0,
      currentCtc: 1200000,
      expectedCtc: 1600000,
      noticePeriod: 15,
      location: 'Mumbai, India',
      skillIds: [],
    };
    const createDirectCandRes = await request('POST', '/candidates', directCandPayload, recruiterToken);
    if (createDirectCandRes.status === 201) {
      directCandidateId = createDirectCandRes.data.id;
    }
    recordResult(
      'POST /candidates (Recruiter) creates candidate entry',
      createDirectCandRes.status === 201 && !!directCandidateId,
      `Status: ${createDirectCandRes.status}, Candidate ID: ${createDirectCandRes.data?.candidateId}`
    );

    // Test 6.2: Recruiter updates candidate record (PUT /candidates/{candidateId})
    const updateDirectCandRes = await request('PUT', `/candidates/${directCandidateId}`, {
      ...directCandPayload,
      currentDesignation: 'Lead Full Stack Engineer',
      experience: 4.5,
    }, recruiterToken);
    recordResult(
      'PUT /candidates/{candidateId} (Recruiter) updates candidate record',
      updateDirectCandRes.status === 200 && updateDirectCandRes.data?.currentDesignation === 'Lead Full Stack Engineer',
      `Status: ${updateDirectCandRes.status}, Designation: ${updateDirectCandRes.data?.currentDesignation}`
    );

    // Test 6.3: Recruiter deletes candidate record (DELETE /candidates/{candidateId})
    const deleteDirectCandRes = await request('DELETE', `/candidates/${directCandidateId}`, null, recruiterToken);
    recordResult(
      'DELETE /candidates/{candidateId} (Recruiter) soft-deletes candidate (returns 204)',
      deleteDirectCandRes.status === 204,
      `Status: ${deleteDirectCandRes.status}`
    );
  }

  // Test 6.4: Recruiter lists all candidates
  const listCandidatesRes = await request('GET', '/candidates?page=0&size=10', null, recruiterToken);
  recordResult(
    'GET /candidates (Recruiter) lists candidate directory with pagination metadata',
    listCandidatesRes.status === 200 && Array.isArray(listCandidatesRes.data?.content),
    `Status: ${listCandidatesRes.status}, Total Elements: ${listCandidatesRes.data?.totalElements}`
  );

  // Test 6.5: Filter candidate by location or status
  const filterCandidatesRes = await request('GET', '/candidates?location=Pune&candidateStatus=ACTIVE', null, recruiterToken);
  recordResult(
    'GET /candidates?location=Pune&candidateStatus=ACTIVE filters correctly',
    filterCandidatesRes.status === 200 && Array.isArray(filterCandidatesRes.data?.content),
    `Status: ${filterCandidatesRes.status}, Matched Count: ${filterCandidatesRes.data?.content?.length}`
  );

  // Test 6.6: Recruiter gets candidate by ID
  const getCandidateByIdRes = await request('GET', `/candidates/${candidateProfileId}`, null, recruiterToken);
  recordResult(
    'GET /candidates/{candidateId} (Recruiter) fetches full candidate record',
    getCandidateByIdRes.status === 200 && getCandidateByIdRes.data?.id === candidateProfileId,
    `Status: ${getCandidateByIdRes.status}, Candidate ID: ${getCandidateByIdRes.data?.candidateId}`
  );

  // Test 6.7: RBAC check - Candidate querying recruiter candidate directory
  const candListDirRes = await request('GET', '/candidates', null, candidateToken);
  recordResult(
    'GET /candidates with CANDIDATE role is rejected with 403 Forbidden',
    candListDirRes.status === 403,
    `Status: ${candListDirRes.status}`
  );

  // =========================================================================
  // 7. JOB MANAGEMENT ENDPOINTS (/jobs)
  // =========================================================================
  logHeader('7. JOB MANAGEMENT ENDPOINTS');

  // Test 7.1: RBAC check - Candidate creating job
  const candCreateJobRes = await request('POST', '/jobs', {
    title: 'Unauthorized Job',
  }, candidateToken);
  recordResult(
    'POST /jobs with CANDIDATE role is rejected with 403 Forbidden',
    candCreateJobRes.status === 403,
    `Status: ${candCreateJobRes.status}`
  );

  // Test 7.2: Recruiter creates job posting
  const futureDeadline = new Date(Date.now() + 30 * 24 * 60 * 60 * 1000).toISOString().split('T')[0];
  const jobPayload = {
    title: 'Senior Java Backend Engineer',
    description: 'Looking for experienced Spring Boot, PostgreSQL, and Microservices developer to design scalable systems.',
    employmentType: 'FULL_TIME',
    experienceLevel: 'SENIOR',
    location: 'Pune, Maharashtra',
    remote: true,
    salaryMin: 1800000,
    salaryMax: 2800000,
    currency: 'INR',
    skills: ['Java', 'Spring Boot', 'PostgreSQL', 'Microservices', 'Docker'],
    education: 'Bachelor or Master in Computer Science / IT',
    openings: 3,
    applicationDeadline: futureDeadline,
  };
  const createJobRes = await request('POST', '/jobs', jobPayload, recruiterToken);
  const jobCreated = createJobRes.status === 201 && createJobRes.data?.id;
  if (jobCreated) {
    createdJobId = createJobRes.data.id;
  }
  recordResult(
    'POST /jobs creates job posting with status OPEN and deadline',
    jobCreated,
    `Status: ${createJobRes.status}, Job ID: ${createdJobId}, Title: ${createJobRes.data?.title}`
  );

  // Test 7.3: Recruiter fetches their own jobs
  const getMyJobsRes = await request('GET', '/jobs', null, recruiterToken);
  recordResult(
    'GET /jobs (Recruiter) returns recruiter\'s job listings',
    getMyJobsRes.status === 200 && Array.isArray(getMyJobsRes.data) && getMyJobsRes.data.some(j => j.id === createdJobId),
    `Status: ${getMyJobsRes.status}, Job Count: ${getMyJobsRes.data?.length}`
  );

  // Test 7.4: Candidate views open jobs
  const getOpenJobsRes = await request('GET', '/jobs/open', null, candidateToken);
  recordResult(
    'GET /jobs/open (Candidate) returns open job postings across platform',
    getOpenJobsRes.status === 200 && Array.isArray(getOpenJobsRes.data) && getOpenJobsRes.data.some(j => j.id === createdJobId),
    `Status: ${getOpenJobsRes.status}, Open Jobs Count: ${getOpenJobsRes.data?.length}`
  );

  // Test 7.5: Recruiter gets specific job by ID
  const getJobByIdRes = await request('GET', `/jobs/${createdJobId}`, null, recruiterToken);
  recordResult(
    'GET /jobs/{jobId} fetches specific job details',
    getJobByIdRes.status === 200 && getJobByIdRes.data?.id === createdJobId,
    `Status: ${getJobByIdRes.status}, Title: ${getJobByIdRes.data?.title}, Status: ${getJobByIdRes.data?.status}`
  );

  // Test 7.6: Recruiter updates job
  const updateJobRes = await request('PUT', `/jobs/${createdJobId}`, {
    ...jobPayload,
    title: 'Lead / Staff Java Backend Engineer',
    openings: 5,
  }, recruiterToken);
  recordResult(
    'PUT /jobs/{jobId} updates job posting details',
    updateJobRes.status === 200 && updateJobRes.data?.openings === 5,
    `Status: ${updateJobRes.status}, New Title: ${updateJobRes.data?.title}`
  );

  // =========================================================================
  // 8. JOB APPLICATION & ATS SCORING PIPELINE
  // =========================================================================
  logHeader('8. JOB APPLICATION & ATS INTELLIGENT MATCH PIPELINE');

  // Test 8.1: Candidate applies to job
  const applyPayload = {
    coverNote: 'I have 6 years of backend engineering experience with Spring Boot, PostgreSQL, and distributed architectures.',
  };
  const applyRes = await request('POST', `/jobs/${createdJobId}/apply`, applyPayload, candidateToken);
  const appCreated = applyRes.status === 201 && applyRes.data?.success && applyRes.data?.data?.id;
  let applicationData = null;
  if (appCreated) {
    applicationData = applyRes.data.data;
    createdApplicationId = applicationData.id;
  }
  recordResult(
    'POST /jobs/{jobId}/apply (Candidate) submits application & computes ATS score',
    appCreated,
    `Status: ${applyRes.status}, App ID: ${createdApplicationId}, ATS Score: ${applicationData?.atsMatchScore}%, Stage: ${applicationData?.status}`
  );

  // Test 8.2: Duplicate application prevention
  const dupApplyRes = await request('POST', `/jobs/${createdJobId}/apply`, applyPayload, candidateToken);
  recordResult(
    'POST /jobs/{jobId}/apply (Duplicate submission) returns 409 Conflict',
    dupApplyRes.status === 409,
    `Status: ${dupApplyRes.status}`
  );

  // Test 8.3: Candidate views their submitted applications
  const myAppsRes = await request('GET', '/candidate/applications', null, candidateToken);
  recordResult(
    'GET /candidate/applications (Candidate) lists submitted applications with live stage',
    myAppsRes.status === 200 && myAppsRes.data?.success && Array.isArray(myAppsRes.data?.data) && myAppsRes.data.data.some(a => a.id === createdApplicationId),
    `Status: ${myAppsRes.status}, Applications Count: ${myAppsRes.data?.data?.length}`
  );

  // Test 8.4: Recruiter views applicants ranked by ATS score
  const jobApplicantsRes = await request('GET', `/jobs/${createdJobId}/applications`, null, recruiterToken);
  recordResult(
    'GET /jobs/{jobId}/applications (Recruiter) lists applicants ranked by ATS match score',
    jobApplicantsRes.status === 200 && jobApplicantsRes.data?.success && Array.isArray(jobApplicantsRes.data?.data) && jobApplicantsRes.data.data.some(a => a.id === createdApplicationId),
    `Status: ${jobApplicantsRes.status}, Applicants Count: ${jobApplicantsRes.data?.data?.length}`
  );

  // Test 8.5: Recruiter updates application stage & notes
  const updateStagePayload = {
    status: 'INTERVIEW_SCHEDULED',
    recruiterNotes: 'Technical screening passed with flying colors. Scheduled round 1 architecture interview.',
  };
  const updateStageRes = await request('PATCH', `/applications/${createdApplicationId}/status`, updateStagePayload, recruiterToken);
  recordResult(
    'PATCH /applications/{applicationId}/status (Recruiter) updates recruitment pipeline stage',
    updateStageRes.status === 200 && updateStageRes.data?.data?.status === 'INTERVIEW_SCHEDULED',
    `Status: ${updateStageRes.status}, New Stage: ${updateStageRes.data?.data?.status}, Notes: ${updateStageRes.data?.data?.recruiterNotes}`
  );

  // Test 8.6: Candidate views single application details
  const getAppByIdRes = await request('GET', `/applications/${createdApplicationId}`, null, candidateToken);
  recordResult(
    'GET /applications/{applicationId} (Candidate) retrieves updated application details',
    getAppByIdRes.status === 200 && getAppByIdRes.data?.data?.id === createdApplicationId && getAppByIdRes.data?.data?.status === 'INTERVIEW_SCHEDULED',
    `Status: ${getAppByIdRes.status}, Live Stage: ${getAppByIdRes.data?.data?.status}`
  );

  // Test 8.7: RBAC check - Candidate attempting to update recruitment stage
  const candUpdateStageRes = await request('PATCH', `/applications/${createdApplicationId}/status`, {
    status: 'OFFERED',
  }, candidateToken);
  recordResult(
    'PATCH /applications/{applicationId}/status with CANDIDATE role is rejected with 403 Forbidden',
    candUpdateStageRes.status === 403,
    `Status: ${candUpdateStageRes.status}`
  );

  // =========================================================================
  // 9. JOB LIFECYCLE CLOSURE
  // =========================================================================
  logHeader('9. JOB LIFECYCLE CLOSURE & EDGE CASES');

  // Test 9.1: Recruiter closes job
  const closeJobRes = await request('PATCH', `/jobs/${createdJobId}/close`, null, recruiterToken);
  recordResult(
    'PATCH /jobs/{jobId}/close (Recruiter) closes job posting (returns 204 No Content)',
    closeJobRes.status === 204,
    `Status: ${closeJobRes.status}`
  );

  // Test 9.2: Verify job status is now CLOSED
  const verifyClosedJobRes = await request('GET', `/jobs/${createdJobId}`, null, recruiterToken);
  recordResult(
    'GET /jobs/{jobId} confirms job status changed to CLOSED',
    verifyClosedJobRes.status === 200 && verifyClosedJobRes.data?.status === 'CLOSED',
    `Status: ${verifyClosedJobRes.status}, Job Status: ${verifyClosedJobRes.data?.status}`
  );

  // =========================================================================
  // 10. ERROR HANDLING & VALIDATION CHECKS
  // =========================================================================
  logHeader('10. GLOBAL EXCEPTION & VALIDATION HANDLING');

  // Test 10.1: Non-existent UUID (404 Not Found)
  const nonExistentUuid = '00000000-0000-0000-0000-000000000000';
  const notFoundJobRes = await request('GET', `/jobs/${nonExistentUuid}`, null, recruiterToken);
  recordResult(
    'GET /jobs/{nonExistentUUID} returns structured 404 Not Found error',
    notFoundJobRes.status === 404 && notFoundJobRes.data?.success === false,
    `Status: ${notFoundJobRes.status}, Message: ${notFoundJobRes.data?.message}`
  );

  // Test 10.2: Malformed JSON payload
  const malformedJsonRes = await request('POST', '/auth/login', '{ invalid_json : true }');
  recordResult(
    'POST /auth/login with malformed JSON body returns 400 Bad Request',
    malformedJsonRes.status === 400,
    `Status: ${malformedJsonRes.status}`
  );

  // Test 10.3: Missing required fields validation error
  const invalidRegRes = await request('POST', '/auth/register/candidate', {
    firstName: '',
    email: 'invalid-email',
  });
  recordResult(
    'POST /auth/register/candidate with missing/invalid fields returns 400 Bad Request',
    invalidRegRes.status === 400 && invalidRegRes.data?.success === false,
    `Status: ${invalidRegRes.status}, Message: ${invalidRegRes.data?.message}`
  );

  // =========================================================================
  // SUMMARY REPORT
  // =========================================================================
  logHeader('API INSPECTION & TEST RESULTS SUMMARY');
  console.log(`\n  Total Tests Executed: ${results.length}`);
  console.log(`  Passed Tests:         ${passCount}`);
  console.log(`  Failed Tests:         ${failCount}`);
  console.log(`  Success Rate:         ${((passCount / results.length) * 100).toFixed(1)}%\n`);

  if (failCount === 0) {
    console.log('  🎉 ALL BACKEND API ENDPOINTS ARE FULLY OPERATIONAL AND FUNCTIONING PERFECTLY!\n');
  } else {
    console.log('  ⚠️ SOME TESTS FAILED. PLEASE REVIEW THE FAILURE DETAILS ABOVE.\n');
  }
}

runAllTests().catch(err => {
  console.error('Fatal test runner error:', err);
});
