import { FastifyReply, FastifyRequest } from 'fastify';
import { ZodError } from 'zod';
import { updateProfileSchema } from '../models/user.schema.js';
import { UserService } from '../services/user.service.js';

export class UserController {
  /**
   * GET /api/v1/users/profile
   */
  static async getProfile(req: FastifyRequest, reply: FastifyReply) {
    try {
      const userId = req.user!.userId;
      const result = await UserService.getProfile(userId);

      return reply.status(200).send({
        success: true,
        data: result,
        timestamp: new Date().toISOString(),
      });
    } catch (error: any) {
      return UserController.handleError(error, reply);
    }
  }

  /**
   * PUT /api/v1/users/profile
   */
  static async updateProfile(req: FastifyRequest, reply: FastifyReply) {
    try {
      const userId = req.user!.userId;
      const input = updateProfileSchema.parse(req.body);
      const result = await UserService.updateProfile(userId, input);

      return reply.status(200).send({
        success: true,
        message: 'Profile updated successfully',
        data: result,
        timestamp: new Date().toISOString(),
      });
    } catch (error: any) {
      return UserController.handleError(error, reply);
    }
  }

  /**
   * GET /api/v1/dashboard
   */
  static async getDashboard(req: FastifyRequest, reply: FastifyReply) {
    try {
      const userId = req.user!.userId;
      const result = await UserService.getDashboard(userId);

      return reply.status(200).send({
        success: true,
        data: result,
        timestamp: new Date().toISOString(),
      });
    } catch (error: any) {
      return UserController.handleError(error, reply);
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
        message: formattedErrors[0]?.message || 'Invalid profile input data',
        details: formattedErrors,
        statusCode: 400,
        timestamp: new Date().toISOString(),
      });
    }

    const statusCode = error.statusCode || (error.status ? Number(error.status) : 500);
    const message = statusCode === 500 ? 'Internal server error' : error.message;

    return reply.status(statusCode).send({
      success: false,
      error: statusCode === 404 ? 'Not Found' : statusCode === 401 ? 'Unauthorized' : 'Error',
      message,
      statusCode,
      timestamp: new Date().toISOString(),
    });
  }
}
