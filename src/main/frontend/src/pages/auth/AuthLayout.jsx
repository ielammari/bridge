import './auth.css';

// The hiring stages, in order. Sets expectations before an account is created.
const JOURNEY = ['Candidature', 'Présélection', 'Examen technique', 'Entretien RH', 'Décision'];

/**
 * Frame shared by signing in and signing up: the stages on the left, the form
 * on the right. `wide` gives the form two columns.
 */
export default function AuthLayout({ title, intro, children, footer, wide = false }) {
  return (
    <div className="auth">
      <aside className="auth__aside">
        <div className="auth__brand">
          <span className="auth__logo">Bridge</span>
          <span className="auth__scope">Recrutement</span>
        </div>

        <ol className="auth__journey">
          {JOURNEY.map((label, index) => (
            <li key={label} className="auth__step">
              <span className="auth__step-index mono" aria-hidden="true">
                {String(index + 1).padStart(2, '0')}
              </span>
              <span className="auth__step-label">{label}</span>
            </li>
          ))}
        </ol>
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
