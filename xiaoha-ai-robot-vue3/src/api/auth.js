import axios from "@/axios";

// 发送短信验证码
export function sendSmsCode(phone) {
    return axios.post("/auth/sms/send", { phone })
}

// 手机号验证码登录
export function login(phone, code) {
    return axios.post("/auth/login", { phone, code })
}

// 退出登录
export function logout() {
    return axios.post("/auth/logout")
}

// 获取当前用户信息
export function getCurrentUser() {
    return axios.get("/user/me")
}

// 修改用户昵称
export function updateProfile(nickname) {
    return axios.put("/user/profile", { nickname })
}
