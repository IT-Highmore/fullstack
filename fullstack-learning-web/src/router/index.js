import { createRouter, createWebHistory } from "vue-router";
import Layout from "../layout/Layout.vue";

const routes = [
  {
    path: "/login",
    name: "Login",
    component: () => import("../views/Login.vue"),
    meta: { title: "登录" },
  },
  {
    path: "/",
    component: Layout,
    redirect: "/dashboard",
    children: [
      {
        path: "dashboard",
        name: "Dashboard",
        component: () => import("../views/Dashboard.vue"),
        meta: { title: "控制台首页" },
      },
      {
        path: "users",
        name: "UserManagement",
        component: () => import("../views/UserManagement.vue"),
        meta: { title: "用户管理" },
      },
    ],
  },
];

const router = createRouter({
  history: createWebHistory(),
  routes,
});

// 🔐 全局前置路由守卫
router.beforeEach((to, from, next) => {
  const token = localStorage.getItem("token");

  if (to.path === "/login") {
    if (token) {
      // 已登录用户访问登录页，直接重定向到主页
      next("/dashboard");
    } else {
      next();
    }
  } else {
    if (!token) {
      // 未登录用户访问受保护路由，重定向到登录页
      next("/login");
    } else {
      next();
    }
  }
});

export default router;
