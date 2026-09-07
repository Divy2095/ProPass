import { FastifyReply, FastifyRequest } from 'fastify';
import { ZodError } from 'zod';
import { createEventSchema, eventParamsSchema, validateQrSchema } from '../models/event.schema.js';
import { EventService } from '../services/event.service.js';

export class EventController {
  /**
   * GET /api/v1/events/:eventId
   */
  static async getEvent(req: FastifyRequest, reply: FastifyReply) {
    try {
      const { eventId } = eventParamsSchema.parse(req.params);
      const event = await EventService.getEventByIdOrSlug(eventId);

      return reply.status(200).send({
        success: true,
        data: {
          event,
        },
        timestamp: new Date().toISOString(),
      });
    } catch (error: any) {
      return EventController.handleError(error, reply);
    }
  }

  /**
   * POST /api/v1/events/validate-qr
   */
  static async validateQr(req: FastifyRequest, reply: FastifyReply) {
    try {
      const { qrContent } = validateQrSchema.parse(req.body);
      const result = await EventService.validateQrCode(qrContent);

      return reply.status(200).send({
        success: true,
        message: 'Valid ProPass event QR code',
        data: result,
        timestamp: new Date().toISOString(),
      });
    } catch (error: any) {
      return EventController.handleError(error, reply);
    }
  }

  /**
   * POST /api/v1/events (Protected - Organizer only)
   */
  static async createEvent(req: FastifyRequest, reply: FastifyReply) {
    try {
      const input = createEventSchema.parse(req.body);
      const organizerId = req.user!.userId;
      const event = await EventService.createEvent(input, organizerId);

      return reply.status(201).send({
        success: true,
        message: 'Event created successfully',
        data: {
          event,
        },
        timestamp: new Date().toISOString(),
      });
    } catch (error: any) {
      return EventController.handleError(error, reply);
    }
  }

  /**
   * GET /api/v1/organizer/events (Protected - Organizer only)
   */
  static async getOrganizerEvents(req: FastifyRequest, reply: FastifyReply) {
    try {
      const organizerId = req.user!.userId;
      const events = await EventService.getEventsByOrganizer(organizerId);

      return reply.status(200).send({
        success: true,
        data: {
          events,
        },
        timestamp: new Date().toISOString(),
      });
    } catch (error: any) {
      return EventController.handleError(error, reply);
    }
  }

  /**
   * Standard error handler
   */
  private static handleError(error: any, reply: FastifyReply) {
    if (error instanceof ZodError) {
      const formattedErrors = error.errors.map((e) => ({
        field: e.path.join('.'),
        message: e.message,
      }));

      return reply.status(400).send({
        success: false,
        error: 'Validation Error',
        message: formattedErrors[0]?.message || 'Invalid input data',
        details: formattedErrors,
        statusCode: 400,
        timestamp: new Date().toISOString(),
      });
    }

    const statusCode = error.statusCode || (error.status ? Number(error.status) : 500);
    const message = statusCode === 500 ? 'Internal server error' : error.message;

    return reply.status(statusCode).send({
      success: false,
      error:
        statusCode === 404
          ? 'Not Found'
          : statusCode === 410
          ? 'Gone'
          : statusCode === 400
          ? 'Bad Request'
          : 'Error',
      message,
      statusCode,
      timestamp: new Date().toISOString(),
    });
  }
}
