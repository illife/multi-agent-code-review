import React from 'react'
import { cn } from '../../lib/utils'

export interface BadgeProps extends React.HTMLAttributes<HTMLDivElement> {
  variant?: 'default' | 'success' | 'warning' | 'error' | 'info'
  size?: 'sm' | 'md' | 'lg'
}

export const Badge = React.forwardRef<HTMLDivElement, BadgeProps>(
  ({ className, variant = 'default', size = 'md', ...props }, ref) => {
    const variants = {
      default: 'bg-slate-100 text-slate-800 dark:bg-slate-700 dark:text-slate-300',
      success: 'bg-success-100 text-success-800 dark:bg-success-900 dark:text-success-300',
      warning: 'bg-warning-100 text-warning-800 dark:bg-warning-900 dark:text-warning-300',
      error: 'bg-error-100 text-error-800 dark:bg-error-900 dark:text-error-300',
      info: 'bg-primary-100 text-primary-800 dark:bg-primary-900 dark:text-primary-300',
    }

    const sizes = {
      sm: 'px-2 py-0.5 text-xs',
      md: 'px-2.5 py-1 text-sm',
      lg: 'px-3 py-1.5 text-base',
    }

    return (
      <div
        ref={ref}
        className={cn(
          'inline-flex items-center rounded-full font-medium',
          variants[variant],
          sizes[size],
          className
        )}
        {...props}
      />
    )
  }
)

Badge.displayName = 'Badge'

export default Badge
