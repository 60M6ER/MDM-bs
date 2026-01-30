const routes = [
  {
    path: '/',
    component: () => import('layouts/MainLayout.vue'),
    children: [
      {
        path: '',
        component: () => import('pages/IndexPage.vue'),
        meta: {requiresAuth: true}
      },
      {
        path: '/devices',
        component: () => import('pages/DevicesPage.vue'),
        meta: {requiresAuth: true}
      },
      {
        path: '/settings',
        component: () => import('pages/settings/SettingsLayout.vue'),
        meta: {requiresAuth: true},
        children: [
          { path: '', redirect: '/settings/app' },
          { path: 'app', component: () => import('pages/settings/AppSettingsPage.vue') },
          { path: 'exchange_devices', component: () => import('pages/settings/ExchangeSettingsPage.vue') },
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
    name: 'login',
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
