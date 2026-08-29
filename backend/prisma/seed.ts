import { PrismaClient, PurposeOfVisit, RegistrationStatus, Role } from '@prisma/client';

const prisma = new PrismaClient();

async function main() {
  console.log('🌱 Starting ProPass database seeding...');

  // 1. Seed Events
  const events = [
    {
      slug: 'techconf-2024',
      title: 'TechConf 2024',
      overline: 'EVENT REGISTRATION',
      subtitle: 'Complete your registration to secure your spot.',
      description: 'The premier global technology and developer conference.',
      location: 'Moscone Center, San Francisco, CA',
      startDate: new Date('2026-10-14T09:00:00Z'),
      endDate: new Date('2026-10-16T18:00:00Z'),
      maxDuration: 5,
      isActive: true,
    },
    {
      slug: 'google-office-visit',
      title: 'Google Office Visit',
      overline: 'VISITOR PASS',
      subtitle: 'Check in for your scheduled campus visit.',
      description: 'Authorized visitor pass for Google Campus building access.',
      location: 'Google Building 43, Mountain View, CA',
      startDate: new Date('2026-09-28T08:30:00Z'),
      endDate: new Date('2026-09-28T17:30:00Z'),
      maxDuration: 1,
      isActive: true,
    },
    {
      slug: 'android-conf-2026',
      title: 'Android Conf 2026',
      overline: 'CONFERENCE PASS',
      subtitle: 'Secure your badge for Android developer sessions.',
      description: 'Annual gathering of Android platform engineers and designers.',
      location: 'Convention Center, Austin, TX',
      startDate: new Date('2026-11-05T09:00:00Z'),
      endDate: new Date('2026-11-07T18:00:00Z'),
      maxDuration: 3,
      isActive: true,
    },
  ];

  for (const event of events) {
    const upsertedEvent = await prisma.event.upsert({
      where: { slug: event.slug },
      update: event,
      create: event,
    });
    console.log(`  ✓ Event ready: ${upsertedEvent.title} (${upsertedEvent.slug})`);
  }

  // 2. Seed Demo Users & Profiles
  const demoUsers = [
    {
      email: 'alex.morgan@example.com',
      role: Role.USER,
      profile: {
        fullName: 'Alex Morgan',
        title: 'Software Engineer',
        organization: 'University of Technology',
        phone: '+1 (555) 012-3456',
        linkedinUrl: 'https://linkedin.com/in/alexmorgan',
        isVerified: true,
        completionScore: 90,
      },
      pass: {
        passNumber: 'PP-2024-1001',
        qrPayload: 'propass:pass:PP-2024-1001',
        tier: 'PREMIUM',
      },
    },
    {
      email: 'sarah.jenkins@example.com',
      role: Role.USER,
      profile: {
        fullName: 'Sarah Jenkins',
        title: 'Senior UX Researcher',
        organization: 'TechFlow Inc.',
        phone: '+1 (555) 018-9234',
        linkedinUrl: 'https://linkedin.com/in/sarahjenkins',
        isVerified: true,
        completionScore: 85,
      },
      pass: {
        passNumber: 'PP-2024-1002',
        qrPayload: 'propass:pass:PP-2024-1002',
        tier: 'PREMIUM',
      },
    },
    {
      email: 'elena.rodriguez@example.com',
      role: Role.USER,
      profile: {
        fullName: 'Elena Rodriguez',
        title: 'Lead Product Designer',
        organization: 'Acme Corp',
        phone: '+1 (555) 019-2834',
        linkedinUrl: 'https://linkedin.com/in/elenarodriguez',
        isVerified: true,
        completionScore: 95,
      },
      pass: {
        passNumber: 'PP-2024-1003',
        qrPayload: 'propass:pass:PP-2024-1003',
        tier: 'PREMIUM',
      },
    },
  ];

  for (const demo of demoUsers) {
    const user = await prisma.user.upsert({
      where: { email: demo.email },
      update: {
        role: demo.role,
      },
      create: {
        email: demo.email,
        role: demo.role,
      },
    });

    await prisma.profile.upsert({
      where: { userId: user.id },
      update: demo.profile,
      create: {
        userId: user.id,
        ...demo.profile,
      },
    });

    await prisma.digitalPass.upsert({
      where: { userId: user.id },
      update: {
        passNumber: demo.pass.passNumber,
        qrPayload: demo.pass.qrPayload,
        tier: demo.pass.tier,
      },
      create: {
        userId: user.id,
        passNumber: demo.pass.passNumber,
        qrPayload: demo.pass.qrPayload,
        tier: demo.pass.tier,
      },
    });

    console.log(`  ✓ Demo User ready: ${demo.profile.fullName} (${demo.email})`);
  }

  // 3. Seed Sample Registration (Sarah Jenkins -> TechConf 2024)
  const sarah = await prisma.user.findUnique({ where: { email: 'sarah.jenkins@example.com' } });
  const techConf = await prisma.event.findUnique({ where: { slug: 'techconf-2024' } });

  if (sarah && techConf) {
    await prisma.registration.upsert({
      where: {
        userId_eventId: {
          userId: sarah.id,
          eventId: techConf.id,
        },
      },
      update: {},
      create: {
        userId: sarah.id,
        eventId: techConf.id,
        fullName: 'Sarah Jenkins',
        email: 'sarah.jenkins@example.com',
        institution: 'TechFlow Inc.',
        purpose: PurposeOfVisit.SPEAKER,
        durationDays: 3,
        vehicleNumber: 'CA7XYZ99',
        status: RegistrationStatus.CONFIRMED,
      },
    });
    console.log(`  ✓ Sample Registration seeded: Sarah Jenkins @ TechConf 2024`);
  }

  console.log('✅ ProPass database seeding completed successfully!');
}

main()
  .catch((e) => {
    console.error('❌ Seeding failed:', e);
    process.exit(1);
  })
  .finally(async () => {
    await prisma.$disconnect();
  });
