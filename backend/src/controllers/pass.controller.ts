import { FastifyReply, FastifyRequest } from 'fastify';
import { PassService } from '../services/pass.service.js';

export class PassController {
  /**
   * GET /api/v1/passes/me
   */
  static async getMyPass(req: FastifyRequest, reply: FastifyReply) {
    try {
      const userId = req.user!.userId;
      const result = await PassService.getMyDigitalPass(userId);

      return reply.status(200).send({
        success: true,
        data: result,
        timestamp: new Date().toISOString(),
      });
    } catch (error: any) {
      return PassController.handleError(error, reply);
    }
  }

  /**
   * Standard error handler
   */
  private static handleError(error: any, reply: FastifyReply) {
    const statusCode = error.statusCode || (error.status ? Number(error.status) : 500);
    const message = statusCode === 500 ? 'Internal server error' : error.message;

    return reply.status(statusCode).send({
      success: false,
      error:
        statusCode === 404
          ? 'Not Found'
          : statusCode === 401
          ? 'Unauthorized'
          : 'Error',
      message,
      statusCode,
      timestamp: new Date().toISOString(),
    });
  }
}
