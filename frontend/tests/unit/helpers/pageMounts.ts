import { flushPromises, mount } from '@vue/test-utils'
import KnowledgeArticleDetailView from '../../../src/views/KnowledgeArticleDetailView.vue'
import KnowledgeArticleEditorView from '../../../src/views/KnowledgeArticleEditorView.vue'
import LoginView from '../../../src/views/LoginView.vue'
import TicketDetailView from '../../../src/views/TicketDetailView.vue'
import TicketCreateView from '../../../src/views/TicketCreateView.vue'

export async function mountKnowledgeArticleEditorView() {
  const wrapper = mount(KnowledgeArticleEditorView, {
    global: {
      stubs: {
        AppSidebar: true,
        AppTopbar: true,
      },
    },
  })

  await flushPromises()
  return wrapper
}

export async function mountTicketDetailView() {
  const wrapper = mount(TicketDetailView)
  await flushPromises()
  return wrapper
}

export async function mountKnowledgeArticleDetailView() {
  const wrapper = mount(KnowledgeArticleDetailView, {
    global: {
      stubs: {
        AppSidebar: true,
        AppTopbar: true,
      },
    },
  })
  await flushPromises()
  return wrapper
}

export async function mountTicketCreateView() {
  const wrapper = mount(TicketCreateView)
  await flushPromises()
  return wrapper
}

export async function mountLoginView() {
  const wrapper = mount(LoginView)
  await flushPromises()
  return wrapper
}
