// ─────────────────────────────────────────
// Leaderboard API Stub
// TODO: Connect to FastAPI /leaderboard endpoints
// AI ranking engine will power this endpoint
// ─────────────────────────────────────────
export const leaderboardApi = {
  // TODO: GET /leaderboard — ranked candidate list for a job posting
  getLeaderboard: async (_jobId, _page) => {
    throw new Error('Not implemented — awaiting FastAPI integration');
  },

  // TODO: GET /leaderboard/global — global platform leaderboard
  getGlobalLeaderboard: async (_page) => {
    throw new Error('Not implemented — awaiting FastAPI integration');
  },

  // TODO: GET /leaderboard/candidate/:id/score — individual AI score breakdown
  getCandidateScore: async (_candidateId, _jobId) => {
    throw new Error('Not implemented — awaiting FastAPI integration');
  },
};
