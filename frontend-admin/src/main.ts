import './styles/_style.css'
import './styles/_reset.css'
import { createApp } from 'vue'
import App from './App.vue'
import { createRouter, createWebHistory } from 'vue-router'
import { routes } from './routes'

const router = createRouter({
  history: createWebHistory(),
  routes: routes,
})

createApp(App).use(router).mount('#app')
