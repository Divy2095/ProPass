import { PurposeOfVisit } from '@prisma/client';
import { z } from 'zod';

const purposeMapping: Record<string, PurposeOfVisit> = {
  GENERAL_ATTENDEE: PurposeOfVisit.GENERAL_ATTENDEE,
  'GENERAL ATTENDEE': PurposeOfVisit.GENERAL_ATTENDEE,
  'General Attendee': PurposeOfVisit.GENERAL_ATTENDEE,
  general_attendee: PurposeOfVisit.GENERAL_ATTENDEE,
  SPEAKER: PurposeOfVisit.SPEAKER,
  Speaker: PurposeOfVisit.SPEAKER,
  speaker: PurposeOfVisit.SPEAKER,
  SPONSOR_EXHIBITOR: PurposeOfVisit.SPONSOR_EXHIBITOR,
  'SPONSOR / EXHIBITOR': PurposeOfVisit.SPONSOR_EXHIBITOR,
  'Sponsor / Exhibitor': PurposeOfVisit.SPONSOR_EXHIBITOR,
  Sponsor: PurposeOfVisit.SPONSOR_EXHIBITOR,
  sponsor: PurposeOfVisit.SPONSOR_EXHIBITOR,
  MEDIA_PRESS: PurposeOfVisit.MEDIA_PRESS,
  'MEDIA / PRESS': PurposeOfVisit.MEDIA_PRESS,
  'Media / Press': PurposeOfVisit.MEDIA_PRESS,
  Media: PurposeOfVisit.MEDIA_PRESS,
  media: PurposeOfVisit.MEDIA_PRESS,
};

export const createRegistrationSchema = z.object({
  eventId: z
    .string({ required_error: 'Event identifier is required' })
    .trim()
    .min(1, 'Event identifier cannot be empty'),
  fullName: z
    .string({ required_error: 'Full name is required' })
    .trim()
    .min(1, 'Full name cannot be empty')
    .max(100, 'Full name cannot exceed 100 characters'),
  email: z
    .string({ required_error: 'Email is required' })
    .trim()
    .toLowerCase()
    .email('Invalid email address format'),
  institution: z
    .string({ required_error: 'Institution is required' })
    .trim()
    .min(1, 'Institution cannot be empty')
    .max(100, 'Institution cannot exceed 100 characters'),
  purpose: z
    .string({ required_error: 'Purpose of visit is required' })
    .trim()
    .refine((val) => val in purposeMapping, {
      message:
        'Invalid purpose of visit. Allowed values: GENERAL_ATTENDEE, SPEAKER, SPONSOR_EXHIBITOR, MEDIA_PRESS',
    })
    .transform((val) => purposeMapping[val] as PurposeOfVisit),
  durationDays: z
    .number({ required_error: 'Duration (days) is required' })
    .int('Duration must be a whole number of days')
    .min(1, 'Duration must be at least 1 day'),
  vehicleNumber: z
    .string()
    .trim()
    .max(30, 'Vehicle number cannot exceed 30 characters')
    .optional()
    .nullable()
    .transform((val) => (val && val.length > 0 ? val.toUpperCase() : null)),
});

export type CreateRegistrationInput = z.infer<typeof createRegistrationSchema>;
