import { prisma } from '../config/prisma.js';
import { MyPassResponse } from '../models/pass.schema.js';

export class PassService {
  /**
   * Retrieves the authenticated user's digital pass and holder profile.
   */
  static async getMyDigitalPass(userId: string): Promise<MyPassResponse> {
    const digitalPass = await prisma.digitalPass.findUnique({
      where: { userId },
      include: {
        user: {
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
                avatarUrl: true,
                isVerified: true,
              },
            },
          },
        },
      },
    });

    if (!digitalPass) {
      const error: any = new Error('No digital pass found for this user account');
      error.statusCode = 404;
      throw error;
    }

    const now = new Date();
    const isExpired = Boolean(digitalPass.expiresAt && digitalPass.expiresAt < now);
    const effectiveIsActive = digitalPass.isActive && !isExpired;

    return {
      pass: {
        id: digitalPass.id,
        passNumber: digitalPass.passNumber,
        tier: digitalPass.tier,
        isActive: effectiveIsActive,
        isExpired,
        qrPayload: digitalPass.qrPayload,
        expiresAt: digitalPass.expiresAt,
        createdAt: digitalPass.createdAt,
        updatedAt: digitalPass.updatedAt,
      },
      holder: {
        userId: digitalPass.user.id,
        email: digitalPass.user.email,
        fullName: digitalPass.user.profile?.fullName || 'ProPass Holder',
        title: digitalPass.user.profile?.title || null,
        organization: digitalPass.user.profile?.organization || null,
        phone: digitalPass.user.profile?.phone || null,
        linkedinUrl: digitalPass.user.profile?.linkedinUrl || null,
        avatarUrl: digitalPass.user.profile?.avatarUrl || null,
        isVerified: digitalPass.user.profile?.isVerified ?? false,
      },
    };
  }
}
