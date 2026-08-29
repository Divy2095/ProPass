import { FastifyReply, FastifyRequest } from 'fastify';
import { JwtUserPayload, verifyAccessToken } from '../utils/jwt.js';

declare module 'fastify' {
  interface FastifyRequest {
    user?: JwtUserPayload;
  }
}

/**
 * Fastify preHandler hook to authenticate requests via JWT Bearer tokens.
 */
export async function authenticate(request: FastifyRequest, reply: FastifyReply) {
  const authHeader = request.headers.authorization;

  if (!authHeader) {
    return reply.status(401).send({
      success: false,
      error: 'Unauthorized',
      message: 'Authorization header is required (Bearer <token>)',
      statusCode: 401,
      timestamp: new Date().toISOString(),
    });
  }

  const [scheme, token] = authHeader.split(' ');

  if (scheme !== 'Bearer' || !token) {
    return reply.status(401).send({
      success: false,
      error: 'Unauthorized',
      message: 'Malformed authorization header. Expected "Bearer <token>"',
      statusCode: 401,
      timestamp: new Date().toISOString(),
    });
  }

  try {
    const userPayload = verifyAccessToken(token);
    request.user = userPayload;
  } catch (error: any) {
    const message = error.name === 'TokenExpiredError' 
      ? 'Access token has expired' 
      : 'Invalid or malformed access token';

    return reply.status(401).send({
      success: false,
      error: 'Unauthorized',
      message,
      statusCode: 401,
      timestamp: new Date().toISOString(),
    });
  }
}
