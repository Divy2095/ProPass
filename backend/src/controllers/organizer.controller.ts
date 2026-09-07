import { FastifyReply, FastifyRequest } from 'fastify';
import { prisma } from '../config/prisma.js';

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
      return reply.status(500).send({
        success: false,
        error: 'Internal Server Error',
        message: error.message || 'Failed to retrieve organizer profile',
        statusCode: 500,
        timestamp: new Date().toISOString(),
      });
    }
  }
}
