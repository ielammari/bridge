import { Link } from 'react-router-dom';
import Wordmark from '../Wordmark/Wordmark.jsx';
import './PublicChrome.css';

/** The foot of the public site: the mark and the year. */
export default function PublicFooter() {
  return (
    <footer className="pubfoot">
      <div className="pubfoot__inner">
        <Link to="/" className="pubfoot__brand">
          <Wordmark className="pubfoot__logo" />
        </Link>

        <p className="pubfoot__note">Bridge <span className="pubfoot__mark">©</span> {new Date().getFullYear()}</p>
      </div>
    </footer>
  );
}
