export interface AdminRoom {
  id: number
  title: string
  description: string | null
  address: string
  areaName: string | null
  price: number
  areaSqm: number
  maxOccupants: number
  moderationStatus: string
  rejectionReason: string | null
  occupancyStatus: string
  visibilityStatus: string
  landlordId: number
}

async function adminRequest<T>(
  path: string,
  token: string,
  method: 'GET' | 'PATCH' = 'GET',
  body?: unknown
): Promise<T> {
  const headers: Record<string, string> = {
    Authorization: `Bearer ${token}`,
  }

  const options: RequestInit = {
    method,
    headers,
  }

  if (body !== undefined) {
    headers['Content-Type'] = 'application/json'
    options.body = JSON.stringify(body)
  }

  const response = await fetch(`/api/v1/admin/rooms${path}`, options)

  if (!response.ok) {
    if (response.status === 401) {
      throw new Error('Phiên đăng nhập hết hạn.')
    }

    if (response.status === 403) {
      throw new Error('Tài khoản không có quyền ADMIN.')
    }

    if (response.status === 400) {
      throw new Error('Lý do từ chối không hợp lệ.')
    }

    if (response.status === 409) {
      throw new Error('Tin đăng đã được xử lý trước đó.')
    }

    throw new Error(`Lỗi HTTP ${response.status}`)
  }

  return response.json() as Promise<T>
}

export function getPendingRoomsApi(token: string) {
  return adminRequest<AdminRoom[]>('/pending', token)
}

export function approveRoomApi(id: number, token: string) {
  return adminRequest<AdminRoom>(
    `/${id}/approve`,
    token,
    'PATCH'
  )
}

export function rejectRoomApi(
  id: number,
  rejectionReason: string,
  token: string
) {
  return adminRequest<AdminRoom>(
    `/${id}/reject`,
    token,
    'PATCH',
    { rejectionReason }
  )
}
