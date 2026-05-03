import axios from 'axios'
import { API_BASE } from '../config/api'

export const http = axios.create({
  baseURL: API_BASE,
  timeout: 60_000,
  headers: { 'Content-Type': 'application/json' },
})

