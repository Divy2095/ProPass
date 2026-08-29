import jwt, { SignOptions } from 'jsonwebtoken';
import { Role } from '@prisma/client';
import { env } from '../config/env.js';

export interface JwtUserPayload {
  userId: string;
  email: string;
  role: Role;
  type: 'access';
  iat?: number;
  exp?: number;
}

/**
 * Generates a signed JWT access token.
 */
export function generateAccessToken(payload: { userId: string; email: string; role: Role }): string {
  const tokenPayload: Omit<JwtUserPayload, 'iat' | 'exp'> = {
    userId: payload.userId,
    email: payload.email,
    role: payload.role,
    type: 'access',
  };

  const options: SignOptions = {
    expiresIn: env.JWT_EXPIRES_IN as any,
  };

  return jwt.sign(tokenPayload, env.JWT_SECRET, options);
}

/**
 * Verifies and decodes a JWT access token.
 * Throws JsonWebTokenError or TokenExpiredError if invalid or expired.
 */
export function verifyAccessToken(token: string): JwtUserPayload {
  const decoded = jwt.verify(token, env.JWT_SECRET) as JwtUserPayload;

  if (decoded.type !== 'access') {
    throw new jwt.JsonWebTokenError('Invalid token type');
  }

  return decoded;
}
