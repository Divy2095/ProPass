import { z } from 'zod';

export const updateProfileSchema = z.object({
  fullName: z
    .string({ required_error: 'Full name is required' })
    .trim()
    .min(1, 'Full name cannot be empty')
    .max(100, 'Full name cannot exceed 100 characters')
    .optional(),
  title: z
    .string()
    .trim()
    .max(100, 'Title cannot exceed 100 characters')
    .nullable()
    .optional(),
  organization: z
    .string()
    .trim()
    .max(100, 'Organization cannot exceed 100 characters')
    .nullable()
    .optional(),
  phone: z
    .string()
    .trim()
    .max(30, 'Phone number cannot exceed 30 characters')
    .regex(/^[+0-9\s\-().]*$/, 'Phone number contains invalid characters')
    .nullable()
    .optional(),
  linkedinUrl: z
    .string()
    .trim()
    .url('LinkedIn URL must be a valid URL')
    .nullable()
    .optional()
    .or(z.literal('')),
  avatarUrl: z
    .string()
    .trim()
    .url('Avatar URL must be a valid URL')
    .nullable()
    .optional()
    .or(z.literal('')),
});

export type UpdateProfileInput = z.infer<typeof updateProfileSchema>;
