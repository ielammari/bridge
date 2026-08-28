import PublicFooter from './PublicFooter.jsx';
import PublicHeader from './PublicHeader.jsx';
import './PublicChrome.css';

/** The frame every public page wears: the header, the page, the foot. */
export default function PublicLayout({ children }) {
  return (
    <div className="public">
      <a className="pubskip" href="#contenu">Aller au contenu</a>
      <PublicHeader />
      <main className="public__main" id="contenu">{children}</main>
      <PublicFooter />
    </div>
  );
}
