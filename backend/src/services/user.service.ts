import { prisma } from '../config/prisma.js';
import { UpdateProfileInput } from '../models/user.schema.js';

export class UserService {
  /**
   * Computes profile completion score (0 - 100%) based on filled fields.
   */
  static calculateCompletionScore(profile: {
    fullName?: string | null;
    title?: string | null;
    organization?: string | null;
    phone?: string | null;
    linkedinUrl?: string | null;
    avatarUrl?: string | null;
  }): number {
    let score = 0;
    if (profile.fullName && profile.fullName.trim().length > 0) score += 20;
    if (profile.title && profile.title.trim().length > 0) score += 20;
    if (profile.organization && profile.organization.trim().length > 0) score += 20;
    if (profile.phone && profile.phone.trim().length > 0) score += 15;
    if (profile.linkedinUrl && profile.linkedinUrl.trim().length > 0) score += 15;
    if (profile.avatarUrl && profile.avatarUrl.trim().length > 0) score += 10;
    return Math.min(100, Math.max(0, score));
  }

  /**
   * Retrieves the authenticated user's profile.
   */
  static async getProfile(userId: string) {
    const user = await prisma.user.findUnique({
      where: { id: userId },
      select: {
        id: true,
        email: true,
        role: true,
        createdAt: true,
        updatedAt: true,
        profile: {
          select: {
            id: true,
            fullName: true,
            title: true,
            organization: true,
            phone: true,
            linkedinUrl: true,
            avatarUrl: true,
            isVerified: true,
            completionScore: true,
            createdAt: true,
            updatedAt: true,
          },
        },
      },
    });

    if (!user) {
      const error: any = new Error('User not found');
      error.statusCode = 404;
      throw error;
    }

    return {
      user: {
        id: user.id,
        email: user.email,
        role: user.role,
        createdAt: user.createdAt,
        updatedAt: user.updatedAt,
      },
      profile: user.profile,
    };
  }

  /**
   * Updates or creates the authenticated user's profile.
   */
  static async updateProfile(userId: string, input: UpdateProfileInput) {
    const user = await prisma.user.findUnique({
      where: { id: userId },
      include: { profile: true },
    });

    if (!user) {
      const error: any = new Error('User not found');
      error.statusCode = 404;
      throw error;
    }

    // Merge existing profile fields with provided updates
    const currentFullName = input.fullName !== undefined ? input.fullName : user.profile?.fullName || '';
    const currentTitle = input.title !== undefined ? (input.title || null) : user.profile?.title || null;
    const currentOrganization = input.organization !== undefined ? (input.organization || null) : user.profile?.organization || null;
    const currentPhone = input.phone !== undefined ? (input.phone || null) : user.profile?.phone || null;
    const currentLinkedinUrl = input.linkedinUrl !== undefined ? (input.linkedinUrl || null) : user.profile?.linkedinUrl || null;
    const currentAvatarUrl = input.avatarUrl !== undefined ? (input.avatarUrl || null) : user.profile?.avatarUrl || null;

    // Recalculate completion score on the backend
    const completionScore = UserService.calculateCompletionScore({
      fullName: currentFullName,
      title: currentTitle,
      organization: currentOrganization,
      phone: currentPhone,
      linkedinUrl: currentLinkedinUrl,
      avatarUrl: currentAvatarUrl,
    });

    const updatedProfile = await prisma.profile.upsert({
      where: { userId },
      update: {
        fullName: currentFullName,
        title: currentTitle,
        organization: currentOrganization,
        phone: currentPhone,
        linkedinUrl: currentLinkedinUrl,
        avatarUrl: currentAvatarUrl,
        completionScore,
      },
      create: {
        userId,
        fullName: currentFullName || 'ProPass User',
        title: currentTitle,
        organization: currentOrganization,
        phone: currentPhone,
        linkedinUrl: currentLinkedinUrl,
        avatarUrl: currentAvatarUrl,
        completionScore,
      },
      select: {
        id: true,
        userId: true,
        fullName: true,
        title: true,
        organization: true,
        phone: true,
        linkedinUrl: true,
        avatarUrl: true,
        isVerified: true,
        completionScore: true,
        createdAt: true,
        updatedAt: true,
      },
    });

    return {
      user: {
        id: user.id,
        email: user.email,
        role: user.role,
      },
      profile: updatedProfile,
    };
  }

  /**
   * Aggregates live data from PostgreSQL for the Home Dashboard.
   */
  static async getDashboard(userId: string) {
    const user = await prisma.user.findUnique({
      where: { id: userId },
      include: {
        profile: true,
        digitalPass: true,
        registrations: {
          orderBy: { registeredAt: 'desc' },
          take: 5,
          include: {
            event: {
              select: {
                id: true,
                slug: true,
                title: true,
                overline: true,
                subtitle: true,
                location: true,
                startDate: true,
                endDate: true,
              },
            },
          },
        },
      },
    });

    if (!user) {
      const error: any = new Error('User not found');
      error.statusCode = 404;
      throw error;
    }

    const firstName = user.profile?.fullName
      ? user.profile.fullName.trim().split(' ')[0]
      : 'User';

    const greeting = `Hello, ${firstName}`;

    const recentActivity = user.registrations.map((reg) => ({
      registrationId: reg.id,
      eventId: reg.eventId,
      eventSlug: reg.event.slug,
      eventTitle: reg.event.title,
      eventOverline: reg.event.overline,
      eventSubtitle: reg.event.subtitle,
      eventLocation: reg.event.location,
      eventStartDate: reg.event.startDate,
      purpose: reg.purpose,
      durationDays: reg.durationDays,
      status: reg.status,
      registeredAt: reg.registeredAt,
    }));

    return {
      greeting,
      user: {
        id: user.id,
        email: user.email,
        role: user.role,
      },
      profile: user.profile
        ? {
            id: user.profile.id,
            fullName: user.profile.fullName,
            title: user.profile.title,
            organization: user.profile.organization,
            phone: user.profile.phone,
            linkedinUrl: user.profile.linkedinUrl,
            avatarUrl: user.profile.avatarUrl,
            isVerified: user.profile.isVerified,
            completionScore: user.profile.completionScore,
          }
        : null,
      pass: user.digitalPass
        ? {
            id: user.digitalPass.id,
            passNumber: user.digitalPass.passNumber,
            tier: user.digitalPass.tier,
            isActive: user.digitalPass.isActive,
            qrPayload: user.digitalPass.qrPayload,
            expiresAt: user.digitalPass.expiresAt,
          }
        : null,
      recentActivity,
    };
  }
}
