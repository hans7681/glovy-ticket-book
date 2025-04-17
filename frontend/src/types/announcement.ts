// Types related to announcements will be defined here.
export {}

/**
 * Represents an announcement.
 * Based on the Announcement schema in the OpenAPI documentation.
 */
export interface Announcement {
  id: number
  title: string
  content: string
  publisherId?: number | null // ID of the admin user who published
  isPublished: boolean
  publishTime?: string | null // ISO 8601 DateTime string or null
  createTime: string // ISO 8601 DateTime string
  updateTime: string // ISO 8601 DateTime string
}

/**
 * Request body for publishing/unpublishing an announcement.
 * PUT /api/admin/announcements/{id}/publish
 */
export interface PublishRequest {
  publish: boolean
}
