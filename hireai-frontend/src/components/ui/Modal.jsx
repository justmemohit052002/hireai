import React from 'react';
import * as DialogPrimitive from '@radix-ui/react-dialog';
import { X } from 'lucide-react';
import { AnimatePresence, motion } from 'framer-motion';
import { cn } from '@/utils';

export const Modal = ({
  isOpen,
  onClose,
  title,
  description,
  children,
  maxWidth = 'md',
}) => {
  const maxWidthMap = {
    sm: 'max-w-sm',
    md: 'max-w-md',
    lg: 'max-w-lg',
    xl: 'max-w-xl',
    '2xl': 'max-w-2xl',
    '4xl': 'max-w-4xl',
  };

  return (
    <DialogPrimitive.Root open={isOpen} onOpenChange={(open) => !open && onClose()}>
      <AnimatePresence>
        {isOpen && (
          <DialogPrimitive.Portal forceMount>
            {/* Backdrop */}
            <DialogPrimitive.Overlay asChild>
              <motion.div
                initial={{ opacity: 0 }}
                animate={{ opacity: 1 }}
                exit={{ opacity: 0 }}
<<<<<<< HEAD
                className="fixed inset-0 z-50 bg-black/60 backdrop-blur-xs"
=======
                className="fixed inset-0 z-50 bg-black/70 backdrop-blur-sm"
>>>>>>> origin/main
              />
            </DialogPrimitive.Overlay>

            {/* Modal Box */}
            <DialogPrimitive.Content asChild>
              <div className="fixed inset-0 z-50 flex items-center justify-center p-3 sm:p-4 overflow-y-auto">
                <motion.div
                  initial={{ opacity: 0, scale: 0.95, y: 8 }}
                  animate={{ opacity: 1, scale: 1, y: 0 }}
                  exit={{ opacity: 0, scale: 0.95, y: 8 }}
                  transition={{ type: 'spring', damping: 25, stiffness: 350 }}
                  className={cn(
<<<<<<< HEAD
                    'relative w-full rounded-xl bg-surface border border-border p-6 md:p-8 shadow-xl overflow-hidden text-foreground',
=======
                    'relative w-full my-auto rounded-3xl glass border border-white/20 dark:border-white/10 p-5 sm:p-6 md:p-8 shadow-2xl max-h-[90vh] flex flex-col',
>>>>>>> origin/main
                    maxWidthMap[maxWidth]
                  )}
                >
                  {/* Close button */}
<<<<<<< HEAD
                  <DialogPrimitive.Close className="absolute top-4 right-4 rounded-lg p-2 text-muted-foreground hover:bg-muted hover:text-foreground transition-colors focus:outline-none focus:ring-2 focus:ring-brand-blue">
=======
                  <DialogPrimitive.Close className="absolute top-4 right-4 rounded-full p-2 text-muted-foreground hover:bg-muted hover:text-foreground transition-colors focus:outline-none z-10">
>>>>>>> origin/main
                    <X className="w-5 h-5" />
                  </DialogPrimitive.Close>

                  {(title || description) && (
                    <div className="mb-4 space-y-1 pr-8 shrink-0">
                      {title && (
                        <DialogPrimitive.Title className="text-xl sm:text-2xl font-bold font-heading text-foreground tracking-tight">
                          {title}
                        </DialogPrimitive.Title>
                      )}
                      {description && (
                        <DialogPrimitive.Description className="text-xs sm:text-sm text-muted-foreground">
                          {description}
                        </DialogPrimitive.Description>
                      )}
                    </div>
                  )}

                  <div className="overflow-y-auto pr-1 -mr-1">{children}</div>
                </motion.div>
              </div>
            </DialogPrimitive.Content>
          </DialogPrimitive.Portal>
        )}
      </AnimatePresence>
    </DialogPrimitive.Root>
  );
};
