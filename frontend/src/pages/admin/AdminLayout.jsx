import React, { useContext } from 'react';
import { AuthContext } from '../../context/AuthContext';

export default function AdminLayout({ children, title }) {
  const { logout } = useContext(AuthContext);

  return (
    <div className="admin-content">
      <header className="admin-header">
        <div>
          <p className="admin-eyebrow">Admin Console</p>
          <h1>{title}</h1>
        </div>
        <button className="admin-icon-button" onClick={logout} title="Logout">
          <span className="material-symbols-outlined">logout</span>
        </button>
      </header>
      {children}
    </div>
  );
}
