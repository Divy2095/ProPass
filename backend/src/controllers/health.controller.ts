import { FastifyReply, FastifyRequest } from 'fastify';
import { prisma } from '../config/prisma.js';

export async function getHealthHandler(_req: FastifyRequest, reply: FastifyReply) {
  let dbStatus = 'disconnected';
  try {
    await prisma.$queryRaw`SELECT 1`;
    dbStatus = 'connected';
  } catch (error) {
    dbStatus = 'unreachable';
  }

  const isHealthy = dbStatus === 'connected';

  return reply.status(isHealthy ? 200 : 503).send({
    status: isHealthy ? 'ok' : 'degraded',
    service: 'propass-backend',
    version: '1.0.0',
    timestamp: new Date().toISOString(),
    database: dbStatus,
  });
}
