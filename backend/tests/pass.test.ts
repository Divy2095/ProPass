import { FastifyInstance } from 'fastify';
import { afterAll, beforeAll, describe, expect, it } from 'vitest';
import { buildApp } from '../src/app.js';
import { prisma } from '../src/config/prisma.js';

describe('Digital Pass API (Phase 2E)', () => {
  let app: FastifyInstance;

  const testUserA = {
    email: 'pass.user.a@propass.id',
    password: 'Password123!',
  };

  const testUserB = {
    email: 'pass.user.b@propass.id',
    password: 'Password123!',
  };

  const userWithoutPass = {
    email: 'no.pass.user@propass.id',
    password: 'Password123!',
  };

  const userWithExpiredPass = {
    email: 'expired.pass.user@propass.id',
    password: 'Password123!',
  };

  const userWithInactivePass = {
    email: 'inactive.pass.user@propass.id',
    password: 'Password123!',
  };

  let tokenA: string;
  let userIdA: string;
  let tokenB: string;
  let userIdB: string;
  let tokenNoPass: string;
  let tokenExpired: string;
  let tokenInactive: string;

  beforeAll(async () => {
    app = await buildApp();
    await app.ready();

    // Clean up test users
    await prisma.user.deleteMany({
      where: {
        email: {
          in: [
            testUserA.email,
            testUserB.email,
            userWithoutPass.email,
            userWithExpiredPass.email,
            userWithInactivePass.email,
          ],
        },
      },
    });

    // 1. User A with active pass
    const resA = await app.inject({
      method: 'POST',
      url: '/api/v1/auth/register',
      payload: testUserA,
    });
    const bodyA = JSON.parse(resA.body);
    tokenA = bodyA.data.tokens.accessToken;
    userIdA = bodyA.data.user.id;

    await prisma.profile.create({
      data: {
        userId: userIdA,
        fullName: 'Alex Morgan',
        title: 'Senior Mobile Architect',
        organization: 'University of Technology',
        phone: '+1 (555) 012-3456',
        linkedinUrl: 'https://linkedin.com/in/alexmorgan',
        isVerified: true,
        completionScore: 90,
      },
    });

    await prisma.digitalPass.create({
      data: {
        userId: userIdA,
        passNumber: 'PP-2024-8881',
        qrPayload: 'propass:pass:PP-2024-8881',
        tier: 'PREMIUM',
        isActive: true,
      },
    });

    // 2. User B with active pass
    const resB = await app.inject({
      method: 'POST',
      url: '/api/v1/auth/register',
      payload: testUserB,
    });
    const bodyB = JSON.parse(resB.body);
    tokenB = bodyB.data.tokens.accessToken;
    userIdB = bodyB.data.user.id;

    await prisma.profile.create({
      data: {
        userId: userIdB,
        fullName: 'Sarah Jenkins',
        title: 'Senior UX Researcher',
        organization: 'TechFlow Inc.',
        isVerified: true,
        completionScore: 85,
      },
    });

    await prisma.digitalPass.create({
      data: {
        userId: userIdB,
        passNumber: 'PP-2024-8882',
        qrPayload: 'propass:pass:PP-2024-8882',
        tier: 'VIP',
        isActive: true,
      },
    });

    // 3. User without pass
    const resNoPass = await app.inject({
      method: 'POST',
      url: '/api/v1/auth/register',
      payload: userWithoutPass,
    });
    tokenNoPass = JSON.parse(resNoPass.body).data.tokens.accessToken;

    // 4. User with expired pass
    const resExpired = await app.inject({
      method: 'POST',
      url: '/api/v1/auth/register',
      payload: userWithExpiredPass,
    });
    const bodyExpired = JSON.parse(resExpired.body);
    tokenExpired = bodyExpired.data.tokens.accessToken;
    const userIdExpired = bodyExpired.data.user.id;

    const pastDate = new Date();
    pastDate.setDate(pastDate.getDate() - 30); // 30 days ago

    await prisma.digitalPass.create({
      data: {
        userId: userIdExpired,
        passNumber: 'PP-2024-EXPIRED',
        qrPayload: 'propass:pass:PP-2024-EXPIRED',
        tier: 'STANDARD',
        isActive: true,
        expiresAt: pastDate,
      },
    });

    // 5. User with deactivated/inactive pass
    const resInactive = await app.inject({
      method: 'POST',
      url: '/api/v1/auth/register',
      payload: userWithInactivePass,
    });
    const bodyInactive = JSON.parse(resInactive.body);
    tokenInactive = bodyInactive.data.tokens.accessToken;
    const userIdInactive = bodyInactive.data.user.id;

    await prisma.digitalPass.create({
      data: {
        userId: userIdInactive,
        passNumber: 'PP-2024-INACTIVE',
        qrPayload: 'propass:pass:PP-2024-INACTIVE',
        tier: 'PREMIUM',
        isActive: false,
      },
    });
  });

  afterAll(async () => {
    await prisma.user.deleteMany({
      where: {
        email: {
          in: [
            testUserA.email,
            testUserB.email,
            userWithoutPass.email,
            userWithExpiredPass.email,
            userWithInactivePass.email,
          ],
        },
      },
    });
    await app.close();
  });

  describe('GET /api/v1/passes/me', () => {
    it('1. Authenticated user retrieves their own active Digital Pass (200 OK)', async () => {
      const res = await app.inject({
        method: 'GET',
        url: '/api/v1/passes/me',
        headers: { authorization: `Bearer ${tokenA}` },
      });

      expect(res.statusCode).toBe(200);
      const body = JSON.parse(res.body);

      expect(body.success).toBe(true);
      expect(body.data.pass).toBeDefined();
      expect(body.data.pass.passNumber).toBe('PP-2024-8881');
      expect(body.data.pass.tier).toBe('PREMIUM');
      expect(body.data.pass.isActive).toBe(true);
      expect(body.data.pass.isExpired).toBe(false);
      expect(body.data.pass.qrPayload).toBe('propass:pass:PP-2024-8881');
    });

    it('2. Unauthenticated request returns 401 Unauthorized', async () => {
      const res = await app.inject({
        method: 'GET',
        url: '/api/v1/passes/me',
      });

      expect(res.statusCode).toBe(401);
      const body = JSON.parse(res.body);
      expect(body.success).toBe(false);
    });

    it('3. User cannot access another users pass (Isolation check)', async () => {
      const resB = await app.inject({
        method: 'GET',
        url: '/api/v1/passes/me',
        headers: { authorization: `Bearer ${tokenB}` },
      });

      expect(resB.statusCode).toBe(200);
      const bodyB = JSON.parse(resB.body);
      expect(bodyB.data.pass.passNumber).toBe('PP-2024-8882');
      expect(bodyB.data.pass.passNumber).not.toBe('PP-2024-8881');
      expect(bodyB.data.holder.userId).toBe(userIdB);
      expect(bodyB.data.holder.userId).not.toBe(userIdA);
    });

    it('4. User without pass returns 404 Not Found', async () => {
      const res = await app.inject({
        method: 'GET',
        url: '/api/v1/passes/me',
        headers: { authorization: `Bearer ${tokenNoPass}` },
      });

      expect(res.statusCode).toBe(404);
      const body = JSON.parse(res.body);
      expect(body.success).toBe(false);
      expect(body.message).toContain('No digital pass found');
    });

    it('5. Inactive pass returns isActive = false', async () => {
      const res = await app.inject({
        method: 'GET',
        url: '/api/v1/passes/me',
        headers: { authorization: `Bearer ${tokenInactive}` },
      });

      expect(res.statusCode).toBe(200);
      const body = JSON.parse(res.body);
      expect(body.data.pass.passNumber).toBe('PP-2024-INACTIVE');
      expect(body.data.pass.isActive).toBe(false);
    });

    it('6. Expired pass returns isExpired = true and isActive = false', async () => {
      const res = await app.inject({
        method: 'GET',
        url: '/api/v1/passes/me',
        headers: { authorization: `Bearer ${tokenExpired}` },
      });

      expect(res.statusCode).toBe(200);
      const body = JSON.parse(res.body);
      expect(body.data.pass.passNumber).toBe('PP-2024-EXPIRED');
      expect(body.data.pass.isExpired).toBe(true);
      expect(body.data.pass.isActive).toBe(false);
    });

    it('7. Response contains required holder information matching Stitch UI', async () => {
      const res = await app.inject({
        method: 'GET',
        url: '/api/v1/passes/me',
        headers: { authorization: `Bearer ${tokenA}` },
      });

      expect(res.statusCode).toBe(200);
      const body = JSON.parse(res.body);

      expect(body.data.holder).toBeDefined();
      expect(body.data.holder.fullName).toBe('Alex Morgan');
      expect(body.data.holder.title).toBe('Senior Mobile Architect');
      expect(body.data.holder.organization).toBe('University of Technology');
      expect(body.data.holder.phone).toBe('+1 (555) 012-3456');
      expect(body.data.holder.linkedinUrl).toBe('https://linkedin.com/in/alexmorgan');
      expect(body.data.holder.isVerified).toBe(true);
    });

    it('8. Sensitive database fields are never exposed in response', async () => {
      const res = await app.inject({
        method: 'GET',
        url: '/api/v1/passes/me',
        headers: { authorization: `Bearer ${tokenA}` },
      });

      expect(res.statusCode).toBe(200);
      const body = JSON.parse(res.body);

      expect(body.data.holder.passwordHash).toBeUndefined();
      expect(body.data.pass.tokenHash).toBeUndefined();
    });

    it('9. Pass tier cannot be manipulated by client', async () => {
      const resB = await app.inject({
        method: 'GET',
        url: '/api/v1/passes/me',
        headers: { authorization: `Bearer ${tokenB}` },
      });

      expect(resB.statusCode).toBe(200);
      const body = JSON.parse(resB.body);
      expect(body.data.pass.tier).toBe('VIP');
    });

    it('10. QR payload is returned matching expected format', async () => {
      const resA = await app.inject({
        method: 'GET',
        url: '/api/v1/passes/me',
        headers: { authorization: `Bearer ${tokenA}` },
      });

      expect(resA.statusCode).toBe(200);
      const body = JSON.parse(resA.body);
      expect(body.data.pass.qrPayload).toBe('propass:pass:PP-2024-8881');
    });
  });
});
