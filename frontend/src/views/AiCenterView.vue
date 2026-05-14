<template>
  <div class="app-shell">
    <AppSidebar :workspace-nav="workspaceNav" :manage-nav="manageNav" />

    <main class="main-content">
      <AppTopbar />

      <section class="panel knowledge-hero ai-hero">
        <div>
          <span class="hero-tag">AI Center</span>
          <h2>把摘要、推荐、回复建议和知识沉淀放进同一条协作链路里。</h2>
          <p>这个页面先把 AI 能力的展示层搭起来，后面可以逐步接真实模型调用、工单推荐和内容生成结果。</p>
        </div>
        <div class="hero-stats">
          <div class="stat-row">
            <div>
              <p>今日建议</p>
              <strong>42</strong>
            </div>
            <span class="chip chip-blue">Suggestions</span>
          </div>
          <div class="stat-row">
            <div>
              <p>被采纳</p>
              <strong>26</strong>
            </div>
            <span class="chip chip-green">Accepted</span>
          </div>
          <div class="stat-row">
            <div>
              <p>知识推荐</p>
              <strong>18</strong>
            </div>
            <span class="chip chip-orange">Linked</span>
          </div>
        </div>
      </section>

      <section class="ai-grid">
        <article class="panel">
          <div class="panel-head">
            <div>
              <h3>回复建议</h3>
              <p>让支持同学先有一个高质量起草版本。</p>
            </div>
            <span class="chip chip-green">Ready</span>
          </div>
          <div class="ai-summary">
            建议先向用户确认支付时间、订单号和回调日志范围，再同步当前排查进度，避免用户重复提交信息。
          </div>
        </article>

        <article class="panel">
          <div class="panel-head">
            <div>
              <h3>知识推荐</h3>
              <p>把工单和知识资产自动串起来。</p>
            </div>
            <button class="ghost-button" type="button">查看全部</button>
          </div>
          <div class="mini-list">
            <div class="mini-item"><span>支付回调失败排查手册</span><span>92% 匹配</span></div>
            <div class="mini-item"><span>发布异常应急处理流程</span><span>78% 匹配</span></div>
            <div class="mini-item"><span>内部文档权限问题 SOP</span><span>71% 匹配</span></div>
          </div>
        </article>
      </section>
    </main>
  </div>
</template>

<script setup lang="ts">
import AppSidebar from '../components/layout/AppSidebar.vue'
import AppTopbar from '../components/layout/AppTopbar.vue'
import { manageNav, workspaceNav } from '../mock/dashboard'
</script>
