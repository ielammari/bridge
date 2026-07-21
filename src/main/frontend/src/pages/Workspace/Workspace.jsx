import { useAuth } from '../../context/AuthContext.jsx';
import Button from '../../components/Button/Button.jsx';
import './workspace.css';

/**
 * Signed in shell. Confirms the session and the role in force; the per role
 * workspaces replace its body as each is built.
 */
export default function Workspace({ title, children }) {
  const { user, logout } = useAuth();

  return (
    <div className="workspace">
      <header className="workspace__bar">
        <span className="workspace__logo">Bridge</span>
        <div className="workspace__account">
          <span className="workspace__identity">
            {user.firstName} {user.lastName}
            <span className="workspace__role">{user.role}</span>
          </span>
          <Button variant="text" onClick={logout}>
            Se déconnecter
          </Button>
        </div>
      </header>

      <main className="workspace__content">
        <h1>{title}</h1>
        {children}
      </main>
    </div>
  );
}
