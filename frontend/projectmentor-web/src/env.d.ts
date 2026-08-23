/// <reference types="vite/client" />

interface ImportMetaEnv {
  readonly VITE_WORKBENCH_EXPERIENCE_ENABLED?: string
}

interface ImportMeta {
  readonly env: ImportMetaEnv
}
