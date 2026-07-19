export interface SocialMediaFormatDef {
  key: string;
  label: string;
  targetWidth: number;
  targetHeight: number;
  isCircle: boolean;
}

export const SOCIAL_MEDIA_FORMATS: SocialMediaFormatDef[] = [
  { key: 'INSTAGRAM_POST',      label: 'Instagram Post (1080×1080)',      targetWidth: 1080, targetHeight: 1080, isCircle: false },
  { key: 'INSTAGRAM_PORTRAIT',  label: 'Instagram Portrait (1080×1350)',  targetWidth: 1080, targetHeight: 1350, isCircle: false },
  { key: 'INSTAGRAM_LANDSCAPE', label: 'Instagram Landscape (1080×566)',  targetWidth: 1080, targetHeight: 566,  isCircle: false },
  { key: 'INSTAGRAM_STORY',     label: 'Instagram Story (1080×1920)',     targetWidth: 1080, targetHeight: 1920, isCircle: false },
  { key: 'INSTAGRAM_PROFILE',   label: 'Instagram Profile (110×110)',     targetWidth: 110,  targetHeight: 110,  isCircle: true  },
  { key: 'FACEBOOK_POST',       label: 'Facebook Post (1200×630)',        targetWidth: 1200, targetHeight: 630,  isCircle: false },
  { key: 'FACEBOOK_PROFILE',    label: 'Facebook Profile (170×170)',      targetWidth: 170,  targetHeight: 170,  isCircle: true  },
  { key: 'LINKEDIN_POST',       label: 'LinkedIn Post (1200×627)',        targetWidth: 1200, targetHeight: 627,  isCircle: false },
  { key: 'LINKEDIN_PROFILE',    label: 'LinkedIn Profile (400×400)',      targetWidth: 400,  targetHeight: 400,  isCircle: true  },
  { key: 'TWITTER_POST',        label: 'Twitter/X Post (1600×900)',       targetWidth: 1600, targetHeight: 900,  isCircle: false },
  { key: 'TWITTER_PROFILE',     label: 'Twitter/X Profile (400×400)',     targetWidth: 400,  targetHeight: 400,  isCircle: true  },
  { key: 'TWITTER_HEADER',      label: 'Twitter/X Header (1500×500)',     targetWidth: 1500, targetHeight: 500,  isCircle: false },
];
