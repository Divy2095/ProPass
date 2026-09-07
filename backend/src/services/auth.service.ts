import { UserRole } from '@prisma/client';
import { env } from '../config/env.js';
import { prisma } from '../config/prisma.js';
import { LoginInput, RegisterInput } from '../models/auth.schema.js';
import { generateSecureToken, hashPassword, hashToken, verifyPassword } from '../utils/crypto.js';
import { generateAccessToken } from '../utils/jwt.js';

export interface AuthResponse {
  user: {
    id: string;
    email: string;
    role: UserRole;
    createdAt: Date;
  };
  tokens: {
    accessToken: string;
    refreshToken: string;
    expiresIn: string;
  };
}

export interface RefreshResponse {
  tokens: {
    accessToken: string;
    refreshToken: string;
    expiresIn: string;
  };
}

export class AuthService {
  /**
   * Registers a new user with email and hashed password.
   * Normal user registration always creates ATTENDEE role.
   */
  static async register(input: RegisterInput): Promise<AuthResponse> {
    const normalizedEmail = input.email.trim().toLowerCase();

    // Check for existing user
    const existingUser = await prisma.user.findUnique({
      where: { email: normalizedEmail },
    });

    if (existingUser) {
      const error: any = new Error('An account with this email address already exists');
      error.statusCode = 409;
      throw error;
    }

    // Hash password with Argon2
    const passwordHash = await hashPassword(input.password);

    // Create user in PostgreSQL with forced ATTENDEE role
    const user = await prisma.user.create({
      data: {
        email: normalizedEmail,
        passwordHash,
        role: UserRole.ATTENDEE,
      },
      select: {
        id: true,
        email: true,
        role: true,
        createdAt: true,
      },
    });

    // Generate JWT access token & secure refresh token
    const accessToken = generateAccessToken({
      userId: user.id,
      email: user.email,
      role: user.role,
    });

    const rawRefreshToken = generateSecureToken();
    const tokenHash = hashToken(rawRefreshToken);
    const expiresAt = new Date();
    expiresAt.setDate(expiresAt.getDate() + env.REFRESH_TOKEN_EXPIRES_DAYS);

    // Store hashed refresh token in database
    await prisma.refreshToken.create({
      data: {
        userId: user.id,
        tokenHash,
        expiresAt,
      },
    });

    return {
      user,
      tokens: {
        accessToken,
        refreshToken: rawRefreshToken,
        expiresIn: env.JWT_EXPIRES_IN,
      },
    };
  }

  /**
   * Authenticates user credentials and returns tokens.
   */
  static async login(input: LoginInput): Promise<AuthResponse> {
    const normalizedEmail = input.email.trim().toLowerCase();

    const user = await prisma.user.findUnique({
      where: { email: normalizedEmail },
      select: {
        id: true,
        email: true,
        passwordHash: true,
        role: true,
        createdAt: true,
      },
    });

    if (!user || !user.passwordHash) {
      const error: any = new Error('Invalid email or password');
      error.statusCode = 401;
      throw error;
    }

    const isValidPassword = await verifyPassword(input.password, user.passwordHash);
    if (!isValidPassword) {
      const error: any = new Error('Invalid email or password');
      error.statusCode = 401;
      throw error;
    }

    // Generate tokens
    const accessToken = generateAccessToken({
      userId: user.id,
      email: user.email,
      role: user.role,
    });

    const rawRefreshToken = generateSecureToken();
    const tokenHash = hashToken(rawRefreshToken);
    const expiresAt = new Date();
    expiresAt.setDate(expiresAt.getDate() + env.REFRESH_TOKEN_EXPIRES_DAYS);

    await prisma.refreshToken.create({
      data: {
        userId: user.id,
        tokenHash,
        expiresAt,
      },
    });

    return {
      user: {
        id: user.id,
        email: user.email,
        role: user.role,
        createdAt: user.createdAt,
      },
      tokens: {
        accessToken,
        refreshToken: rawRefreshToken,
        expiresIn: env.JWT_EXPIRES_IN,
      },
    };
  }

  /**
   * Rotates an active refresh token and returns a new token pair.
   */
  static async refresh(rawRefreshToken: string): Promise<RefreshResponse> {
    const tokenHash = hashToken(rawRefreshToken);

    const tokenRecord = await prisma.refreshToken.findUnique({
      where: { tokenHash },
      include: {
        user: {
          select: {
            id: true,
            email: true,
            role: true,
          },
        },
      },
    });

    if (!tokenRecord) {
      const error: any = new Error('Invalid or revoked refresh token');
      error.statusCode = 401;
      throw error;
    }

    // Check expiration
    if (tokenRecord.expiresAt < new Date()) {
      await prisma.refreshToken.delete({
        where: { id: tokenRecord.id },
      });
      const error: any = new Error('Refresh token has expired');
      error.statusCode = 401;
      throw error;
    }

    // Token rotation: delete old refresh token
    await prisma.refreshToken.delete({
      where: { id: tokenRecord.id },
    });

    // Generate new token pair
    const accessToken = generateAccessToken({
      userId: tokenRecord.user.id,
      email: tokenRecord.user.email,
      role: tokenRecord.user.role,
    });

    const newRawRefreshToken = generateSecureToken();
    const newTokenHash = hashToken(newRawRefreshToken);
    const expiresAt = new Date();
    expiresAt.setDate(expiresAt.getDate() + env.REFRESH_TOKEN_EXPIRES_DAYS);

    await prisma.refreshToken.create({
      data: {
        userId: tokenRecord.user.id,
        tokenHash: newTokenHash,
        expiresAt,
      },
    });

    return {
      tokens: {
        accessToken,
        refreshToken: newRawRefreshToken,
        expiresIn: env.JWT_EXPIRES_IN,
      },
    };
  }

  /**
   * Revokes a refresh token so it cannot be reused.
   */
  static async logout(rawRefreshToken: string): Promise<void> {
    const tokenHash = hashToken(rawRefreshToken);

    await prisma.refreshToken.deleteMany({
      where: { tokenHash },
    });
  }

  /**
   * Retrieves a user by ID without sensitive password/hash fields.
   */
  static async getUserById(userId: string) {
    return prisma.user.findUnique({
      where: { id: userId },
      select: {
        id: true,
        email: true,
        role: true,
        createdAt: true,
        updatedAt: true,
      },
    });
  }
}
