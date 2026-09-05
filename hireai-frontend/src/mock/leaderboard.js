import { MOCK_LEADERBOARD_CANDIDATES } from './users';

// TODO: Replace mock data with API response from FastAPI /leaderboard endpoint

export const MOCK_LEADERBOARD = [
  {
    rank: 1,
    candidate: MOCK_LEADERBOARD_CANDIDATES[0],
    score: 98,
    matchPercentage: 97,
    applicationCount: 3,
    badges: ['top-performer', 'highly-skilled', 'verified'],
    trend: 'stable',
  },
  {
    rank: 2,
    candidate: MOCK_LEADERBOARD_CANDIDATES[1],
    score: 96,
    matchPercentage: 95,
    applicationCount: 5,
    badges: ['rising-star', 'fast-responder', 'highly-skilled'],
    trend: 'up',
  },
  {
    rank: 3,
    candidate: MOCK_LEADERBOARD_CANDIDATES[2],
    score: 95,
    matchPercentage: 93,
    applicationCount: 2,
    badges: ['top-performer', 'verified'],
    trend: 'down',
  },
  {
    rank: 4,
    candidate: MOCK_LEADERBOARD_CANDIDATES[3],
    score: 93,
    matchPercentage: 91,
    applicationCount: 4,
    badges: ['highly-skilled', 'fast-responder'],
    trend: 'up',
  },
  {
    rank: 5,
    candidate: MOCK_LEADERBOARD_CANDIDATES[4],
    score: 91,
    matchPercentage: 88,
    applicationCount: 1,
    badges: ['verified', 'highly-skilled'],
    trend: 'stable',
  },
  {
    rank: 6,
    candidate: MOCK_LEADERBOARD_CANDIDATES[5],
    score: 89,
    matchPercentage: 86,
    applicationCount: 6,
    badges: ['rising-star', 'fast-responder'],
    trend: 'up',
  },
];
