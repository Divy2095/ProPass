import { FastifyReply, FastifyRequest } from 'fastify';
import { ZodError } from 'zod';
import {
  loginSchema,
  logoutSchema,
  refreshTokenSchema,
  registerSchema,
} from '../models/auth.schema.js';
import { AuthService } from '../services/auth.service.js';

export class AuthController {
  /**
   * POST /api/v1/auth/register
   */
  static async register(req: FastifyRequest, reply: FastifyReply) {
    try {
      const input = registerSchema.parse(req.body);
      const result = await AuthService.register(input);

      return reply.status(201).send({
        success: true,
        message: 'Account registered successfully',
        data: result,
        timestamp: new Date().toISOString(),
      });
    } catch (error: any) {
      return AuthController.handleError(error, reply);
    }
  }

  /**
   * POST /api/v1/auth/login
   */
  static async login(req: FastifyRequest, reply: FastifyReply) {
    try {
      const input = loginSchema.parse(req.body);
      const result = await AuthService.login(input);

      return reply.status(200).send({
        success: true,
        message: 'Login successful',
        data: result,
        timestamp: new Date().toISOString(),
      });
    } catch (error: any) {
      return AuthController.handleError(error, reply);
    }
  }

  /**
   * POST /api/v1/auth/refresh
   */
  static async refresh(req: FastifyRequest, reply: FastifyReply) {
    try {
      const input = refreshTokenSchema.parse(req.body);
      const result = await AuthService.refresh(input.refreshToken);

      return reply.status(200).send({
        success: true,
        message: 'Token refreshed successfully',
        data: result,
        timestamp: new Date().toISOString(),
      });
    } catch (error: any) {
      return AuthController.handleError(error, reply);
    }
  }

  /**
   * POST /api/v1/auth/logout
   */
  static async logout(req: FastifyRequest, reply: FastifyReply) {
    try {
      const input = logoutSchema.parse(req.body);
      await AuthService.logout(input.refreshToken);

      return reply.status(200).send({
        success: true,
        message: 'Logged out successfully',
        timestamp: new Date().toISOString(),
      });
    } catch (error: any) {
      return AuthController.handleError(error, reply);
    }
  }

  /**
   * GET /api/v1/auth/me (Protected route)
   */
  static async getMe(req: FastifyRequest, reply: FastifyReply) {
    try {
      if (!req.user) {
        return reply.status(401).send({
          success: false,
          error: 'Unauthorized',
          message: 'User authentication required',
          statusCode: 401,
          timestamp: new Date().toISOString(),
        });
      }

      const user = await AuthService.getUserById(req.user.userId);
      if (!user) {
        return reply.status(404).send({
          success: false,
          error: 'Not Found',
          message: 'User account not found',
          statusCode: 404,
          timestamp: new Date().toISOString(),
        });
      }

      return reply.status(200).send({
        success: true,
        data: {
          user,
        },
        timestamp: new Date().toISOString(),
      });
    } catch (error: any) {
      return AuthController.handleError(error, reply);
    }
  }

  /**
   * Consistent error response handler
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
      error: statusCode === 401 ? 'Unauthorized' : statusCode === 409 ? 'Conflict' : 'Error',
      message,
      statusCode,
      timestamp: new Date().toISOString(),
    });
  }
}
