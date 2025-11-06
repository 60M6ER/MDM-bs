const routes = [
  {
    path: '/',
    component: () => import('layouts/MainLayout.vue'),
    children: [
      { path: '', component: () => import('pages/IndexPage.vue') },
      {
        path: '/settings',
        component: () => import('pages/settings/SettingsLayout.vue'),
        children: [
          { path: '', redirect: '/settings/app' },
          { path: 'app', component: () => import('pages/settings/AppSettingsPage.vue') },
          {
            path: 'departments/:id?',
            component: () => import('pages/settings/DepartmentsShell.vue'),
            props: true
          }
        ]
      },
    ],
  },

  {
    path: '/login',
    component: () => import('pages/LoginPage.vue'),
  },

  // Always leave this as last one,
  // but you can also remove it
  {
    path: '/:catchAll(.*)*',
    component: () => import('pages/ErrorNotFound.vue'),
  },
]

export default routes
