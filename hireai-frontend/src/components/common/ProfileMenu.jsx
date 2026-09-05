import React from 'react';
import { useNavigate } from 'react-router-dom';
import { LogOut, Settings, User as UserIcon, MessageSquare } from 'lucide-react';
import { useAuth } from '@/context/AuthContext';
import { Avatar } from '@/components/ui/Avatar';
import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuItem,
  DropdownMenuSeparator,
  DropdownMenuTrigger,
} from '@/components/ui/Dropdown';
import { ROUTES } from '@/constants';

export const ProfileMenu = () => {
  const { user, role, logout } = useAuth();
  const navigate = useNavigate();

  if (!user) return null;

  const displayName =
    user.name ||
    (user.firstName ? `${user.firstName} ${user.lastName || ''}`.trim() : user.email?.split('@')[0] || 'User');

  const profileRoute = role === 'candidate' ? ROUTES.CANDIDATE_PROFILE : ROUTES.RECRUITER_PROFILE;
  const inboxRoute = role === 'candidate' ? ROUTES.CANDIDATE_INBOX : ROUTES.RECRUITER_INBOX;
  const settingsRoute = role === 'candidate' ? ROUTES.CANDIDATE_SETTINGS : ROUTES.RECRUITER_SETTINGS;

  const handleLogout = async () => {
    await logout();
    navigate(ROUTES.HOME);
  };

  return (
    <DropdownMenu>
      <DropdownMenuTrigger asChild>
        <button className="flex items-center gap-2.5 rounded-full p-1 hover:bg-muted/60 transition-colors outline-none focus-visible:ring-2 focus-visible:ring-[#F56681]">
          <Avatar name={displayName} src={user.avatarUrl} size="md" />
          <div className="hidden md:flex flex-col text-left">
            <span className="text-xs font-bold text-foreground leading-tight">{displayName}</span>
            <span className="text-[10px] text-muted-foreground capitalize font-medium">{role}</span>
          </div>
        </button>
      </DropdownMenuTrigger>

      <DropdownMenuContent align="end" className="w-52 p-1">
        <div className="px-3 py-2 border-b border-border/40">
          <p className="font-semibold text-xs text-foreground truncate">{displayName}</p>
          <p className="text-[11px] text-muted-foreground truncate">{user.email}</p>
        </div>

        <div className="py-1">
          <DropdownMenuItem onClick={() => navigate(profileRoute)}>
            <UserIcon className="w-3.5 h-3.5 text-muted-foreground" />
            <span>Profile</span>
          </DropdownMenuItem>

          <DropdownMenuItem onClick={() => navigate(inboxRoute)}>
            <MessageSquare className="w-3.5 h-3.5 text-muted-foreground" />
            <span>Inbox</span>
          </DropdownMenuItem>

          <DropdownMenuItem onClick={() => navigate(settingsRoute)}>
            <Settings className="w-3.5 h-3.5 text-muted-foreground" />
            <span>Settings</span>
          </DropdownMenuItem>

          <DropdownMenuSeparator />

          <DropdownMenuItem onClick={handleLogout} className="text-red-500 focus:text-red-600 focus:bg-red-500/10">
            <LogOut className="w-3.5 h-3.5" />
            <span>Sign Out</span>
          </DropdownMenuItem>
        </div>
      </DropdownMenuContent>
    </DropdownMenu>
  );
};
