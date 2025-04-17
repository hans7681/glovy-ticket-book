/**
 * Represents a movie type (genre).
 */
export interface MovieType {
  id: number
  name: string
  tmdbGenreId?: number | null // TMDB Genre ID is optional
}
