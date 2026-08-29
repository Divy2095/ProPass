export interface DigitalPassDto {
  id: string;
  passNumber: string;
  tier: string;
  isActive: boolean;
  isExpired: boolean;
  qrPayload: string;
  expiresAt: Date | null;
  createdAt: Date;
  updatedAt: Date;
}

export interface PassHolderDto {
  userId: string;
  email: string;
  fullName: string;
  title: string | null;
  organization: string | null;
  phone: string | null;
  linkedinUrl: string | null;
  avatarUrl: string | null;
  isVerified: boolean;
}

export interface MyPassResponse {
  pass: DigitalPassDto;
  holder: PassHolderDto;
}
