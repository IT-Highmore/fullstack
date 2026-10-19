# 使用轻量级 Nginx 镜像
FROM nginx:alpine

# 复制打包后的静态文件到 Nginx 目录
COPY dist/ /usr/share/nginx/html/

# 复制自定义 Nginx 配置
COPY nginx.conf /etc/nginx/conf.d/default.conf

EXPOSE 80
CMD ["nginx", "-g", "daemon off;"]