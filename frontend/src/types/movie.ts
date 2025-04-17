// src/types/movie.ts

// Re-define MovieType here to resolve import issues
export interface MovieType {
  id: number
  name: string
  tmdbGenreId?: number | null
}

// Corresponds to the Movie schema in OpenAPI
export interface Movie {
  id: number
  title: string
  director?: string
  actors?: string
  duration?: number
  description?: string
  posterUrl?: string // Assuming this holds the full URL or path
  releaseDate?: string // Format: "YYYY-MM-DD"
  country?: string
  status?: 'COMING_SOON' | 'NOW_PLAYING' | 'OFFLINE'
  trailerUrl?: string
  tmdbId?: number
  tmdbVoteAverage?: number
  tmdbVoteCount?: number
  tmdbPopularity?: number
  totalBoxOffice?: number
  averageRating?: number
  createTime?: string // Format: ISO DateTime
  updateTime?: string // Format: ISO DateTime
  movieTypeIds?: number[] // Only used for create/update potentially
  movieTypes?: MovieType[] // Included in response
}
