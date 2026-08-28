import { createRouter, createWebHistory } from 'vue-router'

import HomeView from '@/views/HomeView.vue'
import AdminLayout from '@/layouts/AdminLayout.vue'
import PublicLayout from '@/layouts/PublicLayout.vue'
import { requireAdmin } from '@/router/adminGuard'
import LinksView from '@/views/static/LinksView.vue'
import NewQuestionView from '@/views/static/NewQuestionView.vue'
import NotFoundView from '@/views/static/NotFoundView.vue'
import PostDetailView from '@/views/posts/PostDetailView.vue'
import SearchView from '@/views/posts/SearchView.vue'
import AdminQuestionDetailView from '@/views/admin/AdminQuestionDetailView.vue'
import AdminQuestionsView from '@/views/admin/AdminQuestionsView.vue'
import AdminPostEditorView from '@/views/admin/AdminPostEditorView.vue'
import AdminPostsView from '@/views/admin/AdminPostsView.vue'
import AdminHashtagsView from '@/views/admin/AdminHashtagsView.vue'
import AdminLinksView from '@/views/admin/AdminLinksView.vue'
import AdminLoginView from '@/views/admin/AdminLoginView.vue'
import AdminAnalyticsView from '@/views/admin/AdminAnalyticsView.vue'
import AdminView from '@/views/admin/AdminView.vue'
import ParcialesView from '@/views/sections/ParcialesView.vue'
import SectionPostsView from '@/views/sections/SectionPostsView.vue'
import TalleresView from '@/views/sections/TalleresView.vue'
import HashtagPostsView from '@/views/tags/HashtagPostsView.vue'

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes: [
    {
      path: '/',
      component: PublicLayout,
      children: [
        {
          path: '',
          name: 'home',
          component: HomeView,
        },
        {
          path: 'talleres',
          name: 'workshops',
          component: TalleresView,
        },
        {
          path: 'talleres/:slug',
          name: 'workshop-detail',
          component: SectionPostsView,
          props: { sectionKind: 'taller' },
        },
        {
          path: 'parciales',
          name: 'exams',
          component: ParcialesView,
        },
        {
          path: 'parciales/:slug',
          name: 'exam-detail',
          component: SectionPostsView,
          props: { sectionKind: 'parcial' },
        },
        {
          path: 'enlaces',
          name: 'links',
          component: LinksView,
        },
        {
          path: 'publicaciones/:id',
          name: 'post-detail',
          component: PostDetailView,
        },
        {
          path: 'buscar',
          name: 'search',
          component: SearchView,
        },
        {
          path: 'hashtags/:slug',
          name: 'hashtag-detail',
          component: HashtagPostsView,
        },
        {
          path: 'preguntas/nueva',
          name: 'new-question',
          component: NewQuestionView,
        },
      ],
    },
    {
      path: '/admin/login',
      name: 'admin-login',
      component: AdminLoginView,
    },
    {
      path: '/admin',
      component: AdminLayout,
      beforeEnter: requireAdmin,
      children: [
        {
          path: '',
          name: 'admin-home',
          component: AdminView,
        },
        {
          path: 'preguntas',
          name: 'admin-questions',
          component: AdminQuestionsView,
        },
        {
          path: 'preguntas/:id',
          name: 'admin-question-detail',
          component: AdminQuestionDetailView,
        },
        {
          path: 'publicaciones',
          name: 'admin-posts',
          component: AdminPostsView,
        },
        {
          path: 'publicaciones/:id',
          name: 'admin-post-detail',
          component: AdminPostEditorView,
        },
        {
          path: 'hashtags',
          name: 'admin-hashtags',
          component: AdminHashtagsView,
        },
        {
          path: 'enlaces',
          name: 'admin-links',
          component: AdminLinksView,
        },
        {
          path: 'estadisticas',
          name: 'admin-analytics',
          component: AdminAnalyticsView,
        },
      ],
    },
    {
      path: '/:pathMatch(.*)*',
      name: 'not-found',
      component: PublicLayout,
      children: [
        {
          path: '',
          component: NotFoundView,
        },
      ],
    },
  ],
})

export default router
