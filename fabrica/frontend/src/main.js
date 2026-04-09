import { createApp } from 'vue'
import './style.css'
import App from './App.vue'
import router from './router'

/** Instancia Vue 3 con el router de historial HTML5 y montaje en `#app`. */
createApp(App).use(router).mount('#app')
