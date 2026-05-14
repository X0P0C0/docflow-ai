<template>
  <div class="content-column side-column">
    <article class="panel panel-dark">
      <div class="panel-head panel-head-dark">
        <div>
          <h3>AI 助手摘要</h3>
          <p>像安静的辅助面板，不喧宾夺主</p>
        </div>
      </div>
      <div class="ai-summary">
        已识别 3 条工单与“支付回调失败”知识文章高度相关。建议优先检查签名配置、回调连通性和 Redis 幂等键有效期。
      </div>
      <div class="mini-list dark-list">
        <div class="mini-item"><span>推荐回复模板</span><span>2 条</span></div>
        <div class="mini-item"><span>关联知识文章</span><span>5 篇</span></div>
        <div class="mini-item"><span>可自动分类工单</span><span>11 条</span></div>
      </div>
    </article>

    <article class="panel">
      <div class="panel-head">
        <div>
          <h3>测试路径</h3>
          <p>按这条顺序走，最容易一次跑通完整业务。</p>
        </div>
      </div>
      <div class="mini-list">
        <div class="mini-item"><span>1.</span><span>登录后先新建一条工单</span></div>
        <div class="mini-item"><span>2.</span><span>回到工单列表确认它已出现</span></div>
        <div class="mini-item"><span>3.</span><span>进入详情补评论、指派处理人、更新状态</span></div>
        <div class="mini-item"><span>4.</span><span>再去 Dashboard 看列表与摘要是否同步</span></div>
      </div>
    </article>

    <article class="panel">
      <div class="panel-head">
        <div>
          <h3>最近动态</h3>
          <p>团队协作时间线</p>
        </div>
      </div>
      <div class="timeline">
        <div v-for="activity in activities" :key="activity.title" class="timeline-item">
          <span class="dot"></span>
          <div>
            <strong>{{ activity.title }}</strong>
            <p>{{ activity.desc }}</p>
          </div>
        </div>
      </div>
    </article>

    <article class="panel">
      <div class="panel-head">
        <div>
          <h3>当前结论</h3>
          <p>这就是我们项目的前端气质</p>
        </div>
      </div>
      <div class="mini-list">
        <div class="mini-item"><span>整体风格</span><span>克制、专业、现代</span></div>
        <div class="mini-item"><span>页面结构</span><span>左导航 + 顶栏 + 主内容</span></div>
        <div class="mini-item"><span>内容方向</span><span>重视层级、留白、产品感</span></div>
        <div class="mini-item"><span>AI 呈现方式</span><span>摘要卡片、建议面板</span></div>
      </div>
    </article>
  </div>
</template>

<script setup lang="ts">
import type { ActivityItem } from '../../types/dashboard'

defineProps<{
  activities: ActivityItem[]
}>()
</script>
