import { prisma } from '../config/prisma.js';
import { FormService } from './form.service.js';

export class EventService {
  /**
   * Supported ProPass QR regex pattern:
   * Matches http/https, optional www, propass.id/event/<eventId>, optional trailing slash.
   */
  private static readonly PROPASS_QR_REGEX =
    /^(?:https?):\/\/(?:www\.)?propass\.id\/event\/([a-zA-Z0-9_-]+)\/?$/i;

  /**
   * Parses and extracts the event slug/ID from a raw QR code string.
   * Returns lowercase event ID if valid, or null if not a ProPass event QR.
   */
  static parseProPassQr(qrContent: string): string | null {
    if (!qrContent || typeof qrContent !== 'string') return null;

    const trimmed = qrContent.trim();
    const match = EventService.PROPASS_QR_REGEX.exec(trimmed);

    if (!match || !match[1]) return null;

    return match[1].toLowerCase();
  }

  /**
   * Looks up an event in PostgreSQL by either slug or UUID id.
   */
  static async getEventByIdOrSlug(identifier: string) {
    const cleanId = identifier.trim();

    const event = await prisma.event.findFirst({
      where: {
        OR: [
          { slug: cleanId.toLowerCase() },
          { id: cleanId },
        ],
      },
      select: {
        id: true,
        slug: true,
        title: true,
        overline: true,
        subtitle: true,
        description: true,
        location: true,
        startDate: true,
        endDate: true,
        maxDuration: true,
        isActive: true,
        date: true,
        startTime: true,
        endTime: true,
        organizerId: true,
        createdAt: true,
        updatedAt: true,
        _count: {
          select: { registrations: true },
        },
        form: {
          select: {
            id: true,
            eventId: true,
            questions: {
              orderBy: { orderIndex: 'asc' },
              select: {
                id: true,
                label: true,
                type: true,
                isRequired: true,
                options: true,
                isDefaultField: true,
                orderIndex: true,
              },
            },
          },
        },
      },
    });

    if (!event) {
      const error: any = new Error(`Event "${cleanId}" not found`);
      error.statusCode = 404;
      throw error;
    }

    if (!event.isActive) {
      const error: any = new Error(`Event "${event.title}" is currently inactive or concluded`);
      error.statusCode = 410; // 410 Gone / Inactive
      throw error;
    }

    const form = event.form ?? {
      id: `default-form-${event.id}`,
      eventId: event.id,
      questions: FormService.getDefaultQuestions(),
    };

    return {
      ...event,
      registrationCount: event._count?.registrations ?? 0,
      form,
    };
  }

  /**
   * Validates a raw QR code string, parses the event ID, and fetches active event metadata.
   */
  static async validateQrCode(qrContent: string) {
    const slug = EventService.parseProPassQr(qrContent);

    if (!slug) {
      const error: any = new Error(
        'Invalid ProPass QR code format. Expected format: https://propass.id/event/<eventId>'
      );
      error.statusCode = 400;
      throw error;
    }

    const event = await EventService.getEventByIdOrSlug(slug);

    return {
      event,
      qrPayload: qrContent.trim(),
      parsedSlug: slug,
    };
  }

  /**
   * Generates a unique event slug from title.
   * Collisions are handled gracefully by appending an incrementing suffix.
   */
  static async generateUniqueSlug(baseTitle: string, customSlug?: string): Promise<string> {
    let candidate = (customSlug || baseTitle)
      .toLowerCase()
      .replace(/[^a-z0-9]+/g, '-')
      .replace(/^-+|-+$/g, '');

    if (!candidate) candidate = 'event';

    let slug = candidate;
    let counter = 1;

    while (true) {
      const existing = await prisma.event.findUnique({ where: { slug } });
      if (!existing) return slug;
      counter++;
      slug = `${candidate}-${counter}`;
    }
  }

  /**
   * Converts local date and time strings into ISO start and end timestamps.
   */
  private static parseEventDates(dateStr: string, startTimeStr?: string, endTimeStr?: string, durationDays: number = 1) {
    let startDate = new Date(dateStr);
    if (isNaN(startDate.getTime())) {
      startDate = new Date();
    }

    if (startTimeStr && startTimeStr.trim().length > 0) {
      const match = startTimeStr.trim().match(/(\d{1,2}):(\d{2})(?:\s*([APap][Mm]))?/);
      if (match) {
        let hours = parseInt(match[1], 10);
        const minutes = parseInt(match[2], 10);
        const meridiem = match[3]?.toUpperCase();
        if (meridiem === 'PM' && hours < 12) hours += 12;
        if (meridiem === 'AM' && hours === 12) hours = 0;
        startDate.setHours(hours, minutes, 0, 0);
      }
    }

    let endDate = new Date(startDate.getTime());
    if (endTimeStr && endTimeStr.trim().length > 0) {
      const match = endTimeStr.trim().match(/(\d{1,2}):(\d{2})(?:\s*([APap][Mm]))?/);
      if (match) {
        let hours = parseInt(match[1], 10);
        const minutes = parseInt(match[2], 10);
        const meridiem = match[3]?.toUpperCase();
        if (meridiem === 'PM' && hours < 12) hours += 12;
        if (meridiem === 'AM' && hours === 12) hours = 0;
        endDate.setHours(hours, minutes, 0, 0);
      }
    }

    if (endDate.getTime() <= startDate.getTime()) {
      endDate = new Date(startDate.getTime() + durationDays * 24 * 60 * 60 * 1000);
    }

    return { startDate, endDate };
  }

  /**
   * Creates and persists a new organizer event in PostgreSQL.
   */
  static async createEvent(input: any, organizerId: string) {
    const title = input.title || input.name || 'Untitled Event';
    const duration = input.maxDuration || input.maxDurationDays || 1;
    const slug = await EventService.generateUniqueSlug(title, input.slug);
    const { startDate, endDate } = EventService.parseEventDates(input.date, input.startTime, input.endTime, duration);

    const event = await prisma.event.create({
      data: {
        title,
        slug,
        overline: 'EVENT REGISTRATION',
        subtitle: input.description && input.description.trim().length > 0
          ? input.description.trim()
          : 'Complete your registration to secure your spot.',
        description: input.description?.trim() || null,
        location: input.location.trim(),
        date: input.date.trim(),
        startTime: input.startTime?.trim() || null,
        endTime: input.endTime?.trim() || null,
        startDate,
        endDate,
        maxDuration: duration,
        isActive: true,
        organizerId,
      },
    });

    return {
      ...event,
      qrPayload: `https://propass.id/event/${event.slug}`,
    };
  }

  /**
   * Retrieves all events created by a specific organizer.
   */
  static async getEventsByOrganizer(organizerId: string) {
    const events = await prisma.event.findMany({
      where: { organizerId },
      include: {
        _count: {
          select: { registrations: true },
        },
      },
      orderBy: { createdAt: 'desc' },
    });

    return events.map((event) => ({
      ...event,
      registrationCount: event._count?.registrations ?? 0,
      qrPayload: `https://propass.id/event/${event.slug}`,
    }));
  }
}
