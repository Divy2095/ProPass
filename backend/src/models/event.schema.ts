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
