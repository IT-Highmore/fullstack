export const permission = {
  mounted(el, binding) {
    const { value } = binding; // 获取指令传入的权限值，例如 'user:add'
    const permissions = JSON.parse(localStorage.getItem("permissions") || "[]");

    if (value && typeof value === "string") {
      const hasPermission = permissions.includes(value);

      // 如果没有对应权限，移除该 DOM 元素
      if (!hasPermission) {
        el.parentNode && el.parentNode.removeChild(el);
      }
    } else {
      throw new Error(`请指定正确格式的权限标识，如 v-permission="'user:add'"`);
    }
  },
};
