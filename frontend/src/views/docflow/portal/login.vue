<script setup lang="ts">
import { ref } from "vue";
import { useRouter } from "vue-router";
import { message } from "@/utils/message";
import { customerLogin, customerRegister, setCustomerSession } from "@/api/customer";

defineOptions({ name: "CustomerLogin" });

const router = useRouter();
const isRegister = ref(false);
const loading = ref(false);
const form = ref({
  username: "",
  password: "",
  email: "",
  company: "",
  realName: ""
});

async function handleSubmit() {
  if (!form.value.username || !form.value.password) {
    message("请填写用户名和密码", { type: "warning" });
    return;
  }
  loading.value = true;
  try {
    const { code, data, message: msg } = isRegister.value
      ? await customerRegister(form.value)
      : await customerLogin({ username: form.value.username, password: form.value.password });
    if (code !== 200) throw new Error(msg || "操作失败");
    setCustomerSession(data.token, data.customer);
    message(isRegister.value ? "注册成功" : "登录成功", { type: "success" });
    router.push("/portal/tickets");
  } catch (error) {
    message(error instanceof Error ? error.message : "操作失败", { type: "error" });
  } finally { loading.value = false; }
}
</script>

<template>
  <div class="portal-login-bg">
    <div class="portal-login-card">
      <div class="portal-login-header">
        <h1 class="portal-login-title">DocFlow AI</h1>
        <p class="portal-login-subtitle">客户服务中心</p>
      </div>

      <div class="portal-login-tabs">
        <button :class="{ active: !isRegister }" @click="isRegister = false">登录</button>
        <button :class="{ active: isRegister }" @click="isRegister = true">注册</button>
      </div>

      <div class="portal-login-form">
        <div class="portal-field">
          <label>用户名</label>
          <input v-model="form.username" placeholder="请输入用户名" @keyup.enter="handleSubmit" />
        </div>
        <div class="portal-field">
          <label>密码</label>
          <input v-model="form.password" type="password" placeholder="请输入密码" @keyup.enter="handleSubmit" />
        </div>
        <template v-if="isRegister">
          <div class="portal-field">
            <label>邮箱</label>
            <input v-model="form.email" placeholder="可选" />
          </div>
          <div class="portal-field">
            <label>公司</label>
            <input v-model="form.company" placeholder="可选" />
          </div>
          <div class="portal-field">
            <label>真实姓名</label>
            <input v-model="form.realName" placeholder="可选" />
          </div>
        </template>
        <button class="portal-btn" :disabled="loading" @click="handleSubmit">
          {{ loading ? '处理中...' : (isRegister ? '注册' : '登录') }}
        </button>
      </div>

      <div class="portal-login-footer">
        <router-link to="/login">管理员登录</router-link>
      </div>
    </div>
  </div>
</template>

<style scoped>
.portal-login-bg {
  min-height: 100vh; display: flex; align-items: center; justify-content: center;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
}
.portal-login-card {
  background: #fff; border-radius: 16px; padding: 40px;
  width: 100%; max-width: 420px; box-shadow: 0 20px 60px rgba(0,0,0,0.15);
}
.portal-login-header { text-align: center; margin-bottom: 28px; }
.portal-login-title { font-size: 28px; font-weight: 800; color: #0d253d; margin: 0; }
.portal-login-subtitle { font-size: 14px; color: #64748d; margin-top: 4px; }

.portal-login-tabs {
  display: flex; gap: 0; margin-bottom: 24px;
  background: #f1f5f9; border-radius: 8px; padding: 3px;
}
.portal-login-tabs button {
  flex: 1; padding: 8px; border: none; background: transparent;
  font-size: 14px; font-weight: 600; color: #64748d; border-radius: 6px;
  cursor: pointer; transition: all 0.2s;
}
.portal-login-tabs button.active {
  background: #fff; color: #0d253d; box-shadow: 0 1px 3px rgba(0,0,0,0.1);
}

.portal-field { margin-bottom: 16px; }
.portal-field label { display: block; font-size: 13px; font-weight: 600; color: #374151; margin-bottom: 4px; }
.portal-field input {
  width: 100%; padding: 10px 12px; border: 1px solid #e2e8f0; border-radius: 8px;
  font-size: 14px; outline: none; transition: border-color 0.2s;
  box-sizing: border-box;
}
.portal-field input:focus { border-color: #667eea; }

.portal-btn {
  width: 100%; padding: 12px; border: none; border-radius: 8px;
  background: linear-gradient(135deg, #667eea, #764ba2);
  color: #fff; font-size: 15px; font-weight: 700; cursor: pointer;
  transition: opacity 0.2s; margin-top: 8px;
}
.portal-btn:hover { opacity: 0.9; }
.portal-btn:disabled { opacity: 0.6; cursor: not-allowed; }

.portal-login-footer {
  text-align: center; margin-top: 20px; font-size: 13px;
}
.portal-login-footer a { color: #667eea; text-decoration: none; }
.portal-login-footer a:hover { text-decoration: underline; }
</style>
