import './auth.css';

/**
 * Frame shared by signing in and signing up: the wordmark on the left, the form
 * on the right. `wide` gives the form two columns.
 */
export default function AuthLayout({ title, intro, children, footer, wide = false }) {
  return (
    <div className="auth">
      <aside className="auth__aside">
        <p className="auth__brand">
          <span className="auth__logo">Bridge</span>
          <span className="auth__scope">Recrutement</span>
        </p>
      </aside>

      <main className="auth__panel" id="contenu">
        <div className={`auth__column${wide ? ' auth__column--wide' : ''}`}>
          <header className="auth__header">
            <h1 className="auth__title">{title}</h1>
            {intro && <p className="auth__intro">{intro}</p>}
          </header>

          {children}

          {footer && <footer className="auth__footer">{footer}</footer>}
        </div>
      </main>
    </div>
  );
}
