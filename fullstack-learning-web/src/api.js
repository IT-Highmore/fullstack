import axios from "axios";
import { ElMessage } from "element-plus";

const api = axios.create({
  baseURL: "http://localhost:8080/api",
});

// 1. 请求拦截器：发请求前自动带上 Authorization Header
api.interceptors.request.use(
  (config) => {
    const token = localStorage.getItem("token");
    if (token) {
      config.headers.Authorization = `Bearer ${token}`;
    }
    return config;
  },
  (error) => Promise.reject(error),
);

// 2. 响应拦截器：若返回 401 说明 Token 失效或未登录
api.interceptors.response.use(
  (response) => response,
  (error) => {
    if (error.response && error.response.status === 401) {
      ElMessage.error("身份验证失效或未登录，请重新登录");
      localStorage.removeItem("token");
      window.location.reload();
    }
    return Promise.reject(error);
  },
);

export default api;
