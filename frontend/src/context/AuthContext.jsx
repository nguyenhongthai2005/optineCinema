import React, { createContext, useCallback, useState } from 'react';
import authService from '../services/auth.service';

export const AuthContext = createContext();

export const AuthProvider = ({ children }) => {
  // Lazy initializer: đọc localStorage NGAY khi tạo state (sync, trước first render)
  // → không có race condition với ProtectedRoute
  const [user, setUser] = useState(() => authService.getCurrentUser());

  const login = async (identifier, password) => {
    const data = await authService.login(identifier, password);
    setUser(data);
    return data;
  };

  const logout = () => {
    authService.logout();
    setUser(null);
  };

  const completeGoogleLogin = useCallback(async (token) => {
    const data = await authService.completeGoogleLogin(token);
    setUser(data);
    return data;
  }, []);

  return (
    <AuthContext.Provider value={{ user, login, logout, completeGoogleLogin }}>
      {children}
    </AuthContext.Provider>
  );
};
