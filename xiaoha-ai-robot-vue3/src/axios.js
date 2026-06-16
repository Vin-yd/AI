import axios from "axios";
import { message } from "ant-design-vue";

// 创建 Axios 实例
const instance = axios.create({
    baseURL: "/api",
    timeout: 7000,
})

// 请求拦截器：自动附加 token
instance.interceptors.request.use(
    (config) => {
        const token = localStorage.getItem("token");
        if (token) {
            config.headers.Authorization = `Bearer ${token}`;
        }
        return config;
    },
    (error) => Promise.reject(error)
);

// 响应拦截器：处理 401 未登录
instance.interceptors.response.use(
    (response) => response,
    (error) => {
        if (error.response && error.response.status === 401) {
            localStorage.removeItem("token");
            // 当前不在登录页才跳转
            if (!window.location.hash.includes("/login")) {
                message.warning("登录已过期，请重新登录");
                window.location.hash = "#/login";
            }
        }
        return Promise.reject(error);
    }
);

export default instance;
