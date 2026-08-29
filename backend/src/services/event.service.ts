import { prisma } from '../config/prisma.js';

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
        createdAt: true,
        updatedAt: true,
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

    return event;
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
}
