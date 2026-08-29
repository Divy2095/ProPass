import { afterAll, beforeAll, describe, expect, it } from 'vitest';
import { FastifyInstance } from 'fastify';
import { buildApp } from '../src/app.js';

describe('GET /health API Endpoint', () => {
  let app: FastifyInstance;

  beforeAll(async () => {
    app = await buildApp();
    await app.ready();
  });

  afterAll(async () => {
    await app.close();
  });

  it('should respond to GET / with application metadata', async () => {
    const response = await app.inject({
      method: 'GET',
      url: '/',
    });

    expect(response.statusCode).toBe(200);
    const body = JSON.parse(response.body);
    expect(body.name).toBe('ProPass Digital Identity System API');
    expect(body.status).toBe('operational');
  });

  it('should respond to GET /health with service status', async () => {
    const response = await app.inject({
      method: 'GET',
      url: '/health',
    });

    expect([200, 503]).toContain(response.statusCode);
    const body = JSON.parse(response.body);
    expect(body.service).toBe('propass-backend');
    expect(body.version).toBe('1.0.0');
    expect(body.timestamp).toBeDefined();
  });
});
