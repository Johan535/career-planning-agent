import axios from 'axios'

export const http = axios.create({
  baseURL: 'http://localhost:8123/api',
  timeout: 60_000,
  headers: { 'Content-Type': 'application/json' },
})

