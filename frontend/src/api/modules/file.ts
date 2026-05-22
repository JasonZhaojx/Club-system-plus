import { request } from '@/api/http'

export interface FileUploadResult {
  url: string
  objectName: string
  contentType: string
  size: number
}

export function uploadImage(file: File, scene = 'activity') {
  const formData = new FormData()
  formData.append('file', file)
  formData.append('scene', scene)

  return request<FileUploadResult>({
    url: '/files/images',
    method: 'POST',
    data: formData,
    suppressGlobalError: true,
  })
}

export function uploadAvatar(file: File) {
  const formData = new FormData()
  formData.append('file', file)

  return request<FileUploadResult>({
    url: '/files/avatars',
    method: 'POST',
    data: formData,
    suppressGlobalError: true,
  })
}
