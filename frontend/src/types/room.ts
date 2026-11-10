
export interface Room {
  id: number
  title: string
  description: string | null
  address: string
  areaName: string | null
  price: number
  areaSqm: number
  maxOccupants: number
}

export interface RoomPage {
  content: Room[]
  page: number
  size: number
  totalElements: number
  totalPages: number
  first: boolean
  last: boolean
}

export interface RoomFilters {
  keyword: string
  area: string
  minPrice: string
  maxPrice: string
  minArea: string
  maxArea: string
  sort: string
}
