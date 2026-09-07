import { FastifyReply, FastifyRequest } from 'fastify';
import { ZodError } from 'zod';
import { eventParamsSchema } from '../models/event.schema.js';
import { saveFormSchema } from '../models/form.schema.js';
import { FormService } from '../services/form.service.js';

export class FormController {
  /**
   * GET /api/v1/events/:eventId/form (Organizer only)
   */
  static async getForm(req: FastifyRequest, reply: FastifyReply) {
    try {
      const { eventId } = eventParamsSchema.parse(req.params);
      const organizerId = req.user!.userId;
      const form = await FormService.getFormByEventId(eventId, organizerId);

      return reply.status(200).send({
        success: true,
        data: {
          form,
        },
        timestamp: new Date().toISOString(),
      });
    } catch (error: any) {
      return FormController.handleError(error, reply);
    }
  }

  /**
   * PUT /api/v1/events/:eventId/form (Organizer only)
   */
  static async saveForm(req: FastifyRequest, reply: FastifyReply) {
    try {
      const { eventId } = eventParamsSchema.parse(req.params);
      const { questions } = saveFormSchema.parse(req.body);
      const organizerId = req.user!.userId;

      const form = await FormService.saveForm(eventId, organizerId, questions);

      return reply.status(200).send({
        success: true,
        message: 'Registration form saved successfully',
        data: {
          form,
        },
        timestamp: new Date().toISOString(),
      });
    } catch (error: any) {
      return FormController.handleError(error, reply);
    }
  }

  private static handleError(error: any, reply: FastifyReply) {
    if (error instanceof ZodError) {
      const formattedErrors = error.errors.map((e) => ({
        field: e.path.join('.'),
        message: e.message,
      }));

      return reply.status(400).send({
        success: false,
        error: 'Validation Error',
        message: formattedErrors[0]?.message || 'Invalid form data',
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
