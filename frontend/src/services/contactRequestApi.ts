export type ContactRequestStatus =
  | 'PENDING'
  | 'RESPONDED'
  | 'CLOSED'

export interface ContactRequest {
  id: number

  roomId: number
  roomTitle: string

  studentId: number
  studentName: string
  studentPhone: string | null
  studentEmail: string | null

  landlordId: number
  landlordName: string
  landlordPhone: string | null

  message: string | null
  status: ContactRequestStatus

  createdAt: string
  updatedAt: string
}

type ContactRequestOperation =
  | 'create'
  | 'mine'
  | 'landlord'
  | 'update'

function getErrorMessage(
  status: number,
  operation: ContactRequestOperation
): string {
  if (status === 401) {
    return 'Phiên đăng nhập đã hết hạn. Vui lòng đăng nhập lại.'
  }

  if (status === 403) {
    if (operation === 'create') {
      return 'Chỉ sinh viên mới có quyền gửi yêu cầu liên hệ.'
    }

    if (operation === 'mine') {
      return 'Bạn không có quyền xem danh sách yêu cầu này.'
    }

    return 'Bạn không có quyền quản lý yêu cầu liên hệ.'
  }

  if (status === 404) {
    if (operation === 'create') {
      return 'Không tìm thấy phòng hoặc phòng không còn khả dụng.'
    }

    return 'Không tìm thấy yêu cầu liên hệ.'
  }

  if (status === 409) {
    if (operation === 'create') {
      return 'Bạn đã gửi yêu cầu đang chờ xử lý cho phòng này.'
    }

    return 'Yêu cầu liên hệ đã được xử lý hoặc không thể chuyển sang trạng thái này.'
  }

  if (status === 400) {
    if (operation === 'create') {
      return 'Lời nhắn không hợp lệ.'
    }

    return 'Thông tin cập nhật không hợp lệ.'
  }

  return `Không thể xử lý yêu cầu liên hệ (HTTP ${status}).`
}

async function contactRequestApi<T>(
  path: string,
  token: string,
  method: 'GET' | 'POST' | 'PATCH',
  operation: ContactRequestOperation,
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

  const response = await fetch(path, options)

  if (!response.ok) {
    throw new Error(
      getErrorMessage(response.status, operation)
    )
  }

  return response.json() as Promise<T>
}

export function createContactRequestApi(
  roomId: number,
  message: string,
  token: string
): Promise<ContactRequest> {
  const trimmedMessage = message.trim()

  return contactRequestApi<ContactRequest>(
    `/api/v1/rooms/${roomId}/contact-requests`,
    token,
    'POST',
    'create',
    trimmedMessage
      ? { message: trimmedMessage }
      : undefined
  )
}

export function getMyContactRequestsApi(
  token: string
): Promise<ContactRequest[]> {
  return contactRequestApi<ContactRequest[]>(
    '/api/v1/contact-requests/mine',
    token,
    'GET',
    'mine'
  )
}

export function getLandlordContactRequestsApi(
  token: string
): Promise<ContactRequest[]> {
  return contactRequestApi<ContactRequest[]>(
    '/api/v1/landlord/contact-requests',
    token,
    'GET',
    'landlord'
  )
}

export function updateContactRequestStatusApi(
  id: number,
  status: ContactRequestStatus,
  token: string
): Promise<ContactRequest> {
  return contactRequestApi<ContactRequest>(
    `/api/v1/landlord/contact-requests/${id}/status`,
    token,
    'PATCH',
    'update',
    { status }
  )
}
