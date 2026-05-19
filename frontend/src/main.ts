import { createApp } from 'vue'
import './style.css'
import App from './App.vue'
import router from './router'

// Keep the app entry thin so routing, auth, and each business view own their logic.
createApp(App)
  .use(router)
  .mount('#app')
