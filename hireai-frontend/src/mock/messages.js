// TODO: Replace mock data with API response from FastAPI /messages endpoint

export const MOCK_MESSAGES = [
  {
    id: 'msg-1',
    conversationId: 'conv-1',
    senderId: 'rec-1',
    content: 'Hi Alex! We reviewed your application for the Senior Frontend Engineer role and we are very impressed. Would you be available for a 30-minute intro call this week?',
    sentAt: new Date(Date.now() - 2 * 60 * 60 * 1000).toISOString(),
    read: true,
  },
  {
    id: 'msg-2',
    conversationId: 'conv-1',
    senderId: 'cand-1',
    content: 'Hi Jordan! Thank you so much — I would love that. I am free Thursday afternoon or Friday morning. Whatever works best for you!',
    sentAt: new Date(Date.now() - 90 * 60 * 1000).toISOString(),
    read: true,
  },
  {
    id: 'msg-3',
    conversationId: 'conv-1',
    senderId: 'rec-1',
    content: 'Thursday at 3pm PT works perfectly. I will send you a calendar invite shortly. Looking forward to chatting!',
    sentAt: new Date(Date.now() - 30 * 60 * 1000).toISOString(),
    read: false,
  },
  {
    id: 'msg-4',
    conversationId: 'conv-2',
    senderId: 'rec-2',
    content: 'Hey Alex, I saw your profile on HireAI and think you would be a great fit for our Product Designer role. We are building something really exciting here at Linear.',
    sentAt: new Date(Date.now() - 24 * 60 * 60 * 1000).toISOString(),
    read: true,
  },
  {
    id: 'msg-5',
    conversationId: 'conv-2',
    senderId: 'cand-1',
    content: 'Thanks for reaching out! Linear is one of my favorite products. I would love to learn more about the role.',
    sentAt: new Date(Date.now() - 22 * 60 * 60 * 1000).toISOString(),
    read: true,
  },
];

export const MOCK_CONVERSATIONS = [
  {
    id: 'conv-1',
    participants: ['cand-1', 'rec-1'],
    participantDetails: [
      { id: 'cand-1', name: 'Alex Rivera', role: 'candidate' },
      { id: 'rec-1', name: 'Jordan Kim', role: 'recruiter' },
    ],
    lastMessage: MOCK_MESSAGES[2],
    updatedAt: MOCK_MESSAGES[2].sentAt,
    unreadCount: 1,
    jobContext: { id: 'job-1', title: 'Senior Frontend Engineer' },
  },
  {
    id: 'conv-2',
    participants: ['cand-1', 'rec-2'],
    participantDetails: [
      { id: 'cand-1', name: 'Alex Rivera', role: 'candidate' },
      { id: 'rec-2', name: 'Sam Park', role: 'recruiter' },
    ],
    lastMessage: MOCK_MESSAGES[4],
    updatedAt: MOCK_MESSAGES[4].sentAt,
    unreadCount: 0,
    jobContext: { id: 'job-3', title: 'Product Designer' },
  },
  {
    id: 'conv-3',
    participants: ['cand-1', 'rec-3'],
    participantDetails: [
      { id: 'cand-1', name: 'Alex Rivera', role: 'candidate' },
      { id: 'rec-3', name: 'Mia Torres', role: 'recruiter' },
    ],
    lastMessage: undefined,
    updatedAt: new Date(Date.now() - 3 * 24 * 60 * 60 * 1000).toISOString(),
    unreadCount: 0,
    jobContext: { id: 'job-5', title: 'Full Stack Engineer' },
  },
];
