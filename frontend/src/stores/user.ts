import { defineStore } from "pinia";
import http from "@/api/http";

export interface MenuItem {
  id: number;
  parentId: number;
  title: string;
  path: string;
  icon?: string;
}

export const useUserStore = defineStore("user", {
  state: () => ({
    token: localStorage.getItem("token") || "",
    profile: null as Record<string, unknown> | null,
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
      const data = (await http.get("/auth/me")) as Record<string, unknown>;
      this.profile = data;
      this.menus = (data.menus as MenuItem[]) || [];
    },
  },
});
