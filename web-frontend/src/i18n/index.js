import { ref, computed } from 'vue'
import en from './en'
import zh from './zh'

const messages = { en, zh }
const currentLang = ref(localStorage.getItem('wa-lang') || 'zh')

export function useI18n() {
  const lang = computed(() => currentLang.value)

  function t(key) {
    const keys = key.split('.')
    let val = messages[currentLang.value]
    for (const k of keys) {
      if (val && typeof val === 'object') val = val[k]
      else return key
    }
    return val || key
  }

  function toggleLang() {
    currentLang.value = currentLang.value === 'zh' ? 'en' : 'zh'
    localStorage.setItem('wa-lang', currentLang.value)
  }

  function setLang(l) {
    currentLang.value = l
    localStorage.setItem('wa-lang', l)
  }

  return { t, lang, toggleLang, setLang }
}
