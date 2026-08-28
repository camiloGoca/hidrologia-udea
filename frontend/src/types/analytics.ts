import type { PostSection } from './post'
import type { SectionType } from './section'

export interface PublicVisitCount {
  visits: number
}

export interface AnalyticsSessionRequest {
  sessionId: string
}

export interface AdminAnalyticsSection {
  id: number
  type: SectionType
  name: string
  slug: string
  views: number
}

export interface AdminAnalyticsPost {
  id: number
  title: string
  section: PostSection
  views: number
}

export interface AdminAnalyticsQuestions {
  total: number
  pending: number
  published: number
}

export interface AdminAnalyticsDailyVisit {
  date: string
  visits: number
}

export interface AdminAnalyticsSummary {
  totalVisits: number
  visitsToday: number
  visitsThisWeek: number
  visitsThisMonth: number
  mostViewedSections: AdminAnalyticsSection[]
  mostViewedWorkshop: AdminAnalyticsSection | null
  mostViewedExam: AdminAnalyticsSection | null
  mostViewedPosts: AdminAnalyticsPost[]
  questions: AdminAnalyticsQuestions
  dailyVisits: AdminAnalyticsDailyVisit[]
}
