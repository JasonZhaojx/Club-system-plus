import type { ReadOptions, ReadResult } from './content'

export const EVENT_TIME_ZONE = 'Australia/Sydney'
export const EVENT_LOAD_ERROR = '活动信息暂时无法加载，请稍后重试'

export type EventStatus = 'upcoming' | 'ongoing' | 'ended' | 'cancelled'
export type EventbriteStatus = 'live' | 'started' | 'ended' | 'completed' | 'canceled'

/** Normalized public Worker payload; contains no credentials or attendee data. */
export interface PublicEvent {
  id: string
  title: string
  summary: string
  image: string | null
  url: string
  /** ISO 8601 timestamps with explicit timezone offset or Z. */
  start: string
  end: string
  hideStart: boolean
  hideEnd: boolean
  status: EventbriteStatus
  venue: string
}

/** Client-side filtering by start date in Australia/Sydney. */
export interface EventFilter {
  year?: number
  month?: 1 | 2 | 3 | 4 | 5 | 6 | 7 | 8 | 9 | 10 | 11 | 12
}

/** Compute against current time; do not persist a derived status in content. */
export type ResolveEventStatus = (event: PublicEvent, now: Date) => EventStatus

export interface EventSource {
  /** Success with [] is empty; failed or partial pagination is an error. */
  readEvents(options?: ReadOptions): Promise<ReadResult<PublicEvent[]>>
  /** The identifier must be an Eventbrite ID, never a legacy activity ID. */
  readEvent(eventbriteId: string, options?: ReadOptions): Promise<ReadResult<PublicEvent>>
}
