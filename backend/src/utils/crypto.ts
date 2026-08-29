import argon2 from 'argon2';
import crypto from 'crypto';

/**
 * Hashes a plaintext password using Argon2id with secure defaults.
 */
export async function hashPassword(password: string): Promise<string> {
  return argon2.hash(password, {
    type: argon2.argon2id,
    memoryCost: 65536, // 64 MB
    timeCost: 3,       // 3 iterations
    parallelism: 4,
  });
}

/**
 * Verifies a plaintext password against an Argon2 hash.
 */
export async function verifyPassword(password: string, hash: string): Promise<boolean> {
  try {
    return await argon2.verify(hash, password);
  } catch (error) {
    return false;
  }
}

/**
 * Generates a high-entropy random token string (80 hex characters).
 */
export function generateSecureToken(): string {
  return crypto.randomBytes(40).toString('hex');
}

/**
 * Computes a SHA-256 hash of a token for secure database storage.
 */
export function hashToken(token: string): string {
  return crypto.createHash('sha256').update(token).digest('hex');
}
