import { defineStore } from "pinia";
import http from "@/api/http";
import type { MenuItem, UserProfile } from "@/types/auth";

export const useUserStore = defineStore("user", {
  state: () => ({
    token: localStorage.getItem("token") || "",
    profile: null as UserProfile | null,
    menus: [] as MenuItem[],
  }),
  actions: {
    setToken(t: string) {
      this.token = t;
      localStorage.setItem("token", t);
    },
    clear() {
      this.token = "";
      this.profile = null;
      this.menus = [];
      localStorage.removeItem("token");
    },
    async fetchProfile() {
      const data = (await http.get("/auth/me")) as UserProfile;
      this.profile = data;
      this.menus = data.menus || [];
    },
  },
});
