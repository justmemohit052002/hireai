import React from 'react';
import { MapPin, Briefcase, Award, MessageSquare, ExternalLink } from 'lucide-react';
import { Card } from '@/components/ui/Card';
import { Avatar } from '@/components/ui/Avatar';
import { Badge } from '@/components/ui/Badge';
import { Button } from '@/components/ui/Button';

export const CandidateCard = ({
  candidate,
  rank,
  onMessage,
  onViewProfile,
}) => {
  return (
    <Card hoverable className="flex flex-col justify-between p-5 space-y-4">
      <div>
        {/* Header */}
        <div className="flex items-start justify-between">
          <div className="flex items-center gap-3">
            {rank && (
              <div className="w-7 h-7 rounded-full bg-brand-blue text-white font-extrabold text-xs flex items-center justify-center shadow-md">
                #{rank}
              </div>
            )}
            <Avatar name={candidate.name} src={candidate.avatarUrl} size="lg" />
            <div>
              <h3 className="font-bold text-base font-heading text-foreground">{candidate.name}</h3>
              <p className="text-xs text-muted-foreground font-medium">{candidate.title}</p>
            </div>
          </div>

          {candidate.score && (
            <div className="flex flex-col items-end">
              <span className="text-xs text-muted-foreground font-medium">AI Rank</span>
              <span className="text-lg font-extrabold text-[#F56681] font-mono">{candidate.score}/100</span>
            </div>
          )}
        </div>

        {/* Bio */}
        <p className="text-xs text-muted-foreground line-clamp-2 mt-3 leading-relaxed">
          {candidate.bio}
        </p>

        {/* Skills */}
        <div className="flex flex-wrap gap-1.5 mt-3">
          {candidate.skills.slice(0, 4).map((skill) => (
            <span
              key={skill}
              className="text-[10px] font-semibold px-2 py-0.5 rounded-md bg-muted/80 text-foreground border border-border/40"
            >
              {skill}
            </span>
          ))}
        </div>
      </div>

      {/* Footer */}
      <div className="flex items-center justify-between pt-3 border-t border-border/50">
        <span className="text-xs text-muted-foreground flex items-center gap-1">
          <MapPin className="w-3.5 h-3.5 text-[#F56681]" />
          {candidate.location}
        </span>

        <div className="flex items-center gap-2">
          {onMessage && (
            <Button size="sm" variant="ghost" onClick={() => onMessage(candidate)}>
              <MessageSquare className="w-3.5 h-3.5 mr-1" /> Message
            </Button>
          )}
          {onViewProfile && (
            <Button size="sm" variant="default" onClick={() => onViewProfile(candidate)}>
              Resume <ExternalLink className="w-3.5 h-3.5 ml-1" />
            </Button>
          )}
        </div>
      </div>
    </Card>
  );
};
