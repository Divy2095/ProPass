import { FastifyReply, FastifyRequest } from 'fastify';
import { ZodError } from 'zod';
import { prisma } from '../config/prisma.js';
import { eventParamsSchema } from '../models/event.schema.js';
import { registrationParamsSchema } from '../models/registration.schema.js';
import { RegistrationService } from '../services/registration.service.js';

export class OrganizerController {
  /**
   * GET /api/v1/organizer/me
   * Returns authenticated organizer profile information.
   * Protected by authenticate + requireRole(UserRole.ORGANIZER).
   */
  static async getMe(req: FastifyRequest, reply: FastifyReply) {
    try {
      const userId = req.user!.userId;

      const user = await prisma.user.findUnique({
        where: { id: userId },
        select: {
          id: true,
          email: true,
          role: true,
          profile: {
            select: {
              fullName: true,
              title: true,
              organization: true,
              phone: true,
              linkedinUrl: true,
              isVerified: true,
            },
          },
          createdAt: true,
          updatedAt: true,
        },
      });

      if (!user) {
        return reply.status(404).send({
          success: false,
          error: 'Not Found',
          message: 'Organizer account not found',
          statusCode: 404,
          timestamp: new Date().toISOString(),
        });
      }

      return reply.status(200).send({
        success: true,
        data: {
          organizer: user,
        },
        timestamp: new Date().toISOString(),
      });
    } catch (error: any) {
      return OrganizerController.handleError(error, reply);
    }
  }

  /**
   * GET /api/v1/organizer/events/:eventId/registrations
   * Retrieves all registrations for an event owned by the authenticated organizer.
   */
  static async getEventRegistrations(req: FastifyRequest, reply: FastifyReply) {
    try {
      const { eventId } = eventParamsSchema.parse(req.params);
      const organizerId = req.user!.userId;
      const data = await RegistrationService.getEventRegistrationsForOrganizer(eventId, organizerId);

      return reply.status(200).send({
        success: true,
        data,
        timestamp: new Date().toISOString(),
      });
    } catch (error: any) {
      return OrganizerController.handleError(error, reply);
    }
  }

  /**
   * GET /api/v1/organizer/registrations/:registrationId
   * Retrieves detailed registration view for a single registration on an organizer-owned event.
   */
  static async getRegistrationDetails(req: FastifyRequest, reply: FastifyReply) {
    try {
      const { registrationId } = registrationParamsSchema.parse(req.params);
      const organizerId = req.user!.userId;
      const data = await RegistrationService.getRegistrationDetailForOrganizer(registrationId, organizerId);

      return reply.status(200).send({
        success: true,
        data,
        timestamp: new Date().toISOString(),
      });
    } catch (error: any) {
      return OrganizerController.handleError(error, reply);
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
        message: formattedErrors[0]?.message || 'Invalid parameters',
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
          : statusCode === 403
          ? 'Forbidden'
          : statusCode === 401
          ? 'Unauthorized'
          : statusCode === 400
          ? 'Bad Request'
          : 'Error',
      message,
      statusCode,
      timestamp: new Date().toISOString(),
    });
  }
}
