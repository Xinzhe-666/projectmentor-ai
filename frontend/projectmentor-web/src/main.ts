import { createApp } from 'vue'
import { createPinia } from 'pinia'
import ElementPlus from 'element-plus'
import 'element-plus/dist/index.css'
import 'highlight.js/styles/github.css'

import * as ElementPlusIconsVue from '@element-plus/icons-vue'

import App from './App.vue'
import { i18n } from './locales'
import router from './router'
import './style.css'
import './styles/product-theme.css'
import './styles/animations.css'
import './styles/workbench-shell.css'

const app = createApp(App)

for (const [key, component] of Object.entries(ElementPlusIconsVue)) {
  app.component(key, component)
}

app.use(createPinia())
app.use(router)
app.use(ElementPlus)
app.use(i18n)

app.mount('#app')
