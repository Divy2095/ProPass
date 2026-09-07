import { FastifyReply, FastifyRequest } from 'fastify';
import { UserRole } from '@prisma/client';

/**
 * Fastify preHandler hook factory that enforces specific user roles.
 *
 * - Unauthenticated (no request.user) -> 401 Unauthorized
 * - Authenticated with wrong role -> 403 Forbidden
 * - Correct role -> proceed
 */
export function requireRole(...allowedRoles: UserRole[]) {
  return async (request: FastifyRequest, reply: FastifyReply) => {
    if (!request.user) {
      return reply.status(401).send({
        success: false,
        error: 'Unauthorized',
        message: 'Authentication required',
        statusCode: 401,
        timestamp: new Date().toISOString(),
      });
    }

    if (!allowedRoles.includes(request.user.role)) {
      return reply.status(403).send({
        success: false,
        error: 'Forbidden',
        message: 'Organizer access required.',
        statusCode: 403,
        timestamp: new Date().toISOString(),
      });
    }
  };
}
