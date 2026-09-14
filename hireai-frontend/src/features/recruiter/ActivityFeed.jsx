import React from 'react';
import { UserCheck, FilePlus, MessageSquare, Award, Sparkles } from 'lucide-react';
import { Card } from '@/components/ui/Card';
import { Avatar } from '@/components/ui/Avatar';

const MOCK_ACTIVITIES = [
  {
    id: 'a1',
    user: 'Alex Rivera',
    action: 'applied to',
    target: 'Senior Frontend Engineer',
    time: '5m ago',
    icon: <FilePlus className="w-4 h-4 text-blue-500" />,
  },
  {
    id: 'a2',
    user: 'HireAI Engine',
    action: 'scored Priya Sharma 96% match for',
    target: 'ML Engineer',
    time: '22m ago',
    icon: <Sparkles className="w-4 h-4 text-purple-500" />,
  },
  {
    id: 'a3',
    user: 'Jordan Kim',
    action: 'sent interview invite to',
    target: 'Marcus Chen',
    time: '1h ago',
    icon: <UserCheck className="w-4 h-4 text-emerald-500" />,
  },
  {
    id: 'a4',
    user: 'Layla Hassan',
    action: 'submitted technical challenge for',
    target: 'Product Designer',
    time: '3h ago',
    icon: <Award className="w-4 h-4 text-amber-500" />,
  },
];

export const ActivityFeed = () => {
  return (
    <Card surface-nested className="p-6 space-y-4">
      <h3 className="text-lg font-bold font-heading text-foreground">Recent Activity</h3>
      <div className="space-y-4 divide-y divide-border/40">
        {MOCK_ACTIVITIES.map((act) => (
          <div key={act.id} className="pt-3 first:pt-0 flex items-start gap-3">
            <div className="p-2 rounded-xl bg-surface-2 border border-border/50 shrink-0">
              {act.icon}
            </div>
            <div className="flex-1 text-xs">
              <p className="text-foreground leading-relaxed">
                <span className="font-bold">{act.user}</span> {act.action}{' '}
                <span className="font-semibold text-blue-500">{act.target}</span>
              </p>
              <span className="text-[10px] text-muted-foreground mt-0.5 block">{act.time}</span>
            </div>
          </div>
        ))}
      </div>
    </Card>
  );
};
