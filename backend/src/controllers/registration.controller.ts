import { FastifyReply, FastifyRequest } from 'fastify';
import { ZodError } from 'zod';
import { createRegistrationSchema } from '../models/registration.schema.js';
import { RegistrationService } from '../services/registration.service.js';

export class RegistrationController {
  /**
   * POST /api/v1/registrations
   */
  static async createRegistration(req: FastifyRequest, reply: FastifyReply) {
    try {
      const userId = req.user!.userId;
      const input = createRegistrationSchema.parse(req.body);
      const registration = await RegistrationService.createRegistration(userId, input);

      return reply.status(201).send({
        success: true,
        message: 'Registration confirmed successfully',
        data: {
          registration,
        },
        timestamp: new Date().toISOString(),
      });
    } catch (error: any) {
      return RegistrationController.handleError(error, reply);
    }
  }

  /**
   * GET /api/v1/registrations/my
   */
  static async getMyRegistrations(req: FastifyRequest, reply: FastifyReply) {
    try {
      const userId = req.user!.userId;
      const registrations = await RegistrationService.getMyRegistrations(userId);

      return reply.status(200).send({
        success: true,
        data: {
          registrations,
          count: registrations.length,
        },
        timestamp: new Date().toISOString(),
      });
    } catch (error: any) {
      return RegistrationController.handleError(error, reply);
    }
  }

  /**
   * GET /api/v1/registrations/:id
   */
  static async getMyRegistrationById(req: FastifyRequest, reply: FastifyReply) {
    try {
      const userId = req.user!.userId;
      const { id } = req.params as { id: string };
      const registration = await RegistrationService.getMyRegistrationById(userId, id);

      return reply.status(200).send({
        success: true,
        data: {
          registration,
        },
        timestamp: new Date().toISOString(),
      });
    } catch (error: any) {
      return RegistrationController.handleError(error, reply);
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
        message: formattedErrors[0]?.message || 'Invalid registration data',
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
        statusCode === 409
          ? 'Conflict'
          : statusCode === 404
          ? 'Not Found'
          : statusCode === 410
          ? 'Gone'
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
