import { FastifyInstance } from 'fastify';
import jwt from 'jsonwebtoken';
import { afterAll, beforeAll, describe, expect, it } from 'vitest';
import { buildApp } from '../src/app.js';
import { env } from '../src/config/env.js';
import { prisma } from '../src/config/prisma.js';
import { verifyPassword } from '../src/utils/crypto.js';

describe('Authentication API (Phase 2A)', () => {
  let app: FastifyInstance;

  const testUser = {
    email: 'auth.test.user@propass.id',
    password: 'Password123!',
  };

  beforeAll(async () => {
    app = await buildApp();
    await app.ready();

    // Clean up test data if present
    await prisma.user.deleteMany({
      where: { email: testUser.email },
    });
  });

  afterAll(async () => {
    // Clean up test user and close app
    await prisma.user.deleteMany({
      where: { email: testUser.email },
    });
    await app.close();
  });

  describe('1. User Registration (POST /api/v1/auth/register)', () => {
    it('should successfully register a new user and return tokens', async () => {
      const res = await app.inject({
        method: 'POST',
        url: '/api/v1/auth/register',
        payload: {
          email: testUser.email,
          password: testUser.password,
        },
      });

      expect(res.statusCode).toBe(201);
      const body = JSON.parse(res.body);

      expect(body.success).toBe(true);
      expect(body.data.user.email).toBe(testUser.email);
      expect(body.data.user.id).toBeDefined();
      expect(body.data.user.passwordHash).toBeUndefined(); // Security check
      expect(body.data.tokens.accessToken).toBeDefined();
      expect(body.data.tokens.refreshToken).toBeDefined();
      expect(body.data.tokens.expiresIn).toBe(env.JWT_EXPIRES_IN);

      // Verify user was persisted in database with Argon2 password hash
      const dbUser = await prisma.user.findUnique({
        where: { email: testUser.email },
      });
      expect(dbUser).not.toBeNull();
      expect(dbUser?.passwordHash).toBeDefined();
      expect(dbUser?.passwordHash?.startsWith('$argon2')).toBe(true);
      const isArgon2Valid = await verifyPassword(testUser.password, dbUser!.passwordHash!);
      expect(isArgon2Valid).toBe(true);
    });

    it('should reject registration with duplicate email (409 Conflict)', async () => {
      const res = await app.inject({
        method: 'POST',
        url: '/api/v1/auth/register',
        payload: {
          email: testUser.email,
          password: 'AnotherPassword123!',
        },
      });

      expect(res.statusCode).toBe(409);
      const body = JSON.parse(res.body);
      expect(body.success).toBe(false);
      expect(body.error).toBe('Conflict');
    });

    it('should reject invalid email format (400 Bad Request)', async () => {
      const res = await app.inject({
        method: 'POST',
        url: '/api/v1/auth/register',
        payload: {
          email: 'not-an-email',
          password: 'ValidPassword123!',
        },
      });

      expect(res.statusCode).toBe(400);
      const body = JSON.parse(res.body);
      expect(body.success).toBe(false);
      expect(body.error).toBe('Validation Error');
    });

    it('should reject passwords shorter than 8 characters (400 Bad Request)', async () => {
      const res = await app.inject({
        method: 'POST',
        url: '/api/v1/auth/register',
        payload: {
          email: 'shortpass@example.com',
          password: 'short',
        },
      });

      expect(res.statusCode).toBe(400);
      const body = JSON.parse(res.body);
      expect(body.success).toBe(false);
      expect(body.error).toBe('Validation Error');
    });
  });

  describe('2. User Login (POST /api/v1/auth/login)', () => {
    it('should successfully log in with valid credentials', async () => {
      const res = await app.inject({
        method: 'POST',
        url: '/api/v1/auth/login',
        payload: {
          email: testUser.email,
          password: testUser.password,
        },
      });

      expect(res.statusCode).toBe(200);
      const body = JSON.parse(res.body);

      expect(body.success).toBe(true);
      expect(body.data.user.email).toBe(testUser.email);
      expect(body.data.user.passwordHash).toBeUndefined(); // Security check
      expect(body.data.tokens.accessToken).toBeDefined();
      expect(body.data.tokens.refreshToken).toBeDefined();
    });

    it('should reject login with incorrect password (401 Unauthorized)', async () => {
      const res = await app.inject({
        method: 'POST',
        url: '/api/v1/auth/login',
        payload: {
          email: testUser.email,
          password: 'WrongPassword999!',
        },
      });

      expect(res.statusCode).toBe(401);
      const body = JSON.parse(res.body);
      expect(body.success).toBe(false);
      expect(body.error).toBe('Unauthorized');
      expect(body.message).toBe('Invalid email or password');
    });

    it('should reject login with non-existent email (401 Unauthorized)', async () => {
      const res = await app.inject({
        method: 'POST',
        url: '/api/v1/auth/login',
        payload: {
          email: 'nonexistent.user@propass.id',
          password: 'SomePassword123!',
        },
      });

      expect(res.statusCode).toBe(401);
      const body = JSON.parse(res.body);
      expect(body.success).toBe(false);
      expect(body.message).toBe('Invalid email or password');
    });
  });

  describe('3. Protected Route & Auth Middleware (GET /api/v1/auth/me)', () => {
    let validAccessToken: string;

    beforeAll(async () => {
      const loginRes = await app.inject({
        method: 'POST',
        url: '/api/v1/auth/login',
        payload: testUser,
      });
      const body = JSON.parse(loginRes.body);
      validAccessToken = body.data.tokens.accessToken;
    });

    it('should allow access with a valid JWT access token', async () => {
      const res = await app.inject({
        method: 'GET',
        url: '/api/v1/auth/me',
        headers: {
          authorization: `Bearer ${validAccessToken}`,
        },
      });

      expect(res.statusCode).toBe(200);
      const body = JSON.parse(res.body);
      expect(body.success).toBe(true);
      expect(body.data.user.email).toBe(testUser.email);
    });

    it('should reject request without Authorization header (401 Unauthorized)', async () => {
      const res = await app.inject({
        method: 'GET',
        url: '/api/v1/auth/me',
      });

      expect(res.statusCode).toBe(401);
      const body = JSON.parse(res.body);
      expect(body.success).toBe(false);
    });

    it('should reject request with invalid JWT token (401 Unauthorized)', async () => {
      const res = await app.inject({
        method: 'GET',
        url: '/api/v1/auth/me',
        headers: {
          authorization: 'Bearer invalid.token.payload',
        },
      });

      expect(res.statusCode).toBe(401);
      const body = JSON.parse(res.body);
      expect(body.success).toBe(false);
    });

    it('should reject request with expired JWT token (401 Unauthorized)', async () => {
      // Create an immediately expired token with same secret
      const expiredToken = jwt.sign(
        { userId: 'test-user-id', email: testUser.email, role: 'USER', type: 'access' },
        env.JWT_SECRET,
        { expiresIn: '-10s' }
      );

      const res = await app.inject({
        method: 'GET',
        url: '/api/v1/auth/me',
        headers: {
          authorization: `Bearer ${expiredToken}`,
        },
      });

      expect(res.statusCode).toBe(401);
      const body = JSON.parse(res.body);
      expect(body.message).toContain('expired');
    });
  });

  describe('4. Token Refresh & Rotation (POST /api/v1/auth/refresh)', () => {
    it('should rotate refresh token and issue new token pair', async () => {
      // Log in to get active refresh token
      const loginRes = await app.inject({
        method: 'POST',
        url: '/api/v1/auth/login',
        payload: testUser,
      });
      const initialTokens = JSON.parse(loginRes.body).data.tokens;

      // Refresh session
      const refreshRes = await app.inject({
        method: 'POST',
        url: '/api/v1/auth/refresh',
        payload: {
          refreshToken: initialTokens.refreshToken,
        },
      });

      expect(refreshRes.statusCode).toBe(200);
      const refreshBody = JSON.parse(refreshRes.body);
      expect(refreshBody.success).toBe(true);
      expect(refreshBody.data.tokens.accessToken).toBeDefined();
      expect(refreshBody.data.tokens.refreshToken).toBeDefined();
      expect(refreshBody.data.tokens.refreshToken).not.toBe(initialTokens.refreshToken);

      // Verify the OLD refresh token is revoked and cannot be reused
      const reuseRes = await app.inject({
        method: 'POST',
        url: '/api/v1/auth/refresh',
        payload: {
          refreshToken: initialTokens.refreshToken,
        },
      });

      expect(reuseRes.statusCode).toBe(401);
      const reuseBody = JSON.parse(reuseRes.body);
      expect(reuseBody.success).toBe(false);
      expect(reuseBody.message).toContain('revoked');
    });

    it('should reject unknown or malformed refresh token (401 Unauthorized)', async () => {
      const res = await app.inject({
        method: 'POST',
        url: '/api/v1/auth/refresh',
        payload: {
          refreshToken: 'completely-unknown-token-value-123456789',
        },
      });

      expect(res.statusCode).toBe(401);
      const body = JSON.parse(res.body);
      expect(body.success).toBe(false);
    });
  });

  describe('5. User Logout (POST /api/v1/auth/logout)', () => {
    it('should revoke refresh token so it cannot be refreshed after logout', async () => {
      const loginRes = await app.inject({
        method: 'POST',
        url: '/api/v1/auth/login',
        payload: testUser,
      });
      const tokens = JSON.parse(loginRes.body).data.tokens;

      // Log out
      const logoutRes = await app.inject({
        method: 'POST',
        url: '/api/v1/auth/logout',
        payload: {
          refreshToken: tokens.refreshToken,
        },
      });

      expect(logoutRes.statusCode).toBe(200);
      const logoutBody = JSON.parse(logoutRes.body);
      expect(logoutBody.success).toBe(true);

      // Attempt to refresh using logged out token -> must fail with 401
      const refreshAfterLogoutRes = await app.inject({
        method: 'POST',
        url: '/api/v1/auth/refresh',
        payload: {
          refreshToken: tokens.refreshToken,
        },
      });

      expect(refreshAfterLogoutRes.statusCode).toBe(401);
    });
  });
});
