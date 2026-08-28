import { Link } from 'react-router-dom';
import './PublicChrome.css';

/** The foot of the public site: the mark, the same two ways on, and the year. */
export default function PublicFooter() {
  return (
    <footer className="pubfoot">
      <div className="pubfoot__inner">
        <Link to="/" className="pubfoot__brand">Bridge</Link>

        <p className="pubfoot__note">Bridge <span className="pubfoot__mark">©</span> {new Date().getFullYear()}</p>
      </div>
    </footer>
  );
}
