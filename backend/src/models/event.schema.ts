import { z } from 'zod';

export const validateQrSchema = z.object({
  qrContent: z
    .string({ required_error: 'QR content is required' })
    .trim()
    .min(1, 'QR content cannot be empty')
    .max(2048, 'QR content is too long'),
});

export type ValidateQrInput = z.infer<typeof validateQrSchema>;

export const eventParamsSchema = z.object({
  eventId: z
    .string({ required_error: 'Event identifier is required' })
    .trim()
    .min(1, 'Event identifier cannot be empty')
    .max(100, 'Event identifier is too long'),
});

export type EventParamsInput = z.infer<typeof eventParamsSchema>;

export const createEventSchema = z
  .object({
    name: z.string().trim().min(2, 'Event name must be at least 2 characters long').max(120).optional(),
    title: z.string().trim().min(2, 'Event title must be at least 2 characters long').max(120).optional(),
    description: z.string().trim().max(2000, 'Description cannot exceed 2000 characters').optional().default(''),
    date: z.string({ required_error: 'Event date is required' }).trim().min(1, 'Event date is required'),
    startTime: z.string().trim().optional().default(''),
    endTime: z.string().trim().optional().default(''),
    location: z
      .string({ required_error: 'Event location is required' })
      .trim()
      .min(2, 'Event location is required')
      .max(200, 'Event location cannot exceed 200 characters'),
    maxDuration: z.number().int('Max duration must be an integer').min(1, 'Duration must be at least 1 day').max(30).optional(),
    maxDurationDays: z.number().int('Max duration must be an integer').min(1, 'Duration must be at least 1 day').max(30).optional(),
    slug: z.string().trim().optional(),
  })
  .refine((data) => Boolean(data.name || data.title), {
    message: 'Event name is required',
    path: ['name'],
  });

export type CreateEventInput = z.infer<typeof createEventSchema>;
