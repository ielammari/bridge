import './auth.css';

/**
 * Shared frame for signing in and signing up: a single left aligned column on
 * bare paper, with the wordmark carrying the page's identity.
 */
export default function AuthLayout({ title, intro, children, footer }) {
  return (
    <main className="auth">
      <div className="auth__column">
        <div className="auth__brand">
          <span className="auth__logo">Bridge</span>
          <span className="auth__scope">Recrutement</span>
        </div>

        <header className="auth__header">
          <h1 className="auth__title">{title}</h1>
          {intro && <p className="auth__intro">{intro}</p>}
        </header>

        {children}

        {footer && <footer className="auth__footer">{footer}</footer>}
      </div>
    </main>
  );
}
