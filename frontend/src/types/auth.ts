export interface MenuItem {
  id: number;
  parentId: number | null;
  title: string;
  path: string;
  icon?: string;
  sortOrder?: number | null;
}

export interface UserProfile {
  id: number;
  username: string;
  realName?: string | null;
  employeeNo?: string | null;
  positionName?: string | null;
  departmentId?: number | null;
  phone?: string | null;
  email?: string | null;
  birthday?: string | null;
  hireDate?: string | null;
  status?: number | null;
  roleId?: number | null;
  roleCode?: string | null;
  roleName?: string | null;
  menus?: MenuItem[];
}

export interface LoginPayload {
  token: string;
  user: UserProfile;
}
