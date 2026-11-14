export async function loadIncidents(status: string) {
  return fetch(`/api/incidents?status=${encodeURIComponent(status)}`).then(response => response.json())
}
