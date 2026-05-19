import { flushPromises, mount } from '@vue/test-utils'
import { defineComponent, h } from 'vue'
import KnowledgeArticleDetailView from '../../../src/views/KnowledgeArticleDetailView.vue'
import KnowledgeArticleEditorView from '../../../src/views/KnowledgeArticleEditorView.vue'
import LoginView from '../../../src/views/LoginView.vue'
import TicketDetailView from '../../../src/views/TicketDetailView.vue'
import TicketCreateView from '../../../src/views/TicketCreateView.vue'

let mountedTicketDetailWrapper: ReturnType<typeof mount> | null = null
let mountedTicketCreateWrapper: ReturnType<typeof mount> | null = null
let mountedKnowledgeArticleDetailWrapper: ReturnType<typeof mount> | null = null
let mountedKnowledgeArticleEditorWrapper: ReturnType<typeof mount> | null = null

const ElButtonStub = defineComponent({
  name: 'ElButtonStub',
  props: {
    disabled: Boolean,
    loading: Boolean,
    nativeType: {
      type: String,
      default: 'button',
    },
  },
  emits: ['click'],
  setup(props, { emit, slots, attrs }) {
    return () => h('button', {
      ...attrs,
      type: props.nativeType || 'button',
      disabled: props.disabled || props.loading,
      onClick: (event: MouseEvent) => emit('click', event),
    }, slots.default?.())
  },
})

const ElInputStub = defineComponent({
  name: 'ElInputStub',
  props: {
    modelValue: {
      type: [String, Number],
      default: '',
    },
    type: {
      type: String,
      default: 'text',
    },
    rows: {
      type: [String, Number],
      default: undefined,
    },
    maxlength: {
      type: [String, Number],
      default: undefined,
    },
    placeholder: {
      type: String,
      default: '',
    },
  },
  emits: ['update:modelValue'],
  setup(props, { emit, attrs }) {
    return () => {
      const sharedProps = {
        ...attrs,
        class: ['field-control', attrs.class],
        value: props.modelValue,
        placeholder: props.placeholder,
        maxlength: props.maxlength,
        onInput: (event: Event) => emit('update:modelValue', (event.target as HTMLInputElement | HTMLTextAreaElement).value),
      }

      if (props.type === 'textarea') {
        return h('textarea', {
          ...sharedProps,
          rows: props.rows,
        })
      }

      return h('input', {
        ...sharedProps,
        type: props.type,
      })
    }
  },
})

const ElOptionStub = defineComponent({
  name: 'ElOptionStub',
  props: {
    value: {
      type: [String, Number, Boolean],
      default: '',
    },
    label: {
      type: String,
      default: '',
    },
  },
  setup(props) {
    return () => h('option', { value: String(props.value) }, props.label)
  },
})

const ElSelectStub = defineComponent({
  name: 'ElSelectStub',
  props: {
    modelValue: {
      type: [String, Number],
      default: '',
    },
  },
  emits: ['update:modelValue'],
  setup(props, { emit, slots, attrs }) {
    return () => h('select', {
      ...attrs,
      class: ['field-control', attrs.class],
      value: String(props.modelValue),
      onChange: (event: Event) => {
        const rawValue = (event.target as HTMLSelectElement).value
        emit('update:modelValue', typeof props.modelValue === 'number' ? Number(rawValue) : rawValue)
      },
    }, slots.default?.())
  },
})

const ElCardStub = defineComponent({
  name: 'ElCardStub',
  setup(_, { slots }) {
    return () => h('div', { class: 'el-card-stub' }, slots.default?.())
  },
})

const ElFormStub = defineComponent({
  name: 'ElFormStub',
  setup(_, { slots, attrs }) {
    return () => h('form', attrs, slots.default?.())
  },
})

const ElFormItemStub = defineComponent({
  name: 'ElFormItemStub',
  props: {
    label: {
      type: String,
      default: '',
    },
  },
  setup(props, { slots }) {
    return () => h('label', { class: 'el-form-item-stub' }, [
      h('span', props.label),
      slots.default?.(),
    ])
  },
})

export async function mountKnowledgeArticleEditorView() {
  mountedKnowledgeArticleEditorWrapper?.unmount()
  const wrapper = mount(KnowledgeArticleEditorView, {
    global: {
      stubs: {
        AppShell: {
          template: '<div class="app-shell-stub"><slot /></div>',
        },
        'el-card': ElCardStub,
        'el-form': ElFormStub,
        'el-form-item': ElFormItemStub,
        'el-button': ElButtonStub,
        'el-input': ElInputStub,
        'el-option': ElOptionStub,
        'el-select': ElSelectStub,
      },
    },
  })

  mountedKnowledgeArticleEditorWrapper = wrapper
  await flushPromises()
  return wrapper
}

export async function mountTicketDetailView() {
  mountedTicketDetailWrapper?.unmount()
  const wrapper = mount(TicketDetailView, {
    global: {
      stubs: {
        AppShell: {
          template: '<div class="app-shell-stub"><slot /></div>',
        },
      },
    },
  })
  mountedTicketDetailWrapper = wrapper
  await flushPromises()
  return wrapper
}

export async function mountKnowledgeArticleDetailView() {
  mountedKnowledgeArticleDetailWrapper?.unmount()
  const wrapper = mount(KnowledgeArticleDetailView, {
    global: {
      stubs: {
        AppShell: {
          template: '<div class="app-shell-stub"><slot /></div>',
        },
      },
    },
  })
  mountedKnowledgeArticleDetailWrapper = wrapper
  await flushPromises()
  return wrapper
}

export async function mountTicketCreateView() {
  mountedTicketCreateWrapper?.unmount()
  const wrapper = mount(TicketCreateView, {
    global: {
      stubs: {
        AppShell: {
          template: '<div class="app-shell-stub"><slot /></div>',
        },
        'el-button': ElButtonStub,
        'el-input': ElInputStub,
        'el-option': ElOptionStub,
        'el-select': ElSelectStub,
      },
    },
  })
  mountedTicketCreateWrapper = wrapper
  await flushPromises()
  return wrapper
}

export async function mountLoginView() {
  const wrapper = mount(LoginView)
  await flushPromises()
  return wrapper
}
