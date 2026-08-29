import { RegistrationStatus } from '@prisma/client';
import { prisma } from '../config/prisma.js';
import { CreateRegistrationInput } from '../models/registration.schema.js';
import { EventService } from './event.service.js';

export class RegistrationService {
  /**
   * Submits and persists a new event registration for the authenticated user.
   */
  static async createRegistration(userId: string, input: CreateRegistrationInput) {
    // 1. Resolve and validate the target event
    const event = await EventService.getEventByIdOrSlug(input.eventId);

    // 2. Validate durationDays against event's maxDuration
    if (input.durationDays > event.maxDuration) {
      const error: any = new Error(
        `Duration (${input.durationDays} days) exceeds event maximum allowed duration (${event.maxDuration} days)`
      );
      error.statusCode = 400;
      throw error;
    }

    // 3. Check for existing duplicate registration
    const existingRegistration = await prisma.registration.findUnique({
      where: {
        userId_eventId: {
          userId,
          eventId: event.id,
        },
      },
    });

    if (existingRegistration) {
      const error: any = new Error(`You are already registered for "${event.title}"`);
      error.statusCode = 409;
      throw error;
    }

    // 4. Create the registration record in PostgreSQL
    const registration = await prisma.registration.create({
      data: {
        userId,
        eventId: event.id,
        fullName: input.fullName,
        email: input.email,
        institution: input.institution,
        purpose: input.purpose,
        durationDays: input.durationDays,
        vehicleNumber: input.vehicleNumber,
        status: RegistrationStatus.CONFIRMED,
      },
      include: {
        event: {
          select: {
            id: true,
            slug: true,
            title: true,
            overline: true,
            subtitle: true,
            location: true,
            startDate: true,
            endDate: true,
          },
        },
      },
    });

    return registration;
  }

  /**
   * Retrieves all event registrations belonging to the authenticated user.
   */
  static async getMyRegistrations(userId: string) {
    const registrations = await prisma.registration.findMany({
      where: { userId },
      orderBy: { registeredAt: 'desc' },
      include: {
        event: {
          select: {
            id: true,
            slug: true,
            title: true,
            overline: true,
            subtitle: true,
            location: true,
            startDate: true,
            endDate: true,
          },
        },
      },
    });

    return registrations;
  }
}
