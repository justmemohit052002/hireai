import React from 'react';
import * as DialogPrimitive from '@radix-ui/react-dialog';
import { X } from 'lucide-react';
import { AnimatePresence, motion } from 'framer-motion';
import { cn } from '@/utils';

export const Drawer = ({
  isOpen,
  onClose,
  title,
  description,
  children,
  position = 'bottom',
  width = 'lg',
}) => {
  const widthMap = {
    md: 'max-w-md',
    lg: 'max-w-xl',
    xl: 'max-w-2xl',
    full: 'max-w-full',
  };

  const isBottom = position === 'bottom';
  const initialMotion = isBottom
    ? { y: '100%', opacity: 0 }
    : { x: position === 'right' ? '100%' : '-100%', opacity: 0 };
  const animateMotion = isBottom ? { y: 0, opacity: 1 } : { x: 0, opacity: 1 };
  const exitMotion = isBottom
    ? { y: '100%', opacity: 0 }
    : { x: position === 'right' ? '100%' : '-100%', opacity: 0 };

  return (
    <DialogPrimitive.Root open={isOpen} onOpenChange={(open) => !open && onClose()}>
      <AnimatePresence>
        {isOpen && (
          <DialogPrimitive.Portal forceMount>
            <DialogPrimitive.Overlay asChild>
              <motion.div
                initial={{ opacity: 0 }}
                animate={{ opacity: 1 }}
                exit={{ opacity: 0 }}
                className="fixed inset-0 z-50 bg-black/60 backdrop-blur-sm"
              />
            </DialogPrimitive.Overlay>

            <DialogPrimitive.Content asChild>
              <motion.div
                initial={initialMotion}
                animate={animateMotion}
                exit={exitMotion}
                transition={{ type: 'spring', damping: 28, stiffness: 280 }}
                className={cn(
                  'fixed z-50 glass shadow-2xl flex flex-col',
                  isBottom
                    ? 'bottom-0 inset-x-0 w-full max-w-4xl mx-auto max-h-[88vh] rounded-t-[28px] border-t border-x border-white/20 dark:border-white/10'
                    : cn(
                        'top-0 bottom-0 w-full',
                        position === 'right' ? 'right-0 border-l border-white/20 dark:border-white/10' : 'left-0 border-r border-white/20 dark:border-white/10',
                        widthMap[width]
                      )
                )}
              >
                {/* Grip Handle for Bottom Drawer */}
                {isBottom && (
                  <div className="w-12 h-1.5 rounded-full bg-muted-foreground/30 mx-auto mt-3 mb-1 shrink-0" />
                )}

                {/* Header */}
                <div className="flex items-center justify-between px-6 py-4 border-b border-border/50 shrink-0">
                  <div>
                    {title && (
                      <DialogPrimitive.Title className="text-xl font-bold font-heading text-foreground">
                        {title}
                      </DialogPrimitive.Title>
                    )}
                    {description && (
                      <DialogPrimitive.Description className="text-xs text-muted-foreground mt-0.5">
                        {description}
                      </DialogPrimitive.Description>
                    )}
                  </div>
                  <DialogPrimitive.Close className="rounded-full p-2 text-muted-foreground hover:bg-muted hover:text-foreground transition-colors ml-auto">
                    <X className="w-5 h-5" />
                  </DialogPrimitive.Close>
                </div>

                {/* Content */}
                <div className="flex-1 overflow-y-auto px-6 py-5 space-y-6">{children}</div>
              </motion.div>
            </DialogPrimitive.Content>
          </DialogPrimitive.Portal>
        )}
      </AnimatePresence>
    </DialogPrimitive.Root>
  );
};
