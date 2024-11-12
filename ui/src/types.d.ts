export interface UIConfig {
  sourceEnvironments?: string[]
  currentEnvironment?: string
}

export interface LocalisationOverride {
  id: number
  namespace: string
  locale: string
  key: string
  value: string
  created: string
  createdBy: string
  updated: string
  updatedBy: string
}

export interface Message {
  message: React.ReactNode
  id: string
}
