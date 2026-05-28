import { request } from './request'
import type { Project, ProjectFile, UploadZipResult } from '@/types/api'

export interface CreateProjectParams {
  name: string
  githubUrl?: string
  description?: string
  projectType?: string
  techStack?: string
}

export function createProject(data: CreateProjectParams) {
  return request<Project>({
    url: '/api/projects',
    method: 'post',
    data
  })
}

export function listProjects() {
  return request<Project[]>({
    url: '/api/projects',
    method: 'get'
  })
}

export function getProjectDetail(id: number) {
  return request<Project>({
    url: `/api/projects/${id}`,
    method: 'get'
  })
}

export function deleteProject(id: number) {
  return request<void>({
    url: `/api/projects/${id}`,
    method: 'delete'
  })
}

export function saveReadme(projectId: number, content: string) {
  return request<ProjectFile>({
    url: `/api/projects/${projectId}/readme`,
    method: 'post',
    data: { content }
  })
}

export function listProjectFiles(projectId: number) {
  return request<ProjectFile[]>({
    url: `/api/projects/${projectId}/files`,
    method: 'get'
  })
}

export function uploadZip(projectId: number, file: File) {
  const formData = new FormData()
  formData.append('file', file)

  return request<UploadZipResult>({
    url: `/api/projects/${projectId}/upload-zip`,
    method: 'post',
    data: formData,
    timeout: 900000,
    headers: {
      'Content-Type': 'multipart/form-data'
    }
  })
}
